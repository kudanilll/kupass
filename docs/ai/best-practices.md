# Best Practices

Engineering guidelines for Kupass. When an API detail matters, confirm it against current docs via context7. Versions here are recent (Kotlin 2.4, AGP 9.x, Compose BOM 2026.06).

## Security (highest priority)

- **Crypto**
  - Use Android Keystore-backed keys and `AES/GCM/NoPadding` with a fresh random IV per encryption (the Keystore generates it). Never reuse IVs or hardcode keys.
  - For user-password-derived keys (backups), use a slow KDF (PBKDF2-HMAC-SHA256 with a high iteration count, or Argon2 if a light dependency is acceptable) and a random salt stored with the file.
  - Version every ciphertext format (e.g. a prefix byte or a JSON `version` field) so the scheme can change later without breaking old data.
  - Fail closed: throw or return a typed error. Never fall back to plaintext, and never swallow crypto exceptions with `printStackTrace()`.
  - Keep secrets as `CharArray`/`ByteArray` where practical and don't put them in `toString()`, logs, crash reports, or navigation arguments (pass IDs, not secrets).
- **Storage & backup:** keep `allowBackup="false"`, and also exclude sensitive domains in `data_extraction_rules.xml` (`device-transfer` still runs on some OEMs even with `allowBackup="false"`).
- **Clipboard:** mark sensitive clips (`ClipDescription.EXTRA_IS_SENSITIVE`, API 33+) and clear them after a timeout on every API level.
- **Screens:** keep `FLAG_SECURE`. Don't show secrets in notifications or recents.
- **Intents:** keep only the launcher activity exported. Validate every incoming URI/intent (see the `android-intent-security` skill). Guard `startActivity` against `ActivityNotFoundException`.
- **Release:** R8 on, no debug logging in release, and no secrets in `BuildConfig` (an APK is fully recoverable).

## Kotlin

- Prefer immutable `val`, `data class`/`data object`, and `sealed interface` for UI state and results.
- Use coroutines with structured concurrency: `viewModelScope`, and `withContext(Dispatchers.Default)` for crypto over lists. Room suspend/Flow APIs already run off the main thread.
- No `!!` in production code. Handle nullability explicitly.
- Model one-shot events as a typed sealed class (e.g. `sealed interface DataEvent`), not string keys like `"export_success"`.

## Jetpack Compose

- Keep screens stateless where possible. Hoist state to ViewModels and expose a single `StateFlow<UiState>` per screen.
- Collect with `collectAsStateWithLifecycle()` (lifecycle-runtime-compose) rather than `collectAsState()`.
- Give `LazyColumn` items stable keys (`key = { it.id }`, already done), and avoid heavy work inside item lambdas.
- Use `remember`/`derivedStateOf` for derived values. Don't read `SharedPreferences` or do I/O in composition.
- Handle insets with `Scaffold` + `WindowInsets` (the app is edge-to-edge; see the `edge-to-edge` skill).
- Use `stringResource()` for all visible text, and `contentDescription` for meaningful icons (accessibility).
- Add `@Preview` for new components.

## Room

- `exportSchema = true` with `room.schemaLocation` configured, and commit the schema JSON.
- Every version bump needs a `Migration` (or `AutoMigration`) and a `MigrationTestHelper` test.
- Wrap multi-row writes (import) in `@Transaction` or `withTransaction {}`.
- Use `@Insert(onConflict = ABORT/IGNORE)` deliberately. `REPLACE` deletes and reinserts the row.

## Architecture

- Keep the layers UI → ViewModel → Repository → DAO/CryptoManager. The UI never imports DAO or crypto classes.
- If the object graph grows, prefer lightweight manual DI (an `AppContainer` in `App`) over adding Hilt, to keep the app light. Discuss it before adding a DI framework.

## Performance & Size

- Measure before optimizing: use the `android-profiler` skill for startup, jank, and memory.
- Consider a Baseline Profile for startup (proposed).
- Avoid decrypting every password just to render the list. Only the detail/copy path needs plaintext (see `docs/codebase/CONCERNS.md` §4).
- Keep dependencies minimal. `material-icons-extended` is large, although R8 strips unused icons. Check with the `r8-analyzer` skill when size matters.
- Keep release `isMinifyEnabled = true` and `isShrinkResources = true`, and add keep rules only when needed.

## Testing

- Unit tests (JVM/Robolectric) for the repository, crypto, import/export, and ViewModel logic.
- Room migration tests for every schema change.
- Compose UI tests for critical flows: create, edit, delete confirmation, import/export.
- Test names in backticks describe behavior, as in the existing tests.
