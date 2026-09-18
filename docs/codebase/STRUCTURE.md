# Codebase Structure

> Generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`).

## 1) Top-Level Map

| Path                                                        | Purpose                                                                                                                                      | Evidence                                  |
| ----------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------- |
| `app/`                                                      | The only Gradle module (Android application)                                                                                                 | `settings.gradle.kts` (`include(":app")`) |
| `app/src/main/java/com/nielcode/kupass/`                    | All production Kotlin source                                                                                                                 | `git ls-files`                            |
| `app/src/main/res/`                                         | Resources: `values/` (EN), `values-in/` (ID), `values-night/`, `font/`, `xml/` (backup rules), mipmaps, drawables                            | directory listing                         |
| `app/src/test/`                                             | JVM + Robolectric unit tests                                                                                                                 | `git ls-files`                            |
| `app/src/androidTest/`                                      | Instrumented tests (placeholder only)                                                                                                        | `ExampleInstrumentedTest.kt`              |
| `gradle/`                                                   | Version catalog, wrapper, daemon JVM toolchain                                                                                               | `libs.versions.toml`                      |
| `docs/ai/`                                                  | Agent rules, PRD, best practices                                                                                                             | this repo                                 |
| `docs/codebase/`                                            | Evidence-based codebase map (these docs)                                                                                                     | this repo                                 |
| `image/`                                                    | README banner and screenshots                                                                                                                | `README.md`                               |
| `.github/`                                                  | `dependabot.yml` (Gradle + Actions updates, weekly, target `compose`), `workflows/release.yml` (signed APK + GitHub Release on tag `vX.Y.Z`) | this repo                                 |
| `CHANGELOG.md`                                              | Keep a Changelog. The release workflow publishes the `## [X.Y.Z]` section as release notes                                                   | this repo                                 |
| `CLAUDE.md`                                                 | Short agent overview. Links here                                                                                                             | repo root                                 |
| `secrets.properties`, `release-key.jks`, `local.properties` | Local-only signing/SDK config. Gitignored                                                                                                    | `.gitignore`                              |
| `app/release/`, `build/`, `.gradle/`, `.idea/`, `.kotlin/`  | Build output and IDE state. Gitignored, don't document or edit                                                                               | `.gitignore`                              |

## 2) Entry Points

- Process entry: `App.kt` (`android:name=".App"`). It blocks screenshots on every Activity (`FLAG_SECURE`) and applies the saved locale, night mode, and dynamic colors.
- UI entry: `MainActivity.kt`, the only Activity, exported with the LAUNCHER intent filter. It runs `installSplashScreen()`, `enableEdgeToEdge()`, then `KupassTheme { MainAppScreen() }`.
- Navigation root: `ui/screens/MainAppScreen.kt` (`NavHost`, start destination `HomeBase`).
- Secondary entry points: none (no services, receivers, providers, or workers in `AndroidManifest.xml`).

## 3) Source Map

```plaintext
com/nielcode/kupass/
├── App.kt                         Application: FLAG_SECURE, locale, theme, dynamic colors
├── MainActivity.kt                AppCompatActivity host for Compose
├── data/
│   ├── local/db/
│   │   ├── KupassDatabase.kt      Room singleton "kupass_database", version 1, exportSchema=false
│   │   ├── PasswordDao.kt         Flow getAll/search/getById, suspend insert(REPLACE)/update/delete
│   │   └── PasswordEntity.kt      table "passwords"
│   ├── local/json/JsonExportImport.kt   backup (de)serialization, password field encrypted
│   ├── local/prefs/PreferenceManager.kt SharedPreferences "kupass_preferences"
│   └── repository/PasswordRepository.kt encrypt on write, decrypt on read (password field only)
├── ui/
│   ├── components/                BottomNav (194 lines), SectionHeader, SectionItem
│   ├── screens/
│   │   ├── MainAppScreen.kt       routes, pager (Home/Data/Settings), SAF export/import launchers, toasts
│   │   ├── home/                  HomeScreen, HomeViewModel (list, search, delete, export/import), components/{VaultList, PasswordListItem}
│   │   ├── data/DataScreen.kt     export/import buttons only
│   │   ├── detail/                PasswordDetailScreen (+ copyToClipboard), PasswordDetailViewModel
│   │   ├── editor/                PasswordEditorScreen, PasswordEditorViewModel, components/TextField.kt
│   │   └── settings/SettingsScreen.kt   language/theme/dynamic color dialogs, about links, SingleChoiceDialog
│   └── theme/                     Color.kt, Theme.kt (KupassTheme), Type.kt (Heming display, Google Sans Flex body)
└── utils/
    ├── AppConfig.kt               int codes for language/theme/dynamic color
    ├── CryptoManager.kt           Keystore AES/GCM, alias "kupass_vault_key"
    └── Util.kt                    openUrl()
```

### Navigation routes (`MainAppScreen.kt`)

| Route            | Args                                          | Screen                                                                |
| ---------------- | --------------------------------------------- | --------------------------------------------------------------------- |
| `HomeBase`       | none                                          | `MainPagerScreen`: `HorizontalPager` pages 0 Home, 1 Data, 2 Settings |
| `PasswordDetail` | `passwordId: Long`                            | `PasswordDetailScreen`                                                |
| `PasswordEditor` | `passwordId: Long = -1L` (create when `<= 0`) | `PasswordEditorScreen`                                                |

### Data model (`passwords` table)

| Column                     | Kotlin                 | Stored                                  |
| -------------------------- | ---------------------- | --------------------------------------- |
| `id`                       | `Long` PK autoGenerate | plain                                   |
| `site_name`                | `siteName: String`     | plaintext                               |
| `username`                 | `String = ""`          | plaintext                               |
| `password`                 | `String`               | Base64(IV[12] ‖ AES-GCM ciphertext+tag) |
| `url`                      | `String = ""`          | plaintext                               |
| `notes`                    | `String = ""`          | plaintext                               |
| `created_at`, `updated_at` | `Long` epoch ms        | plain                                   |

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

- Files: PascalCase, named after the main declaration (`PasswordDetailScreen.kt`, `HomeViewModel.kt`). The exception is `Util.kt`, which holds top-level functions.
- Directories: lowercase feature names under `ui/screens/` (feature-based), layer names under `data/` (layer-based).
- Packages mirror directories, with one exception: `ui/screens/editor/components/TextField.kt` declares `package com.nielcode.kupass.ui.components`.
- No path aliases. Plain Kotlin package imports.

## 6) Evidence

- `settings.gradle.kts`, `.gitignore`, `git ls-files`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/nielcode/kupass/App.kt`, `MainActivity.kt`, `ui/screens/MainAppScreen.kt`
- `app/src/main/java/com/nielcode/kupass/data/local/db/PasswordEntity.kt`
- `app/src/main/java/com/nielcode/kupass/ui/screens/editor/components/TextField.kt` (package mismatch)
