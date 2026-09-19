# Coding Conventions

> Generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`). Describes what the code **does**. For what it **should** do, see `docs/ai/best-practices.md`.

## 1) Naming Rules

| Item                 | Rule                                                          | Example                                                        | Evidence                                    |
| -------------------- | ------------------------------------------------------------- | -------------------------------------------------------------- | ------------------------------------------- |
| Files                | PascalCase = main declaration                                 | `PasswordEditorViewModel.kt`                                   | `ui/screens/editor/`                        |
| Screens / ViewModels | `<Feature>Screen`, `<Feature>ViewModel`                       | `PasswordDetailScreen`, `PasswordDetailViewModel`              | `ui/screens/detail/`                        |
| Composables          | PascalCase functions                                          | `VaultList`, `SectionItem`, `DetailField`                      | `ui/components/`, `PasswordDetailScreen.kt` |
| Functions/methods    | camelCase verbs                                               | `savePassword`, `loadPassword`, `onSearchQueryChange`          | ViewModels                                  |
| Backing properties   | `_name` private `MutableStateFlow` + public `name: StateFlow` | `_saveState` / `saveState`                                     | `PasswordEditorViewModel.kt`                |
| Types                | PascalCase. Sealed interfaces for states                      | `SaveState`, `DeleteState`                                     | ViewModels                                  |
| Constants            | `UPPER_SNAKE` in `companion object` or nested `object`s       | `AppConfig.Theme.Code.DARK`, `KEY_LANGUAGE`                    | `AppConfig.kt`, `PreferenceManager.kt`      |
| Room columns         | snake_case via `@ColumnInfo`                                  | `site_name`, `created_at`                                      | `PasswordEntity.kt`                         |
| Routes               | PascalCase `@Serializable` object/data class                  | `HomeBase`, `PasswordEditor`                                   | `MainAppScreen.kt`                          |
| Tests                | Backtick sentence names                                       | `` `v2 file contains no plaintext vault data` ``                        | `BackupCodecTest.kt`                   |
| String resources     | snake_case with a prefix by kind                              | `toast_success_export`, `dialog_title_delete`, `button_cancel` | `res/values/strings.xml`                    |

## 2) Formatting and Linting

- Formatter: none enforced in the build. `kotlin.code.style=official` (`gradle.properties`). The google-java-format IDE plugin is enabled in `.idea/`, which is gitignored, so it isn't shared. The code shows a mix of `ktfmt`-style (4-space, trailing-lambda wrapping) and Android Studio default formatting.
- Linter: Android Lint defaults only. No `lint.xml`, detekt, or ktlint.
- Run: `./gradlew :app:lintDebug`.
- Decision (2026-09-18): enforce a formatter and static analysis in Gradle + CI (PRD QA-1/QA-2). Not implemented yet.

## 3) Import and Module Conventions

- Explicit imports, no wildcards (observed across `ui/` and `data/`).
- There is one fully-qualified call instead of an import: `com.nielcode.kupass.ui.screens.data.DataScreen(...)` in `MainAppScreen.kt`.
- No barrel files or aliases. The package equals the directory, except `editor/components/TextField.kt` (package `ui.components`).

## 4) Error and Logging Conventions

- **ViewModels:** `try/catch` around suspend work. Screen state is a `StateFlow` (`SaveState`, `DeleteState`, both `Error` variants rendered as toasts). One-shot results are a `sealed interface` sent through a `Channel` (`VaultEvent`, mapped to text by `VaultEvent.message(context)`).
- **Crypto:** fail-closed. `CryptoException` (message only, never secret values) is thrown by `CryptoManager` and caught in ViewModels (`.catch {}` on read flows, `try/catch` on writes). No `printStackTrace`/logging in crypto paths.
- **UI feedback:** `Toast` with text from resources (plurals for counts). Compose collects state with `collectAsStateWithLifecycle()`.
- **Logging:** there are no `Log.*` calls and no `printStackTrace()` in production code.
- **Redaction:** no explicit policy in code. Secrets are simply never logged, apart from the stack trace above, which carries no secret value.

## 5) Testing Conventions

- Location: `app/src/test/java/<same package as source>/<Class>Test.kt`.
- Style: Arrange / Act / Assert comments, JUnit4 `assertEquals`/`assertTrue`, `@RunWith(RobolectricTestRunner::class)`.
- Mocking: none. The tests use the real `CryptoManager`, which switches to an in-memory AES key when AndroidKeyStore is unavailable.
- Coverage expectation: [TODO] none configured.

## 6) Localization Conventions

- Every resource string exists in `values/strings.xml` (EN) and `values-in/strings.xml` (ID): 81 strings each.
- The language setting stores an **index** (`AppConfig.Language.Code`) into `arrays.xml` `language_values` (`en-US`, `id-ID`). `AppConfig.Language.ENGLISH_TAG`/`INDONESIA_TAG` are unused.
- Known hardcoded strings: see `CONCERNS.md`.

## 7) Evidence

- `gradle.properties`, `.idea/google-java-format.xml`
- `app/src/main/java/com/nielcode/kupass/ui/screens/editor/PasswordEditorViewModel.kt`
- `app/src/main/java/com/nielcode/kupass/utils/CryptoManager.kt`, `AppConfig.kt`
- `app/src/main/res/values/strings.xml`, `values-in/strings.xml`, `values/arrays.xml`
- `app/src/test/java/com/nielcode/kupass/data/backup/BackupCodecTest.kt`
