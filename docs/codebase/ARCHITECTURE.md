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
5. `HomeViewModel.allPasswords` caches the decrypted vault; `passwords` remains the filtered Flow API. Home and the group page use the full cache so `VaultList` can filter results while keeping accurate total group counts, without decrypting on each keystroke.

**Grouped Home presentation:** `ui/screens/home/VaultGrouping.kt` gives each group a typed, namespaced identity: normalized HTTP(S) host (ROOT lowercase, `www.` stripped; credentials, port, path, query and fragment ignored), Android facet package (certificate ignored, handled before web parsing), or trimmed ROOT-lowercase site name for missing/invalid URLs. Private hosts, local names and IPs are accepted locally; subdomains remain distinct. Grouping never uses the public-only, domain-guessing `siteDomainOf` icon helper. Groups are alphabetically ordered; lazy keys are `group:<namespace>:<value>` and `account:<id>`.

Multi-account groups reuse `PasswordListItem` with a count badge over the icon's physical left corner. Tapping navigates to public `PasswordGroup(groupId)` with a representative row ID, never a site name, URL, or password. `PasswordGroupScreen` lists the original accounts and forwards child IDs through the existing authentication request on its own navigation entry. It retains its group key only in memory and saves a replacement row ID after the anchor is removed, so recreation can resolve the remaining group without saving vault fields. Singletons remain ordinary account items. Search matches name/username/URL/notes, leaves group badges at their full count, animates headline removal/list placement, and smoothly scrolls to the search input. No expanding search popup is composed.

**Account gestures:** `RightwardDeleteGesture.kt` observes DOWN unconsumed on Initial, checks movement against touch slop, and claims only predominant physical +X before the pager's Main pass. Leftward/vertical movement stays unconsumed; consumption seen on Initial or on the pre-claim Final pass, lost pointers, and multitouch abandon the gesture until all pointers lift. After claiming, reversals retain ownership. Release beyond 35% of row width (after slop) opens the existing exact-account confirmation once, and every completion/cancel resets the row. The trash background is on the physical left; the account's accessibility delete action opens the same confirmation. No pager rewrite, synthetic drag, or authentication wiring is involved.

**Export:** `DataScreen` → fresh native authentication → request-bound consent → `ExportPasswordDialog` (backup password ≥ 8 chars, confirmed) → consent transitions to picker → `BackupViewModel.prepareExport(CharArray)` (binds password to request token) → SAF `CreateDocument("application/octet-stream")`, suggested name `kupass-backup.kupass` → validate outstanding picker request → `exportPasswords(uri)` consumes the matching single-use write permission before `repository.getAllPasswords().first()` or any stream → `BackupCodec.encode` on the CPU dispatcher (unchanged v2 JSON envelope, PBKDF2 600k + AES-GCM/AAD) → encrypted write → password array zeroed. Consent expires after five minutes. Cancellation, navigation/tab departure, credential removal, timeout, or a stale result clears pending export through `cancelExport`. Exports exceeding the import byte/entry limits fail before writing. Import remains public but is blocked during pending export authentication, consent, or backup work. No CSV export.

**Account access:** public Home keeps `onNavigateToDetail(Long)`. It requests authentication on the originating navigation entry; only a current successful request navigates and grants the newly created detail entry. Actual detail and positive-ID editor destination content checks the activity-retained `EntryAuthenticationViewModel` grant before invoking the screen (including default ViewModel creation). A restored route has no grant. Authorized detail-to-editor navigation copies permission to that editor entry and retains its parent detail; Home ends the chain. Rotation reattaches native callbacks using the retained request token without re-authenticating. Native requests are serialized until terminal callbacks drain; no closures are retained in grants or singletons. Each controller constructs and privately owns its `AppLock` absence timer from the timeout preference; `AppContainer` stores no timer. Prompts in a second activity cannot reset the first activity's absence or revive its grants. Picker/security-settings/browser/license allowances go through the launching activity's controller, retaining the existing bounded grace. Disposed sensitive screens cancel their loading jobs and drop decrypted ViewModel fields. Positive-ID editors refuse save when their original entry cannot be loaded.

**Import:** SAF `OpenDocument` (`*/*`, including provider-mislabeled files) → bounded, strict Unicode read (≤ 32 MiB; UTF-8 or BOM-marked UTF-16) off main → content validation off main → v2 encrypted JSON password prompt, v1 legacy checks, or Google CSV validation → repository encryption/atomic insert → imported/skipped result. CSV requires `name,url,username,password` plus optional `note`/`notes`, normalizing header case/spaces only. Comma/semicolon separators, LF/CRLF/CR records, blank lines and quoted multiline fields are supported. Credentials are never trimmed or substituted. Invalid records are skipped; malformed quoting/header/width/encoding or over 10,000 nonblank data records reject the file before writes. CSV parsing failures use `CsvImportFailed`, while unreadable/invalid backups use a format-neutral message. Busy guards and auth-bound export permissions remain unchanged.

**Pager and processing UI:** Data forces the floating navigation visible regardless of nested-scroll direction. Its normal vertical scroll handles overflowing content; when content fits, a zero-consumption native `scrollableArea` receives drag input for overscroll stretch. Home/Settings share `BottomFade`, and the navigation/FAB use a zero-offset `dropShadow`. `BackupViewModel.operation` identifies real Import/Export work for the indeterminate, non-dismissible progress dialog; no artificial percentage or delay is introduced.

**Imported-entry editing:** the protected editor accepts every nonempty password, including whitespace-only CSV credentials; empty passwords and blank site names disable Save. Updates preserve username, password, and notes verbatim, including multiline note formatting, along with the existing ID and creation timestamp. New-entry username/note trimming and existing site/URL normalization remain unchanged. The import API also blocks cancelled-but-draining native export requests until their terminal callback clears `nativeRequest`, before any provider or vault access.

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
| `GooglePasswordCsv` / `BackupInput` | Google-schema CSV validation, bounded strict Unicode decoding | Database writes, network calls, format selection by extension | `data/backup/` |
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
| Stateless screens, state hoisted to the pager | `MainPagerScreen` collects `HomeViewModel` + auth-bound `BackupViewModel`; `MainPagerContent` renders Home/Data/Settings from state and callbacks | No ViewModel forwarding between composables (compose-rules) |

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
