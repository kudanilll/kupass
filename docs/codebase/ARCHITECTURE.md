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
    -> VaultCache (decrypted rows by ciphertext fingerprint) -> VaultSnapshotStore (encrypted, no_backup)
    -> PasswordDao (Room, Flow/suspend) -> SQLite "kupass_database"
    <- Flow<List<PasswordEntity>> -> StateFlow -> collectAsStateWithLifecycle() -> recomposition
```

**Save a password** (create):

1. `PasswordEditorScreen` holds the form state in `remember { mutableStateOf }` and calls `viewModel.savePassword(...)` from the top-bar check button.
2. `PasswordEditorViewModel.savePassword` trims the fields, builds a `PasswordEntity`, and sets `SaveState.Saving`.
3. `PasswordRepository.insertPassword` → `CryptoManager.encrypt` on every text field (on `Dispatchers.Default`) → `PasswordDao.insert` (REPLACE).
4. On success `SaveState.Success` is set, and the screen's `LaunchedEffect(saveState)` pops the back stack.
5. The repository remembered the saved row's plaintext, so the next vault emission decrypts nothing. `HomeViewModel.uiState` is `Loading` until the first read, then `Ready(all, matching, visibleGroups)`; the Vault tab shows skeleton rows while loading and the group page keeps the full groups, so `VaultList` filters without decrypting on each keystroke.

**Vault cache:** `VaultCache` keys decrypted fields by a SHA-256 fingerprint of each row's ciphertext (fresh IV per encryption, so equal fingerprints mean equal plaintext). Each emission decrypts only unseen rows and keeps exactly the current rows. Writes (`insert`, `update`, `import`, format upgrade) remember their plaintext until Room returns the rows; a failed write forgets it. After each emission `VaultSnapshotStore` writes the vault to `noBackupFilesDir/vault-cache`, sealed with a per-write AES-256-GCM key wrapped by the Keystore key, so a cold start costs one Keystore operation. Unreadable snapshots are deleted. `getPasswordById` (detail/editor) bypasses the cache to stay fail-closed.

**Grouped Home presentation:** `ui/screens/home/VaultGrouping.kt` gives each group a typed, namespaced identity: normalized HTTP(S) host (ROOT lowercase, `www.` stripped; credentials, port, path, query and fragment ignored), Android facet package (certificate ignored, handled before web parsing), or trimmed ROOT-lowercase site name for missing/invalid URLs. Private hosts, local names and IPs are accepted locally; subdomains remain distinct. Grouping never uses the public-only, domain-guessing `siteDomainOf` icon helper. Groups are alphabetically ordered; lazy keys are `group:<namespace>:<value>` and `account:<id>`.

Multi-account groups reuse `PasswordListItem` with a primary-color count badge at the trailing right side. Tapping navigates to public `PasswordGroup(groupId)` with a representative row ID, never vault fields. `PasswordGroupScreen` has no search input and forwards child IDs through the authentication controller. A valid account session approves new destination-bound grants without another native prompt. Its group key stays in memory; only a replacement anchor ID is saved after deletion. Home retains live animated search and full group counts.

**Account session:** `State.sessionUnlocked` is set only by successful account/unlock authentication, never merely by arming the absence timer. Public navigation clears old destination grants but keeps the session; the next account request checks the timer before creating a new entry/account-bound grant. Background expiry, credential removal, explicit revocation and process death invalidate the session. Export never uses session reuse: each request still needs a new native success and single-use write consent.

**Account gestures:** `RightwardDeleteGesture.kt` observes DOWN unconsumed on Initial, checks movement against touch slop, and claims only predominant physical +X before the pager's Main pass. Leftward/vertical movement stays unconsumed; consumption seen on Initial or on the pre-claim Final pass, lost pointers, and multitouch abandon the gesture until all pointers lift. After claiming, reversals retain ownership. Release beyond 35% of row width (after slop) opens the existing exact-account confirmation once, and every completion/cancel resets the row. The trash background is on the physical left; the account's accessibility delete action opens the same confirmation. No pager rewrite, synthetic drag, or authentication wiring is involved.

**Export:** `DataScreen` → fresh native authentication → request-bound consent → `ExportPasswordDialog` (backup password ≥ 8 chars, confirmed) → consent transitions to picker → `BackupViewModel.prepareExport(CharArray)` (binds password to request token) → SAF `CreateDocument("application/octet-stream")`, suggested name `kupass-backup.kupass` → validate outstanding picker request → `exportPasswords(uri)` consumes the matching single-use write permission before `repository.getAllPasswords().first()` or any stream → `BackupCodec.encode` on the CPU dispatcher (unchanged v2 JSON envelope, PBKDF2 600k + AES-GCM/AAD) → encrypted write → password array zeroed. Consent expires after five minutes. Cancellation, navigation/tab departure, credential removal, timeout, or a stale result clears pending export through `cancelExport`. Exports exceeding the import byte/entry limits fail before writing. Import remains public but is blocked during pending export authentication, consent, or backup work. No CSV export.

**Account access:** Home/group pages keep `onNavigateToDetail(Long)`. The controller checks the activity-local session before approving a new request; if locked, it requires native success first. Navigation grants bind the created detail entry and account ID before any sensitive screen/ViewModel is invoked. Home ends old destination grants without ending a valid session. Detail/editor/back preserves its grant chain; process restoration starts without authority. Native requests serialize and rotate through retained tokens. Every controller privately owns its timer; another activity cannot extend its session or exemptions. Disposed sensitive screens clear decrypted fields, and failed positive-ID editor loads cannot become new accounts.

**Import:** SAF `OpenDocument` (`*/*`, including provider-mislabeled files) → bounded, strict Unicode read (≤ 32 MiB; UTF-8 or BOM-marked UTF-16) off main → content validation off main → v2 encrypted JSON password prompt, v1 legacy checks, or Google CSV validation → repository encryption/atomic insert → imported/skipped result. CSV requires `name,url,username,password` plus optional `note`/`notes`, normalizing header case/spaces only. Comma/semicolon separators, LF/CRLF/CR records, blank lines and quoted multiline fields are supported. Credentials are never trimmed or substituted. Invalid records are skipped; malformed quoting/header/width/encoding or over 10,000 nonblank data records reject the file before writes. CSV parsing failures use `CsvImportFailed`, while unreadable/invalid backups use a format-neutral message. Busy guards and auth-bound export permissions remain unchanged.

**Pager and processing UI:** Data forces navigation visible. `PageScroll.kt` shares native overflow scrolling and zero-consumption stretch for fitting content; Home/group lazy lists likewise use an outer stretch receiver when they cannot scroll. Settings, detail, editor and lock pages share this behavior. Home/Settings share `BottomFade`; navigation/FAB shadows use zero offset. Processing dialogs show real Import/Export work, without fake progress or delays. Import provider-open failures use `ImportReadFailed`; repository/crypto failures use `ImportStoreFailed`; format and CSV errors remain separate.

**Imported-entry editing:** the protected editor accepts every nonempty password, including whitespace-only CSV credentials; empty passwords and blank site names disable Save. Updates preserve username, password, and notes verbatim, including multiline note formatting, along with the existing ID and creation timestamp. New-entry username/note trimming and existing site/URL normalization remain unchanged. The import API also blocks cancelled-but-draining native export requests until their terminal callback clears `nativeRequest`, before any provider or vault access.

## 3) Layer/Module Responsibilities

| Layer or module                                             | Owns                                                                                                                                   | Must not own                                                            | Evidence                                                                             |
| ----------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| `App`                                                       | Process-wide setup: FLAG_SECURE, locale, night mode, dynamic colors                                                                    | Vault data                                                              | `App.kt`                                                                             |
| `AppLock` + `EntryAuthenticationViewModel` + `MainActivity` | Activity-local absence timer and bounded exemptions, memory-only request/entry grants, native prompt session and rotation reattachment | Vault data, process-wide timers, persisted grants, composition closures | `security/AppLock.kt`, `security/EntryAuthenticationViewModel.kt`, `MainActivity.kt` |
| Compose screens                                             | Rendering, ephemeral UI state (dialogs, visibility toggles, form fields), toasts, clipboard                                            | Persistence, crypto                                                     | `ui/screens/**`                                                                      |
| ViewModels                                                  | Screen state as `StateFlow`, one-shot results (`SaveState`, `DeleteState`, `VaultEvent` channel), coroutines                           | Android Views, `Context` beyond `Application`                           | `*ViewModel.kt`                                                                      |
| `PasswordRepository`                                        | Vault API for ViewModels, transparent full-field crypto, in-memory sort/search, legacy-format upgrade                                  | UI state                                                                | `PasswordRepository.kt`                                                              |
| `PasswordDao` / `KupassDatabase`                            | SQL, reactive queries, singleton DB                                                                                                    | Crypto                                                                  | `data/local/db/*`                                                                    |
| `BackupCodec`                                               | Backup file format                                                                                                                     | File I/O (done in the ViewModel)                                        | `BackupCodec.kt`                                                                     |
| `GooglePasswordCsv` / `BackupInput`                         | Google-schema CSV validation, bounded strict Unicode decoding                                                                          | Database writes, network calls, format selection by extension           | `data/backup/`                                                                       |
| `CryptoManager`                                             | Key creation/lookup, AES-GCM encrypt/decrypt                                                                                           | Knowledge of entities                                                   | `CryptoManager.kt`                                                                   |
| `PreferenceManager`                                         | Settings persistence                                                                                                                   | Applying settings (done in `App`/`SettingsScreen`)                      | `PreferenceManager.kt`                                                               |

## 4) Reused Patterns

| Pattern                                                      | Where found                                                                                                                                       | Why it exists                                                        |
| ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| Double-checked-locking singleton                             | `KupassDatabase.getInstance`                                                                                                                      | One Room instance per process                                        |
| Kotlin `object` singleton                                    | `CryptoManager`, `BackupCodec`, `AppConfig`                                                                                                       | Stateless utilities                                                  |
| Repository (thin, with a crypto decorator)                   | `PasswordRepository`                                                                                                                              | Keep the DAO and UI unaware of encryption                            |
| Manual DI container + `viewModelFactory { initializer { } }` | `di/AppContainer.kt`, `*ViewModel.Factory`                                                                                                        | Constructor injection without a DI framework (lightweight, testable) |
| Sealed interface operation state                             | `SaveState`, `DeleteState`                                                                                                                        | Drive navigation after async work via `LaunchedEffect`               |
| Sealed one-shot events over a `Channel`                      | `HomeViewModel.events`, `BackupViewModel.events` (`VaultEvent`) → `MainAppScreen` (`repeatOnLifecycle`)                                           | Toasts after read failures and export/import, delivered exactly once |
| Sealed UI state + `combine` + `stateIn(Eagerly)`             | `HomeViewModel.uiState` (`VaultUiState.Loading` / `Ready`)                                                                                        | Loading and data change in one emission; no empty-state flash        |
| Type-safe navigation with `@Serializable` routes             | `MainAppScreen`                                                                                                                                   | Compile-time route args                                              |
| Stateless screens, state hoisted to the pager                | `MainPagerScreen` collects `HomeViewModel` + auth-bound `BackupViewModel`; `MainPagerContent` renders Home/Data/Settings from state and callbacks | No ViewModel forwarding between composables (compose-rules)          |

## 5) Known Architectural Risks

- ~~Backups bound to one device key~~: fixed by F-2.1 (password-based portable backups). The _database_ is still bound to the device Keystore key by design.
- ~~Fail-open crypto~~: fixed by EN-06 (CryptoManager v2 is fail-closed and versioned).
- ~~No DB migration path~~: fixed by EN-03 (exported schemas, `MIGRATIONS` registry, `KupassDatabaseMigrationTest`).
- **First start without a snapshot:** after install, an update that discards the snapshot, or a Keystore key change, the first read still decrypts every field of every row once (one Keystore operation per field). Later changes decrypt only new rows. See CONCERNS §4.
- **Two dynamic-color mechanisms:** `DynamicColors.applyToActivitiesIfAvailable` (View theme) in `App` and the `KupassTheme(dynamicColor)` Compose scheme in `MainActivity`. Only the latter affects Compose UI. [TODO] confirm whether the View-level call is still needed (splash/system dialogs).

## 6) Evidence

- `app/src/main/java/com/nielcode/kupass/App.kt`, `MainActivity.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/MainAppScreen.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/home/HomeViewModel.kt`, `editor/PasswordEditorViewModel.kt`, `detail/PasswordDetailViewModel.kt`
- `app/src/main/java/com/nielcode/kupass/data/repository/PasswordRepository.kt`
- `app/src/main/java/com/nielcode/kupass/data/local/db/KupassDatabase.kt`, `PasswordDao.kt`
- `app/src/main/java/com/nielcode/kupass/security/CryptoManager.kt`, `data/backup/BackupCodec.kt`
- `app/src/main/java/com/nielcode/kupass/ui/theme/Theme.kt`
