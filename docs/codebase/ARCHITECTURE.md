# Architecture

> Generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`).

## 1) Architectural Style

- **Primary style:** single-module, layered MVVM (UI → ViewModel → Repository → DAO), with feature folders inside the UI layer.
- **Why:** each screen has a `*ViewModel : ViewModel` exposing `StateFlow`, constructed through its `Factory` from `App.container` (`di/AppContainer`). All ViewModels go through `PasswordRepository`, which wraps `PasswordDao`. There is no domain/use-case layer and no DI framework.
- **Primary constraints:**
  1. Offline only: all data lives in the app sandbox (Room + SharedPreferences) with no backend.
  2. Security: secrets are encrypted with a non-exportable Android Keystore key, and the UI is protected by `FLAG_SECURE`.
  3. Lightweight: minimal dependencies (no Hilt, Coil, or network stack yet).

## 2) System Flow

```text
User action (Compose) -> ViewModel (viewModelScope) -> PasswordRepository (encrypt/decrypt)
    -> PasswordDao (Room, Flow/suspend) -> SQLite "kupass_database"
    <- Flow<List<PasswordEntity>> -> StateFlow -> collectAsStateWithLifecycle() -> recomposition
```

**Save a password** (create):

1. `PasswordEditorScreen` holds the form state in `remember { mutableStateOf }` and calls `viewModel.savePassword(...)` from the top-bar check button.
2. `PasswordEditorViewModel.savePassword` trims the fields, builds a `PasswordEntity`, and sets `SaveState.Saving`.
3. `PasswordRepository.insertPassword` → `CryptoManager.encrypt` on every text field (on `Dispatchers.Default`) → `PasswordDao.insert` (REPLACE).
4. On success `SaveState.Success` is set, and the screen's `LaunchedEffect(saveState)` pops the back stack.
5. `HomeViewModel.passwords` combines a cached decrypted vault with the query, filters in memory, and re-emits the same flat entity list. `VaultList` derives presentation-only groups without changing IDs or persistence/import identity.

**Grouped Home presentation:** `ui/screens/home/VaultGrouping.kt` gives each group a typed, namespaced identity: normalized HTTP(S) host (ROOT lowercase, `www.` stripped; credentials, port, path, query and fragment ignored), Android facet package (certificate ignored, handled before web parsing), or trimmed ROOT-lowercase site name for missing/invalid URLs. Private hosts, local names and IPs are accepted locally; subdomains remain distinct. Grouping never uses the public-only, domain-guessing `siteDomainOf` icon helper. Groups are alphabetically ordered; lazy keys are `group:<namespace>:<value>` and `account:<id>`.

Every group, including a singleton, starts collapsed. Its header only toggles expansion; child taps forward the original entity's ID through `onItemClick` → `onNavigateToDetail(Long)`. A nonblank query uses the existing name/username/URL/notes filter, forces matching groups open with only matching accounts, and temporarily disables header collapse. Clearing restores the in-memory manual expansion set. Expansion keys are not saved to Android saved state. The single rounded search input clears without losing focus; IME Search or closing an empty input dismisses focus/keyboard. No search popup or placeholder overlay is composed.

**Account gestures:** `RightwardDeleteGesture.kt` observes DOWN unconsumed on Initial, checks movement against touch slop, and claims only predominant physical +X before the pager's Main pass. Leftward/vertical movement stays unconsumed; consumption seen on Initial or on the pre-claim Final pass, lost pointers, and multitouch abandon the gesture until all pointers lift. After claiming, reversals retain ownership. Release beyond 35% of row width (after slop) opens the existing exact-account confirmation once, and every completion/cancel resets the row. The trash background is on the physical left; the account's accessibility delete action opens the same confirmation. No pager rewrite, synthetic drag, or authentication wiring is involved.

**Export:** `DataScreen` → `ExportPasswordDialog` (backup password ≥ 8 chars, confirmed) → `BackupViewModel.prepareExport(CharArray)` → SAF `CreateDocument` → `exportPasswords(uri)` → `repository.getAllPasswords().first()` → `BackupCodec.encode` on `Dispatchers.Default` (PBKDF2 600k ≈ 2 s on an emulator, with `BackupProgressDialog`) → write → password array zeroed.

**Import:** SAF `OpenDocument` → `BackupViewModel.importPasswords` → bounded read (≤ 32 MB) → `BackupCodec.isPasswordProtected` ? `ImportPasswordDialog` (retry on wrong password) : legacy path → `BackupCodec.decode` → `repository.importPasswords` (skip invalid + duplicates, encrypt all, single-transaction `insertAll`) → `VaultEvent.ImportSucceeded(imported, skipped)`.

## 3) Layer/Module Responsibilities

| Layer or module                  | Owns                                                                                                       | Must not own                                       | Evidence                |
| -------------------------------- | ---------------------------------------------------------------------------------------------------------- | -------------------------------------------------- | ----------------------- |
| `App`                            | Process-wide setup: FLAG_SECURE, locale, night mode, dynamic colors                                        | Vault data                                         | `App.kt`                |
| `AppLock` + `MainActivity` | Lock state, biometric/device-credential prompt, auto-lock on background | Vault data | `security/AppLock.kt`, `MainActivity.kt` |
| Compose screens                  | Rendering, ephemeral UI state (dialogs, visibility toggles, form fields), toasts, clipboard                | Persistence, crypto                                | `ui/screens/**`         |
| ViewModels                       | Screen state as `StateFlow`, one-shot results (`SaveState`, `DeleteState`, `VaultEvent` channel), coroutines | Android Views, `Context` beyond `Application`      | `*ViewModel.kt`         |
| `PasswordRepository`             | Vault API for ViewModels, transparent full-field crypto, in-memory sort/search, legacy-format upgrade | UI state                                           | `PasswordRepository.kt` |
| `PasswordDao` / `KupassDatabase` | SQL, reactive queries, singleton DB                                                                        | Crypto                                             | `data/local/db/*`       |
| `BackupCodec`               | Backup file format                                                                                         | File I/O (done in the ViewModel)                   | `BackupCodec.kt`   |
| `CryptoManager`                  | Key creation/lookup, AES-GCM encrypt/decrypt                                                               | Knowledge of entities                              | `CryptoManager.kt`      |
| `PreferenceManager`              | Settings persistence                                                                                       | Applying settings (done in `App`/`SettingsScreen`) | `PreferenceManager.kt`  |

## 4) Reused Patterns

| Pattern                                            | Where found                                              | Why it exists                                           |
| -------------------------------------------------- | -------------------------------------------------------- | ------------------------------------------------------- |
| Double-checked-locking singleton                   | `KupassDatabase.getInstance`                             | One Room instance per process                           |
| Kotlin `object` singleton                          | `CryptoManager`, `BackupCodec`, `AppConfig`         | Stateless utilities                                     |
| Repository (thin, with a crypto decorator)         | `PasswordRepository`                                     | Keep the DAO and UI unaware of encryption               |
| Manual DI container + `viewModelFactory { initializer { } }` | `di/AppContainer.kt`, `*ViewModel.Factory` | Constructor injection without a DI framework (lightweight, testable) |
| Sealed interface operation state                   | `SaveState`, `DeleteState`                               | Drive navigation after async work via `LaunchedEffect`  |
| Sealed one-shot events over a `Channel` | `HomeViewModel.events`, `BackupViewModel.events` (`VaultEvent`) → `MainAppScreen` (`repeatOnLifecycle`) | Toasts after read failures and export/import, delivered exactly once |
| Cached vault + `combine` + `stateIn(WhileSubscribed(5000))` | `HomeViewModel.passwords` | Reactive search without repeated decryption |
| Type-safe navigation with `@Serializable` routes   | `MainAppScreen`                                          | Compile-time route args                                 |
| Stateless screens, state hoisted to the pager | `MainPagerScreen` collects `HomeViewModel` + `BackupViewModel` and passes state/callbacks to `HomeScreen` and `DataScreen` | No ViewModel forwarding between composables (compose-rules) |

## 5) Known Architectural Risks

- ~~Backups bound to one device key~~: fixed by F-2.1 (password-based portable backups). The *database* is still bound to the device Keystore key by design.
- ~~Fail-open crypto~~: fixed by EN-06 (CryptoManager v2 is fail-closed and versioned).
- ~~No DB migration path~~: fixed by EN-03 (exported schemas, `MIGRATIONS` registry, `KupassDatabaseMigrationTest`).
- **Decrypt-on-list:** every database change decrypts every field of every row (on `Dispatchers.Default`), although the list shows no passwords. Search filters that cached list. See CONCERNS §4.
- **Two dynamic-color mechanisms:** `DynamicColors.applyToActivitiesIfAvailable` (View theme) in `App` and the `KupassTheme(dynamicColor)` Compose scheme in `MainActivity`. Only the latter affects Compose UI. [TODO] confirm whether the View-level call is still needed (splash/system dialogs).

## 6) Evidence

- `app/src/main/java/com/nielcode/kupass/App.kt`, `MainActivity.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/MainAppScreen.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/home/HomeViewModel.kt`, `editor/PasswordEditorViewModel.kt`, `detail/PasswordDetailViewModel.kt`
- `app/src/main/java/com/nielcode/kupass/data/repository/PasswordRepository.kt`
- `app/src/main/java/com/nielcode/kupass/data/local/db/KupassDatabase.kt`, `PasswordDao.kt`
- `app/src/main/java/com/nielcode/kupass/security/CryptoManager.kt`, `data/backup/BackupCodec.kt`
- `app/src/main/java/com/nielcode/kupass/ui/theme/Theme.kt`
