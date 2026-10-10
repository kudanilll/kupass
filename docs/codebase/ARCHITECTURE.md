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
5. `HomeViewModel.passwords` (`flatMapLatest` over the search query → `getAll` Flow → decrypt every field → sort/filter in memory) re-emits, and `VaultList` recomposes.

**Export:** `DataScreen` → fresh native authentication → request-bound consent → `ExportPasswordDialog` (backup password ≥ 8 chars, confirmed) → consent transitions to picker → `BackupViewModel.prepareExport(CharArray)` (binds password to request token) → SAF `CreateDocument` → validate/consume outstanding picker request → `exportPasswords(uri)` consumes the matching single-use write permission before `repository.getAllPasswords().first()` or any stream → `BackupCodec.encode` → encrypted write → password array zeroed. Consent expires after five minutes. Cancellation, navigation/tab departure, credential removal, timeout, or a stale result clears pending export through `cancelExport`. Import remains public. No CSV export.

**Account access:** public Home keeps `onNavigateToDetail(Long)`. It requests authentication on the originating navigation entry; only a current successful request navigates and grants the newly created detail entry. Actual detail and positive-ID editor destination content checks the activity-retained `EntryAuthenticationViewModel` grant before invoking the screen (including default ViewModel creation). A restored route has no grant. Authorized detail-to-editor navigation copies permission to that editor entry and retains its parent detail; Home ends the chain. Rotation reattaches native callbacks using the retained request token without re-authenticating. Native requests are serialized until terminal callbacks drain; no closures are retained in grants or singletons. Each controller constructs and privately owns its `AppLock` absence timer from the timeout preference; `AppContainer` stores no timer. Prompts in a second activity cannot reset the first activity's absence or revive its grants. Picker/security-settings/browser/license allowances go through the launching activity's controller, retaining the existing bounded grace. Disposed sensitive screens cancel their loading jobs and drop decrypted ViewModel fields. Positive-ID editors refuse save when their original entry cannot be loaded.

**Import:** SAF `OpenDocument` → `BackupViewModel.importPasswords` → bounded read (≤ 32 MB) → `BackupCodec.isPasswordProtected` ? `ImportPasswordDialog` (retry on wrong password) : legacy path → `BackupCodec.decode` → `repository.importPasswords` (skip invalid + duplicates, encrypt all, single-transaction `insertAll`) → `VaultEvent.ImportSucceeded(imported, skipped)`.

## 3) Layer/Module Responsibilities

| Layer or module                  | Owns                                                                                                       | Must not own                                       | Evidence                |
| -------------------------------- | ---------------------------------------------------------------------------------------------------------- | -------------------------------------------------- | ----------------------- |
| `App`                            | Process-wide setup: FLAG_SECURE, locale, night mode, dynamic colors                                        | Vault data                                         | `App.kt`                |
| `AppLock` + `EntryAuthenticationViewModel` + `MainActivity` | Activity-local absence timer and bounded exemptions, memory-only request/entry grants, native prompt session and rotation reattachment | Vault data, process-wide timers, persisted grants, composition closures | `security/AppLock.kt`, `security/EntryAuthenticationViewModel.kt`, `MainActivity.kt` |
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
| `flatMapLatest` + `stateIn(WhileSubscribed(5000))` | `HomeViewModel.passwords`                                | Reactive search                                         |
| Type-safe navigation with `@Serializable` routes   | `MainAppScreen`                                          | Compile-time route args                                 |
| Stateless screens, state hoisted to the pager | `MainPagerScreen` collects `HomeViewModel` + `BackupViewModel` and passes state/callbacks to `HomeScreen` and `DataScreen` | No ViewModel forwarding between composables (compose-rules) |

## 5) Known Architectural Risks

- ~~Backups bound to one device key~~: fixed by F-2.1 (password-based portable backups). The *database* is still bound to the device Keystore key by design.
- ~~Fail-open crypto~~: fixed by EN-06 (CryptoManager v2 is fail-closed and versioned).
- ~~No DB migration path~~: fixed by EN-03 (exported schemas, `MIGRATIONS` registry, `KupassDatabaseMigrationTest`).
- **Decrypt-on-list:** every list/search emission decrypts every field of every row (on `Dispatchers.Default`), although the list shows no passwords. Fine for small vaults. See CONCERNS §4.
- **Two dynamic-color mechanisms:** `DynamicColors.applyToActivitiesIfAvailable` (View theme) in `App` and the `KupassTheme(dynamicColor)` Compose scheme in `MainActivity`. Only the latter affects Compose UI. [TODO] confirm whether the View-level call is still needed (splash/system dialogs).

## 6) Evidence

- `app/src/main/java/com/nielcode/kupass/App.kt`, `MainActivity.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/MainAppScreen.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/home/HomeViewModel.kt`, `editor/PasswordEditorViewModel.kt`, `detail/PasswordDetailViewModel.kt`
- `app/src/main/java/com/nielcode/kupass/data/repository/PasswordRepository.kt`
- `app/src/main/java/com/nielcode/kupass/data/local/db/KupassDatabase.kt`, `PasswordDao.kt`
- `app/src/main/java/com/nielcode/kupass/security/CryptoManager.kt`, `data/backup/BackupCodec.kt`
- `app/src/main/java/com/nielcode/kupass/ui/theme/Theme.kt`
