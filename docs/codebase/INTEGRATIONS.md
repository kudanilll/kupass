# External Integrations

> Generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`).

## 1) Integration Inventory

| System                                      | Type                                    | Purpose                                                                                  | Auth model                                              | Criticality                                      | Evidence                                                                              |
| ------------------------------------------- | --------------------------------------- | ---------------------------------------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------------------- |
| Android Keystore (`AndroidKeyStore`)        | Platform crypto                         | Holds the non-exportable AES-256 key `kupass_vault_key`                                  | App UID. No user authentication is required for the key | **High**: losing it makes the data undecryptable | `CryptoManager.kt`                                                                    |
| `androidx.biometric` 1.1.0 (`BiometricPrompt`) | Platform auth | UI-level app lock | Biometric or device credential | High | `MainActivity.kt` |
| Storage Access Framework                    | Platform file I/O                       | User-chosen export/import JSON file                                                      | User picks the file                                     | High (backups)                                   | `MainAppScreen.kt`, `HomeViewModel.kt`                                                |
| Clipboard (`ClipboardManager`) | Platform | Copy fields. Password flagged `EXTRA_IS_SENSITIVE` (API 33+) and cleared after 45 s on all APIs | none | Medium (secret exposure) | `security/SecureClipboard.kt` |
| Browser via `ACTION_VIEW`                   | Implicit intent                         | Open developer/GitHub URLs                                                               | none                                                    | Low                                              | `Util.kt`, `SettingsScreen.kt`                                                        |
| `OssLicensesMenuActivity`                   | Play services lib                       | Open-source license screen                                                               | none                                                    | Low                                              | `SettingsScreen.kt`                                                                   |
| Favget API `https://favget.nielcode.web.id` | HTTP API (`GET /v1/icon?domain=` → 302 to Cloudinary) | Always-on site icons in the vault list. Sends only the bare public domain; a single-word site name falls back to `<name>.com` when URL is empty | `Authorization: Bearer FAVGET_API_KEY`, sent only to the Favget host; the redirected image is fetched over HTTPS without it | Low (cosmetic; failures fall back to the initial) | `data/siteicon/FavgetIconSource.kt`, `SiteIconRepository.kt`, `SiteDomain.kt` |

No analytics, crash reporting, or push service is present. The only network use is the Favget lookup above (plain `HttpURLConnection`, no HTTP library). Icons are cached in memory only, because a disk cache would reveal which sites are in the vault.

## 2) Data Stores

| Store                                            | Role                                      | Access layer                           | Key risk                                                                          | Evidence                                 |
| ------------------------------------------------ | ----------------------------------------- | -------------------------------------- | --------------------------------------------------------------------------------- | ---------------------------------------- |
| Room/SQLite `kupass_database`, table `passwords` | Vault                                     | `PasswordDao` via `PasswordRepository` | Only `password` is encrypted. No migrations (`version 1`, `exportSchema = false`) | `KupassDatabase.kt`, `PasswordEntity.kt` |
| SharedPreferences `kupass_preferences`           | `language`, `theme`, `dynamic_color` ints | `PreferenceManager`                    | Low (no secrets)                                                                  | `PreferenceManager.kt`                   |
| Backup file (user storage, `kupass-backup.json`) | Portable backup v2 | `data/backup/BackupCodec` | Strength depends on the user's backup password (min 8 chars, PBKDF2 600k) | `BackupCodec.kt` |

## 3) Secrets and Credentials Handling

- Signing secrets come from gitignored `secrets.properties` + `release-key.jks`. If the keystore is missing, release falls back to debug signing (`app/build.gradle.kts`).
- `BuildConfig.FAVGET_API_KEY` is read from `secrets.properties` (`FAVGET_API_KEY`, surrounding quotes tolerated) or, in the release workflow, from the optional `FAVGET_API_KEY` repository secret. It is extractable from the APK by design (accepted 2026-09-19); Favget's server-side rate limiting is the control. Without a key, Settings shows the feature as not available.
- Vault key lifecycle: created lazily on first `encrypt`, never rotated. It's deleted by the OS on uninstall/clear data. [TODO] no rotation or re-key procedure exists.

## 4) Reliability and Failure Behavior

- Retry/timeout: Favget requests time out after 8 s, at most 4 run at once, and concurrent requests for one domain share a fetch. Misses are remembered for 6 h and failures (network, 401/403/429, 5xx) for 5 min, in memory.
- Crypto failure policy (EN-06): fail-closed. `CryptoManager.encrypt`/`decrypt` throw `CryptoException`. ViewModels catch it: the list shows the `toast_vault_read_failed` toast and never ciphertext.
- SAF failures are caught generically and surfaced as the `export_failed`/`import_failed` toasts.
- Import cap: rejects more than 2000 entries, but only after the full file is read and parsed.

## 5) Observability

- Logging: none (`printStackTrace()` once in `CryptoManager`).
- Metrics/crash reporting: none.
- Gap: crypto fallbacks happen silently, so corruption can't be detected in the field.
- Decision (2026-09-18): **the app stays 100% telemetry-free.** Field issues come from a user-initiated feedback/bug-report feature (PRD FEAT-5), so errors must be shown to the user in-app instead of being swallowed.

## 6) Evidence

- `app/src/main/java/com/nielcode/kupass/security/CryptoManager.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/home/HomeViewModel.kt`, `ui/screens/MainAppScreen.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/detail/PasswordDetailScreen.kt`
- `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`
- `app/src/main/java/com/nielcode/kupass/data/siteicon/` (site icons)
