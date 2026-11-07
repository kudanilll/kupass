# Codebase Structure

> Generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`).

## 1) Top-Level Map

| Path                                                        | Purpose                                                                                                                                                                                                                                                                                                                                                                        | Evidence                                                                                    |
| ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------- |
| `app/`                                                      | Android application module `:app`: activities, navigation, screens, ViewModels, `AppContainer`, backup/CSV, site icons, app-lock UI. Unit tests in `src/test/`, backup KDF device test in `src/androidTest/` | `settings.gradle.kts` |
| `app/src/main/res/`                                         | Resources: `values/` (EN), `values-in/` (ID), `values-night/`, `xml/` (backup rules), mipmaps, drawables | directory listing |
| `core/security/`                                            | `:core:security`: `CryptoManager` (Keystore AES-GCM), `PasswordGenerator`, `AppLock` timer. Keystore device tests in `src/androidTest/` | `core/security/build.gradle.kts` |
| `core/data/`                                                | `:core:data`: Room (entity, DAO, database), `PasswordRepository`, `VaultCache`, sealed caches, `PreferenceManager`, `AppConfig`. Migration and legacy-upgrade device tests in `src/androidTest/` | `core/data/build.gradle.kts` |
| `core/data/schemas/`                                        | Room exported schemas (`<version>.json`), committed. Source of truth for migration tests | `core/data/build.gradle.kts` (`room { schemaDirectory }`) |
| `core/designsystem/`                                        | `:core:designsystem`: Kupass theme (`Color`, `Theme`, `Type`) and the `res/font/` files | `core/designsystem/build.gradle.kts` |
| `core/testing/`                                             | `:core:testing`: shared test fakes (`FakePasswordDao`, `MainDispatcherRule`), used only as `testImplementation` | `core/testing/build.gradle.kts` |
| `build-logic/`                                              | Included build with the convention plugins `kupass.android.application`, `kupass.android.library`, `kupass.android.compose` (SDK levels, Java 11, Robolectric args, detekt) | `settings.gradle.kts` (`includeBuild`) |
| `gradle/`                                                   | Version catalog, wrapper, daemon JVM toolchain                                                                                                                                                                                                                                                                                                                                 | `libs.versions.toml`                                                                        |
| `config/detekt/detekt.yml`                                  | detekt overrides on top of the defaults (Compose rules enabled)                                                                                                                                                                                                                                                                                                                | `build-logic/.../Detekt.kt`                                                                 |
| `docs/ai/`                                                  | Agent rules, PRD, best practices                                                                                                                                                                                                                                                                                                                                               | this repo                                                                                   |
| `docs/codebase/`                                            | Evidence-based codebase map (these docs)                                                                                                                                                                                                                                                                                                                                       | this repo                                                                                   |
| `image/`                                                    | README banner and screenshots                                                                                                                                                                                                                                                                                                                                                  | `README.md`                                                                                 |
| `.github/`                                                  | `dependabot.yml` (Gradle + Actions updates, weekly, target `master`), `workflows/ci.yml` (checks on push/PR + dependency graph), `workflows/release.yml` (signed APK + GitHub Release on tag `vX.Y.Z`), `ISSUE_TEMPLATE/` (bug report and feature request forms, security contact link), `PULL_REQUEST_TEMPLATE.md`. `CONTRIBUTING.md` and `SECURITY.md` live at the repo root | this repo                                                                                   |
| `CHANGELOG.md`                                              | Keep a Changelog. The release workflow publishes the `## [X.Y.Z]` section as release notes                                                                                                                                                                                                                                                                                     | this repo                                                                                   |
| `CLAUDE.md`                                                 | Short agent overview. Links here                                                                                                                                                                                                                                                                                                                                               | repo root                                                                                   |
| `secrets.properties`, `release-key.jks`, `local.properties` | Local-only signing/SDK config. Gitignored                                                                                                                                                                                                                                                                                                                                      | `.gitignore`                                                                                |
| `app/release/`, `build/`, `.gradle/`, `.idea/`, `.kotlin/`  | Build output and IDE state. Gitignored, don't document or edit                                                                                                                                                                                                                                                                                                                 | `.gitignore`                                                                                |

## 2) Entry Points

- Process entry: `App.kt` (`android:name=".App"`). It blocks screenshots on every Activity (`FLAG_SECURE`) and applies the saved locale, night mode, and dynamic colors.
- UI entry: `MainActivity.kt`, the only exported launcher Activity. It installs splash/edge-to-edge, reattaches `BiometricPrompt` to the activity-retained authentication request, and always hosts public `MainAppScreen`. Sensitive destinations show `LockScreen` until their entry/account-bound grant permits screen/ViewModel creation. There is no global launch gate.
- Navigation root: `ui/screens/MainAppScreen.kt` (`NavHost`, start destination `HomeBase`).
- Secondary entry points: none (no services, receivers, providers, or workers in `AndroidManifest.xml`).

## 3) Source Map

Kotlin packages are shared across modules (files kept their packages when they moved):
`security/{CryptoManager,PasswordGenerator,AppLock}` live in `:core:security`;
`data/local/{db,prefs,cache}`, `data/repository/` and `utils/AppConfig` in `:core:data`;
`ui/theme/{Color,Theme,Type}` in `:core:designsystem`; everything else in `:app`.
Dependency rules: `app → core:*`, `core:data → core:security`; core modules never depend on `app`.

```plaintext
com/nielcode/kupass/
├── App.kt                         Application: FLAG_SECURE, locale, theme, dynamic colors
├── MainActivity.kt                AppCompatActivity host for Compose
├── di/AppContainer.kt             manual DI: repository, prefs, ContentResolver; `CreationExtras.appContainer()`
├── security/CryptoManager.kt     Keystore AES-GCM (`kp2:` format), CryptoException, alias "kupass_vault_key"
├── security/AppLock.kt           sensitive-access background timer (timeout, bounded exemptions)
├── security/EntryAuthenticationViewModel.kt activity-retained native requests, entry-bound grants, single-use export consent
├── security/SecureClipboard.kt   sensitive clipboard copy + 45 s auto-clear on every API level
├── data/
│   ├── local/db/
│   │   ├── KupassDatabase.kt      Room singleton "kupass_database", version 1, exportSchema=true, `MIGRATIONS` registry
│   │   ├── PasswordDao.kt         Flow getAll/getById, getAllOnce, insert(REPLACE)/update/updateAll/delete (no SQL search: data is ciphertext)
│   │   └── PasswordEntity.kt      table "passwords"
│   ├── backup/BackupCodec.kt      portable backup v2 (PBKDF2 + AES-GCM) + strict legacy v1 import
│   │   ├── GooglePasswordCsv.kt   strict Google/Chromium CSV import, optional note, exact credential fields
│   │   └── BackupInput.kt         bounded strict UTF-8 input (32 MiB, optional BOM)
│   ├── local/prefs/PreferenceManager.kt SharedPreferences "kupass_preferences"
│   ├── siteicon/                  site icons: SiteDomain (domain extraction), FavgetIconSource (HTTP), SiteIconRepository (memory + sealed disk cache, dedupe, decode)
│   ├── local/cache/               SealedFile (encrypted file), VaultSnapshotStore (vault-cache), SiteIconStore (site-icons), all in noBackupFilesDir
│   └── repository/PasswordRepository.kt encrypt/decrypt all text fields, in-memory sort+search, legacy format upgrade
│       └── repository/VaultEntityMapping.kt  field encrypt/decrypt, identity, search match, sort
├── ui/
│   ├── components/                BottomNav (+ navSpring, BottomNavHeight), MainTab, EmptyState, SectionHeader, SectionItem, VaultTextField (+ PasswordVisibilityToggle), DeletePasswordDialog, SingleChoiceDialog
│   ├── screens/
│   │   ├── MainAppScreen.kt       routes, state-hoisting MainPagerScreen + stateless MainPagerContent, hide-on-scroll BottomNav, auth-bound backup dialogs + SAF launchers, event toasts
│   │   ├── EntryNavigation.kt     protected detail/editor route registration and pre-ViewModel authorization gate
│   │   ├── FlowDefaults.kt        `WhileUiSubscribed`, `recoverable {}` (I/O, SecurityException, crypto, SQL)
│   │   ├── VaultEvent.kt          one-shot UI events (toasts) shared by Home and Backup
│   │   ├── PageInsets.kt          `pagerPageInsets()`: pager pages take top/side insets, bottom comes from the floating nav
│   │   ├── home/                  HomeScreen (stateless), HomeViewModel (list, search, delete), components/{VaultList, PasswordListItem}
│   │   ├── data/DataScreen.kt     export/import buttons only
│   │   ├── data/BackupPasswordDialog.kt  export/import password dialogs + progress
│   │   ├── data/BackupViewModel.kt  auth-bound export token/password, strict JSON/Google CSV import, encrypted-import prompt, synchronous busy state, events
│   │   ├── lock/LockScreen.kt     locked state / "set up a screen lock" guidance
│   │   ├── detail/                PasswordDetailScreen (+ copyToClipboard), PasswordDetailViewModel
│   │   ├── editor/                PasswordEditorScreen (EditorFormState, top bar, form), PasswordEditorViewModel
│   │   └── settings/SettingsScreen.kt   language/auto-lock/theme/dynamic color dialogs, about links, SingleChoiceDialog
│   └── theme/                     Color.kt, Theme.kt (KupassTheme), Type.kt (Heming display, Google Sans Flex body) + AppearanceSettings.kt (apply language/night mode)
└── utils/
    ├── AppConfig.kt               int codes for language/theme/dynamic color
    └── UrlUtils.kt                openUrl() with ActivityNotFoundException guard
```

### Navigation routes (`MainAppScreen.kt`)

| Route            | Args                                          | Screen                                                                                                               |
| ---------------- | --------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `HomeBase`       | none                                          | `MainPagerScreen`: `HorizontalPager` pages 0 Home, 1 Data, 2 Settings                                                |
| `PasswordDetail` | `passwordId: Long`                            | `PasswordDetailScreen`                                                                                               |
| `PasswordGroup`  | `groupId: Long` (representative row ID)       | Public `PasswordGroupScreen`; only IDs are saved, and child account navigation uses the existing authentication gate |
| `PasswordEditor` | `passwordId: Long = -1L` (create when `<= 0`) | `PasswordEditorScreen`                                                                                               |

`home/PasswordGroupScreen.kt` reuses the existing vault list and Home ViewModel cache. Multi-account Home rows use `PasswordListItem.accountCount`; the CSV reader normalizes header variants while preserving credential fields and supports UTF-8/BOM-marked UTF-16.

`ui/components/PageScroll.kt` shares native stretch behavior for fitting content and standard scrolling for overflow. Group pages omit search; account access uses the activity-retained session, while export stays fresh-authenticated.

### Data model (`passwords` table)

| Column                     | Kotlin                 | Stored                                                          |
| -------------------------- | ---------------------- | --------------------------------------------------------------- |
| `id`                       | `Long` PK autoGenerate | plain                                                           |
| `site_name`                | `siteName: String`     | v2 ciphertext                                                   |
| `username`                 | `String = ""`          | v2 ciphertext                                                   |
| `password`                 | `String`               | v2 ciphertext: `kp2:` + Base64(IV[12] ‖ AES-GCM ciphertext+tag) |
| `url`                      | `String = ""`          | v2 ciphertext                                                   |
| `notes`                    | `String = ""`          | v2 ciphertext                                                   |
| `created_at`, `updated_at` | `Long` epoch ms        | plain                                                           |

## 4) Module Boundaries

| Boundary                | What belongs here                                      | What must not be here     |
| ----------------------- | ------------------------------------------------------ | ------------------------- |
| `ui/screens/<feature>/` | Composables and the feature ViewModel                  | DAO/Room/crypto calls     |
| `ui/components/`        | Reusable stateless composables                         | Feature logic, ViewModels |
| `data/repository/`      | Single access point for vault data. Transparent crypto | Android UI types          |
| `data/local/*`          | Room, JSON, and SharedPreferences details              | UI or navigation          |
| `utils/`                | Stateless helpers and constants                        | Business state            |

Current violations: `SettingsScreen` uses `PreferenceManager` directly (no ViewModel), and `MainActivity` reads `PreferenceManager` inside `setContent`.

## 5) Naming and Organization Rules

- Files: PascalCase, named after the main declaration (`PasswordDetailScreen.kt`, `HomeViewModel.kt`). Files of top-level functions are named for their topic (`UrlUtils.kt`, `SiteDomain.kt`).
- Directories: lowercase feature names under `ui/screens/` (feature-based), layer names under `data/` (layer-based).
- Packages mirror directories.
- No path aliases. Plain Kotlin package imports.

## 6) Evidence

- `settings.gradle.kts`, `build-logic/`, `.gitignore`, `git ls-files`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/nielcode/kupass/App.kt`, `MainActivity.kt`, `ui/screens/MainAppScreen.kt`
- `core/data/src/main/java/com/nielcode/kupass/data/local/db/PasswordEntity.kt`
