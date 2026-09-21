# Codebase Concerns

> Single source of truth for **open** issues. First generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`); last updated 2026-09-21.
>
> **Fixed items are deleted, not struck through.** The history of what was fixed lives in `CHANGELOG.md` and in git. Add an item when you find one, delete it when it is fixed.

## 1) Open Risks (Prioritized)

No open critical or high risks.

The ten risks from the 2026-09-18 audit (`C-1`…`C-10`: device-bound fail-open backups, fail-open crypto, plaintext metadata at rest, no app lock, no Room migration strategy, OS backup/D2D copying the vault, non-atomic import without dedupe, untyped one-shot events, silent save/delete errors, and the `@RequiresApi(P)` clipboard workaround) are all fixed. See `CHANGELOG.md` and the roadmap milestones for what shipped.

## 2) Technical Debt

| Debt item                                              | Why it exists                                      | Where                                                                                                                                                                            | Risk if ignored                                | Suggested fix                                                                        |
| ------------------------------------------------------ | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------- | ------------------------------------------------------------------------------------ |
| Settings logic in the composable                       | No ViewModel                                       | `SettingsScreen.kt`, `MainActivity.kt` (both also build `PreferenceManager` directly instead of taking it from `AppContainer`)                                                    | Prefs I/O in composition. Untestable           | `SettingsViewModel` (EN-4.3)                                                         |
| Unused code                                            | Leftovers                                          | `AppConfig.FILES_PREFIX`, `EMPTY_STRING`, `Language.ENGLISH_TAG/INDONESIA_TAG`, `PasswordListItem.itemCount`, `PasswordDao.deleteById` → `repository.deletePasswordById` (unused) | Noise                                          | Remove or use (EN-4.4)                                                               |
| Navigation 2.x is in maintenance mode                  | Upstream decision (Navigation 2.10 release notes)  | `MainAppScreen.kt`                                                                                                                                                                | No new features, only critical fixes           | Migrate to Navigation 3 together with the adaptive layout: issue #6 (EN-7.6 + S-7.5) |
| Room 2.x while Room 3 (`androidx.room3`, 3.0.x) exists | Room 3 is a new artifact group (driver-based, KMP)  | `data/local/db/*`                                                                                                                                                                 | Room 2.8 is still maintained; effort grows      | Evaluate separately. Not on the critical path                                        |
| Remaining lint warnings                                | Leftovers: `UnusedResources` (33), `IconDuplicates` (10), `IconLocation` (2), `ObsoleteSdkInt` (`mipmap-anydpi-v26`), `ButtonCase` (`button_cancel`) | `./gradlew :app:lintDebug` (counted 2026-09-21)                                                                     | APK bloat, noise that hides real warnings       | Clean up in EN-4.4 / EN-5.3                                                          |

## 3) Security Concerns

Controls in place (no gap): fail-closed AES-GCM (`kp2:`) on every field, portable password-based backups, UI-level app lock, Room migrations, OS backup/D2D exclusions, `FLAG_SECURE` on all Activities, only the launcher Activity exported.

| Risk                              | OWASP (MASVS)  | Evidence                                | Current mitigation                                                                                                                | Gap                                                                                                     |
| --------------------------------- | -------------- | --------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| Clipboard exposure                | MASVS-PLATFORM | `security/SecureClipboard.kt`           | Sensitive flag (API 33+) and clear after 45 s on every API, even when unreadable in the background (Android 10+)                   | The timeout isn't user-configurable yet (PRD SEC-6), and the `Handler` timer doesn't survive process death |
| Site icons send domains to Favget | MASVS-PRIVACY  | `data/siteicon/`, `INTERNET` permission | Opt-in, off by default, privacy note; bare public domain only (no IPs or private names); memory-only cache; key never sent to the CDN | Favget and Cloudinary can see which domains a user looks up (accepted 2026-09-19)                        |
| Vault key not bound to user auth  | MASVS-AUTH     | `security/CryptoManager.kt`             | App lock gates the UI; the Keystore key is not `setUserAuthenticationRequired`                                                    | A compromised unlocked device can use the key. Accepted (decision 2026-09-18: lockout risk)              |
| Vault key is never rotated        | MASVS-CRYPTO   | `security/CryptoManager.kt`             | Created lazily on first encrypt, deleted by the OS on uninstall/clear data                                                        | No rotation or re-key procedure exists                                                                   |

## 4) Performance and Size Concerns

| Concern                        | Evidence                                   | Current symptom                     | Scaling risk                                  | Suggested improvement                              |
| ------------------------------ | ------------------------------------------ | ----------------------------------- | --------------------------------------------- | -------------------------------------------------- |
| Decryption per vault change    | `HomeViewModel`, `PasswordRepository`      | Fine for normal vaults              | Linear in vault size on every vault change    | Debounce search if large vaults need it (EN-5.2)   |
| `material-icons-extended`      | `app/build.gradle.kts`                     | Relies on R8 to strip               | Slow debug builds                             | Copy the needed icons as vectors                   |
| No Baseline Profile            | `app/release/baselineProfiles/` is output only | [TODO] measure cold start        | n/a                                           | Add a baseline profile module if startup is slow   |

## 5) Fragile/High-Churn Areas

| Area                                        | Why fragile                                            | Safe change strategy                                                                |
| ------------------------------------------- | ------------------------------------------------------ | ----------------------------------------------------------------------------------- |
| `security/CryptoManager.kt`                 | Security-critical, has a test-only key path            | Change only with tests. Never break decryption of existing data. Version the format |
| `security/AppLock.kt`, `MainActivity.kt`    | Lock gate plus background-exemption timing             | Verify on a device: leaving for a picker or browser must not lock, long absences must |
| `data/siteicon/`                            | The only network code, and it handles the API key      | Keep the key off every host but Favget. Never add a disk cache or logging            |
| `ui/screens/detail/PasswordDetailScreen.kt` | Clipboard plus API-level branches plus the delete flow | Test on API 27, 32, and 33+                                                         |
| `app/build.gradle.kts`                      | Signing fallback, BuildConfig secrets                  | Never commit secrets. Verify the release build                                       |

## 6) Evidence

- `docs/codebase/.codebase-scan.txt` (TODO, HIGH-CHURN, CODE METRICS sections)
- `app/src/main/java/com/nielcode/kupass/security/` (`CryptoManager.kt`, `AppLock.kt`, `SecureClipboard.kt`)
- `app/src/main/java/com/nielcode/kupass/data/` (`backup/BackupCodec.kt`, `repository/PasswordRepository.kt`, `local/db/KupassDatabase.kt`, `siteicon/`)
- `app/src/main/java/com/nielcode/kupass/ui/screens/` (`MainAppScreen.kt`, `home/HomeViewModel.kt`, `detail/PasswordDetailScreen.kt`)
- `app/src/main/res/xml/data_extraction_rules.xml`, `app/build.gradle.kts`
- Product decisions behind the accepted risks above: `docs/ai/prd.md` §8 Decision Log
