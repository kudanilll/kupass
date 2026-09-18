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
    <- Flow<List<PasswordEntity>> -> StateFlow -> collectAsState() -> recomposition
```

**Save a password** (create):

1. `PasswordEditorScreen` holds the form state in `remember { mutableStateOf }` and calls `viewModel.savePassword(...)` from the top-bar check button.
2. `PasswordEditorViewModel.savePassword` trims the fields, builds a `PasswordEntity`, and sets `SaveState.Saving`.
3. `PasswordRepository.insertPassword` → `CryptoManager.encrypt(password)` → `PasswordDao.insert` (REPLACE).
4. On success `SaveState.Success` is set, and the screen's `LaunchedEffect(saveState)` pops the back stack.
5. `HomeViewModel.passwords` (`flatMapLatest` over the search query → `getAll`/`search` Flow → decrypt each row) re-emits, and `VaultList` recomposes.

**Export:** `DataScreen` → `ExportPasswordDialog` (backup password ≥ 8 chars, confirmed) → `HomeViewModel.prepareExport(CharArray)` → SAF `CreateDocument` → `exportPasswords(uri)` → `repository.getAllPasswords().first()` → `BackupCodec.encode` on `Dispatchers.Default` (PBKDF2 600k ≈ 2 s on an emulator, with `BackupProgressDialog`) → write → password array zeroed.

**Import:** SAF `OpenDocument` → `HomeViewModel.importPasswords` → bounded read (≤ 32 MB) → `BackupCodec.isPasswordProtected` ? `ImportPasswordDialog` (retry on wrong password) : legacy path → `BackupCodec.decode` → filter blanks → `insertPassword` one by one (atomic import is S-2.4).

## 3) Layer/Module Responsibilities

| Layer or module                  | Owns                                                                                                       | Must not own                                       | Evidence                |
| -------------------------------- | ---------------------------------------------------------------------------------------------------------- | -------------------------------------------------- | ----------------------- |
| `App`                            | Process-wide setup: FLAG_SECURE, locale, night mode, dynamic colors                                        | Vault data                                         | `App.kt`                |
| `AppLock` + `MainActivity` | Lock state, biometric/device-credential prompt, auto-lock on background | Vault data | `security/AppLock.kt`, `MainActivity.kt` |
| Compose screens                  | Rendering, ephemeral UI state (dialogs, visibility toggles, form fields), toasts, clipboard                | Persistence, crypto                                | `ui/screens/**`         |
| ViewModels                       | Screen state as `StateFlow`, one-shot results (`SaveState`, `DeleteState`, `operationMessage`), coroutines | Android Views, `Context` beyond `Application`      | `*ViewModel.kt`         |
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
| String-keyed event in `StateFlow<String?>`         | `HomeViewModel.operationMessage` → `MainAppScreen`       | Toast after export/import (weaker variant of the above) |
| `flatMapLatest` + `stateIn(WhileSubscribed(5000))` | `HomeViewModel.passwords`                                | Reactive search                                         |
| Type-safe navigation with `@Serializable` routes   | `MainAppScreen`                                          | Compile-time route args                                 |
| Shared ViewModel across pager pages                | `MainPagerScreen` passes `homeViewModel` to `HomeScreen` | Export/import launchers live at pager level             |

## 5) Known Architectural Risks

- ~~Backups bound to one device key~~: fixed by F-2.1 (password-based portable backups). The *database* is still bound to the device Keystore key by design.
- ~~Fail-open crypto~~: fixed by EN-06 (CryptoManager v2 is fail-closed and versioned).
- **No DB migration path** (`version = 1`, `exportSchema = false`): the first schema change risks crashes or data loss.
- **Decrypt-on-list:** every list emission decrypts every row on the main-dispatcher collector, although the list shows no passwords.
- **Two dynamic-color mechanisms:** `DynamicColors.applyToActivitiesIfAvailable` (View theme) in `App` and the `KupassTheme(dynamicColor)` Compose scheme in `MainActivity`. Only the latter affects Compose UI. [TODO] confirm whether the View-level call is still needed (splash/system dialogs).

## 6) Evidence

- `app/src/main/java/com/nielcode/kupass/App.kt`, `MainActivity.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/MainAppScreen.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/home/HomeViewModel.kt`, `editor/PasswordEditorViewModel.kt`, `detail/PasswordDetailViewModel.kt`
- `app/src/main/java/com/nielcode/kupass/data/repository/PasswordRepository.kt`
- `app/src/main/java/com/nielcode/kupass/data/local/db/KupassDatabase.kt`, `PasswordDao.kt`
- `app/src/main/java/com/nielcode/kupass/utils/CryptoManager.kt`, `data/backup/BackupCodec.kt`
- `app/src/main/java/com/nielcode/kupass/ui/theme/Theme.kt`
