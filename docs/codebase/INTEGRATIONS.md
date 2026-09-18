# External Integrations

> Generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`).

## 1) Integration Inventory

| System                                      | Type                                    | Purpose                                                                                  | Auth model                                              | Criticality                                      | Evidence                                                                              |
| ------------------------------------------- | --------------------------------------- | ---------------------------------------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------------------- |
| Android Keystore (`AndroidKeyStore`)        | Platform crypto                         | Holds the non-exportable AES-256 key `kupass_vault_key`                                  | App UID. No user authentication is required for the key | **High**: losing it makes the data undecryptable | `CryptoManager.kt`                                                                    |
| Storage Access Framework                    | Platform file I/O                       | User-chosen export/import JSON file                                                      | User picks the file                                     | High (backups)                                   | `MainAppScreen.kt`, `HomeViewModel.kt`                                                |
| Clipboard (`ClipboardManager`)              | Platform                                | Copy fields. Password flagged `IS_SENSITIVE` (API 33+) or auto-cleared after 45 s (< 33) | none                                                    | Medium (secret exposure)                         | `PasswordDetailScreen.kt`                                                             |
| Browser via `ACTION_VIEW`                   | Implicit intent                         | Open developer/GitHub URLs                                                               | none                                                    | Low                                              | `Util.kt`, `SettingsScreen.kt`                                                        |
| `OssLicensesMenuActivity`                   | Play services lib                       | Open-source license screen                                                               | none                                                    | Low                                              | `SettingsScreen.kt`                                                                   |
| Favget API `https://favget.nielcode.web.id` | HTTP API (**planned, not wired; keep**) | Site favicons, opt-in (PRD FEAT-2)                                                       | [TODO]                                                  | n/a                                              | `BuildConfig.FAVGET_API_URL` in `app/build.gradle.kts`, TODO in `PasswordListItem.kt` |

No analytics, crash reporting, network client, or push service is present. The `INTERNET` permission is declared in `AndroidManifest.xml` but currently unused by app code.

## 2) Data Stores

| Store                                            | Role                                      | Access layer                           | Key risk                                                                          | Evidence                                 |
| ------------------------------------------------ | ----------------------------------------- | -------------------------------------- | --------------------------------------------------------------------------------- | ---------------------------------------- |
| Room/SQLite `kupass_database`, table `passwords` | Vault                                     | `PasswordDao` via `PasswordRepository` | Only `password` is encrypted. No migrations (`version 1`, `exportSchema = false`) | `KupassDatabase.kt`, `PasswordEntity.kt` |
| SharedPreferences `kupass_preferences`           | `language`, `theme`, `dynamic_color` ints | `PreferenceManager`                    | Low (no secrets)                                                                  | `PreferenceManager.kt`                   |
| Exported JSON file (user storage)                | Backup                                    | `JsonExportImport`                     | Metadata in plaintext. Password bound to the device key                           | `JsonExportImport.kt`                    |

## 3) Secrets and Credentials Handling

- Signing secrets come from gitignored `secrets.properties` + `release-key.jks`. If the keystore is missing, release falls back to debug signing (`app/build.gradle.kts`).
- Hardcoding check: `BuildConfig` has only public URLs and a name. No API keys (the earlier `FAVGET_API_KEY` is gone).
- Vault key lifecycle: created lazily on first `encrypt`, never rotated. It's deleted by the OS on uninstall/clear data. [TODO] no rotation or re-key procedure exists.

## 4) Reliability and Failure Behavior

- Retry/timeout: n/a (no network).
- Fallbacks: `CryptoManager.encrypt` returns plaintext on failure, and `decrypt` returns its input on failure (**fail-open**).
- SAF failures are caught generically and surfaced as the `export_failed`/`import_failed` toasts.
- Import cap: rejects more than 2000 entries, but only after the full file is read and parsed.

## 5) Observability

- Logging: none (`printStackTrace()` once in `CryptoManager`).
- Metrics/crash reporting: none.
- Gap: crypto fallbacks happen silently, so corruption can't be detected in the field.
- Decision (2026-09-18): **the app stays 100% telemetry-free.** Field issues come from a user-initiated feedback/bug-report feature (PRD FEAT-5), so errors must be shown to the user in-app instead of being swallowed.

## 6) Evidence

- `app/src/main/java/com/nielcode/kupass/utils/CryptoManager.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/home/HomeViewModel.kt`, `ui/screens/MainAppScreen.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/detail/PasswordDetailScreen.kt`
- `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`
- `app/src/main/java/com/nielcode/kupass/ui/screens/home/components/PasswordListItem.kt` (favicon TODO)
