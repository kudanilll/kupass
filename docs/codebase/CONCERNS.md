# Codebase Concerns

> Generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`). This is the single source of truth for known issues. Update it whenever one is fixed or found.

## 1) Top Risks (Prioritized)

| ID   | Severity                     | Concern                                                                                                                                                                                                                                                                             | Evidence                                                                                            | Impact                                                                                                | Suggested action                                                                                                            |
| ---- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| C-1 | ~~Critical~~ **Fixed 2026-09-18 (F-2.1)** | Backups were bound to the device key and restored ciphertext as passwords | `data/backup/BackupCodec.kt` | none | Backup v2: whole vault AES-256-GCM, key = PBKDF2-HMAC-SHA256(backup password, 600k iterations, random salt), header as AAD. Legacy v1 files import only if this device can decrypt them, otherwise `BackupException.ForeignDevice`. Regression test in `BackupCodecTest` |
| C-2 | ~~High~~ **Fixed 2026-09-18 (EN-06)** | Crypto failed open | `CryptoManager.kt` | none | v2 format `kp2:` + Base64(IV‖ct‖tag). `encrypt` throws `CryptoException` (never plaintext). `decrypt` throws on v2 failure or v1-looking ciphertext from another key. Legacy plaintext is still readable. The in-memory "test key" fallback was removed from production (tests inject a key via `setKeyProviderForTesting`). Crypto runs on `Dispatchers.Default` |
| C-3 | ~~High~~ **Fixed 2026-09-18 (EN-07)** | Metadata was stored in plaintext | `PasswordRepository.kt` | none | All text fields are v2-encrypted at rest. Sorting and search happen in memory after decryption (decision Q1). `upgradeStoredFormat()` re-encrypts legacy rows at startup (idempotent, skips undecryptable rows). Verified on device by `PasswordRepositoryUpgradeTest` |
| C-4 | ~~High~~ **Fixed 2026-09-18 (S-3.1, S-3.2)** | No app lock | `security/AppLock.kt`, `MainActivity.kt`, `ui/screens/lock/LockScreen.kt` | none | UI-level lock (decision Q3): `BiometricPrompt` with `BIOMETRIC_STRONG\|DEVICE_CREDENTIAL` (API 30+) or `BIOMETRIC_WEAK\|DEVICE_CREDENTIAL` (API 27–29). The vault isn't composed while locked. Auto-lock after 0/30/60/300 s in background (default 30 s). Devices without a screen lock get guidance instead of the vault. Trips to our own screens (file picker, browser, licenses) get a 5-minute grace period instead of skipping the timer (security-review fix). Verified on an Android 17 emulator |
| C-5 | ~~Medium~~ **Fixed 2026-09-18 (EN-03)** | Room had no migration strategy | `KupassDatabase.kt` | none | `exportSchema = true` + `app/schemas/` committed, `KupassDatabase.MIGRATIONS` registry, `KupassDatabaseMigrationTest` (instrumented). Any schema bump must add a migration and a test |
| C-6 | ~~Medium~~ **Fixed 2026-09-18 (S-2.5)** | OS backup/D2D could copy the vault DB | `res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml` | none | Every domain excluded from `cloud-backup` and `device-transfer` (API 31+) and from full backup (API ≤ 30). iOS cross-platform transfer is opt-in and not configured. The vault moves only via the password-protected export |
| C-7 | ~~Medium~~ **Fixed 2026-09-18 (S-2.4)** | Import wasn't atomic and had no dedupe | `PasswordRepository.importPasswords`, `PasswordDao.insertAll` | none | Encrypt all first, then one `@Insert` list transaction. Duplicates (site ignoring case/spaces + username + URL + password) of the vault or of the same file are skipped. The toast reports imported/skipped counts |
| C-8 | ~~Low~~ **Fixed 2026-09-18 (F-2.1, EN-05)** | Unmapped string keys and unbounded import read | `HomeViewModel.kt`, `VaultEvent.kt` | none | Typed `VaultEvent` via `Channel`. Every event has a message. The file read is capped at 32 MB before parsing |
| C-9 | ~~Low~~ **Fixed 2026-09-18 (EN-05)** | Save/delete errors were silent | `PasswordEditorScreen.kt`, `PasswordDetailScreen.kt` | none | Errors show a toast and reset state. The editor keeps the form so nothing typed is lost |
| C-10 | ~~Low~~ **Fixed 2026-09-18** | `@RequiresApi(P)` on `PasswordDetailScreen`/`copyToClipboard` (lint `NewApi` error). The real cause was `clearPrimaryClip()` (API 28) on minSdk 27                                                                                                                                  | `PasswordDetailScreen.kt`                                                                           | none                                                                                                  | Annotation removed. API 27 now overwrites the clip with an empty one                                                        |

## 2) Technical Debt

| Debt item                                                     | Why it exists                                                | Where                                                                                                                                                                             | Risk if ignored                      | Suggested fix                                                                                                  |
| ------------------------------------------------------------- | ------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ | -------------------------------------------------------------------------------------------------------------- |
| ~~String-keyed one-shot events~~ | **Fixed (EN-05)**: `VaultEvent` sealed interface over a `Channel` | `ui/screens/home/VaultEvent.kt` | none | none |
| ~~No DI~~ | **Fixed 2026-09-18 (EN-04)**: `di/AppContainer` + `ViewModelProvider.Factory` per ViewModel | `di/AppContainer.kt` | none | Remaining: `SettingsScreen`/`MainActivity` still create `PreferenceManager` directly (EN-4.3) |
| Settings logic in the composable                              | No ViewModel                                                 | `SettingsScreen.kt`, `MainActivity.kt`                                                                                                                                            | Prefs I/O in composition. Untestable | `SettingsViewModel`                                                                                            |
| ~~`collectAsState()`~~ | **Fixed (EN-05)**: `collectAsStateWithLifecycle()` everywhere, events collected with `repeatOnLifecycle(STARTED)` | all screens | none | none |
| ~~AppCompat and Material Components used but not declared~~   | **Fixed 2026-09-18**: declared in the catalog                | `gradle/libs.versions.toml`                                                                                                                                                       | none                                 | none                                                                                                           |
| ~~Robolectric 4.11.1 hardcoded outside the catalog~~          | **Fixed 2026-09-18**: 4.17 via the catalog (supports SDK 37) | `gradle/libs.versions.toml`                                                                                                                                                       | none                                 | none                                                                                                           |
| Unused code                                                   | Leftovers                                                    | `AppConfig.FILES_PREFIX`, `EMPTY_STRING`, `Language.ENGLISH_TAG/INDONESIA_TAG`, `PasswordListItem.itemCount`, `PasswordDao.deleteById` → `repository.deletePasswordById` (unused) | Noise                                | Remove or use. **Keep** `BuildConfig.FAVGET_API_URL` and `PasswordListItem.imageUrl` (planned favicon feature) |
| ~~Package mismatch~~ | **Fixed (EN-02)**: `TextField.kt` became `ui/components/VaultTextField.kt` | none | none | none |
| ~~Placeholder tests~~ | **Fixed (EN-02)**: `ExampleUnitTest`/`ExampleInstrumentedTest` removed | none | none | none |
| ~~Duplicate route-transition code~~ | **Fixed (EN-02)**: `slideUpEnter()` / `slideDownExit()` helpers | none | none | none |

| Navigation 2.x is in maintenance mode | Upstream decision (Navigation 2.10 release notes) | `MainAppScreen.kt` | No new features, only critical fixes | Plan a migration to Navigation 3 (`navigation-3` skill) when the UI is next reworked |
| Room 2.x while Room 3 (`androidx.room3`, 3.0.x) exists | Room 3 is a new artifact group (driver-based, KMP) | `data/local/db/*` | Room 2.8 still maintained. Migration effort grows over time | Evaluate Room 3 migration separately. Not on the critical path |
| Remaining lint warnings: 32 `UnusedResources`, 10 `IconDuplicates`, 2 `IconLocation`, 1 `ObsoleteSdkInt`, 1 `ButtonCase`, 1 `AndroidGradlePluginVersion` (AGP 9.4.1, Dependabot will propose it) | Leftovers | `./gradlew :app:lintDebug` | APK bloat, noise | Clean up in EN-4.4 / EN-5.3 |

## 3) Security Concerns

| Risk                                    | OWASP (MASVS)               | Evidence                                         | Current mitigation               | Gap                                               |
| --------------------------------------- | --------------------------- | ------------------------------------------------ | -------------------------------- | ------------------------------------------------- |
| Non-portable, fail-open backup crypto   | MASVS-CRYPTO, MASVS-STORAGE | C-1, C-2                                         | Password field encrypted         | Portability, fail-closed behavior, format version |
| Plaintext metadata at rest              | MASVS-STORAGE               | C-3                                              | App sandbox, `allowBackup=false` | Field or DB encryption                            |
| No local authentication                 | MASVS-AUTH                  | C-4                                              | Device lock screen only          | App lock                                          |
| Clipboard exposure | MASVS-PLATFORM | `security/SecureClipboard.kt` | **Fixed 2026-09-18 (S-3.3)**: sensitive flag (API 33+) + clear after 45 s on every API. Cleared even when unreadable in the background (Android 10+) | Timeout isn't user-configurable yet (PRD SEC-6). The Handler timer doesn't survive process death |
| D2D transfer of the DB | MASVS-STORAGE | C-6 | `allowBackup=false` + explicit exclusions for every domain | none |
| Screenshots/recents                     | MASVS-PLATFORM              | `App.kt`                                         | `FLAG_SECURE` on all Activities  | none                                              |
| Exported components                     | MASVS-PLATFORM              | Manifest: only the launcher Activity is exported | OK                               | none                                              |
| Future favicon fetch leaks site domains | MASVS-PRIVACY               | `FAVGET_API_URL`, `INTERNET` permission          | Not implemented yet              | Must be opt-in, domain only (PRD FEAT-2)          |

## 4) Performance and Size Concerns

| Concern                                                                                                                                  | Evidence                                                              | Current symptom           | Scaling risk                                                         | Suggested improvement                                                                                   |
| ---------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- | ------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `googlesansflex.ttf` is ~3.9 MB, while the whole release APK in `app/release/` is ~3.9 MB. The font is likely the biggest single payload | scan CODE METRICS, `res/font/`, `Type.kt`                             | Larger download/install   | Grows with any extra weights                                         | Subset the font and keep Google Sans Flex, per decision (PRD QA-3)                                      |
| Decrypt every field of every row on each list/search emission (5 AES-GCM ops per row) | `PasswordRepository.getAllPasswords/searchPasswords` | Negligible for small vaults. Runs on `Dispatchers.Default` | Linear per keystroke on large vaults | Cache the decrypted list in memory while unlocked and filter it. Debounce search (EN-5.2) |
| ~~SQL `LIKE` search~~ | Removed (EN-07): search is in memory over decrypted rows | none | none | none |
| `material-icons-extended`                                                                                                                | `app/build.gradle.kts`                                                | Relies on R8 to strip     | Slow debug builds                                                    | Copy the needed icons as vectors                                                                        |
| No Baseline Profile (but `app/release/baselineProfiles/` exists as build output)                                                         | `app/release/`                                                        | [TODO] measure cold start | n/a                                                                  | Add a baseline profile module if startup is slow                                                        |

## 5) Fragile/High-Churn Areas (last 90 days)

| Area                                        | Why fragile                                            | Churn signal                     | Safe change strategy                                                                |
| ------------------------------------------- | ------------------------------------------------------ | -------------------------------- | ----------------------------------------------------------------------------------- |
| `security/CryptoManager.kt`                    | Security-critical, fail-open, has a test-only key path | 2 commits (`26a489b`, `8de433b`) | Change only with tests. Never break decryption of existing data. Version the format |
| `ui/screens/detail/PasswordDetailScreen.kt` | Clipboard + API-level branches + delete flow           | 2 commits                        | Test on API 27, 32, and 33+                                                         |
| `app/build.gradle.kts`                      | Signing fallback, BuildConfig                          | 2 commits                        | Never commit secrets. Verify the release build                                      |

## 6) Resolved `[ASK USER]` Questions (answered by Danil, 2026-09-18)

1. Portable backups? **Yes.** Fix C-1 with password-based backup encryption (PRD SEC-1).
2. App lock? **Biometric with device PIN/pattern fallback** (PRD SEC-2).
3. Favget favicons? **Keep the config and plan.** Not used yet. `FAVGET_API_URL` is _not_ dead code (PRD FEAT-2).
4. Font? **Keep Google Sans Flex, but optimize/compress it** (PRD QA-3).
5. Formatter/lint + CI? **Yes** (PRD QA-1, QA-2).
6. Telemetry? **None, ever.** Add a user-initiated feedback/bug-report feature instead (PRD FEAT-5).
7. License? **GPL-3.0.** Commit `d1f7eff` ("AGPLv3") has a wrong commit message. `LICENSE`/README are correct.

## 7) Evidence

- `docs/codebase/.codebase-scan.txt` (TODO, HIGH-CHURN, CODE METRICS sections)
- `app/src/main/java/com/nielcode/kupass/security/CryptoManager.kt`
- `app/src/main/java/com/nielcode/kupass/data/backup/BackupCodec.kt`, `data/repository/PasswordRepository.kt`, `data/local/db/KupassDatabase.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/home/HomeViewModel.kt`, `ui/screens/MainAppScreen.kt`, `ui/screens/detail/PasswordDetailScreen.kt`
- `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/res/font/`
- `app/src/test/java/com/nielcode/kupass/data/backup/BackupCodecTest.kt`
- Android docs (context7 `/websites/developer_android_guide`): `allowBackup=false` does not disable D2D migration on some manufacturers
