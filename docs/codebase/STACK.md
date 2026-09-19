# Technology Stack

> Generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`). Every claim is backed by the evidence list at the bottom.

## 1) Runtime Summary

| Area                  | Value                                                                                                             | Evidence                                                                         |
| --------------------- | ----------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| Primary language      | Kotlin 2.4.20 (33 `.kt` files, ~6.4k LOC total repo)                                                              | `gradle/libs.versions.toml` (`kotlin`), scan CODE METRICS                        |
| Runtime               | Android. minSdk 27 (8.1), compileSdk/targetSdk 37                                                                 | `app/build.gradle.kts`                                                           |
| JVM                   | Gradle daemon toolchain JDK 21 (foojay auto-provisioned). Bytecode target Java 11                                 | `gradle/gradle-daemon-jvm.properties`, `app/build.gradle.kts` (`compileOptions`) |
| Build system          | Gradle 9.7.1 (Kotlin DSL), AGP 9.4.0 (max API 37), single module `:app`                                           | `gradle/wrapper/gradle-wrapper.properties`, `settings.gradle.kts`                |
| Dependency management | Gradle version catalog `gradle/libs.versions.toml`, repos `google()` + `mavenCentral()` (`FAIL_ON_PROJECT_REPOS`) | `settings.gradle.kts`                                                            |
| App identity          | `com.nielcode.kupass`, versionName `3.1.0`, versionCode `5`                                                       | `app/build.gradle.kts`                                                           |
| License               | GPL-3.0                                                                                                           | `LICENSE`, `README.md`                                                           |

## 2) Production Frameworks and Dependencies

| Dependency                                                                                      | Version                      | Role in system                                      | Evidence                              |
| ----------------------------------------------------------------------------------------------- | ---------------------------- | --------------------------------------------------- | ------------------------------------- |
| Compose BOM (`ui`, `ui-graphics`, `material3`, `material-icons-extended`, `ui-tooling-preview`) | 2026.09.00 (material3 1.4.0) | Entire UI layer                                     | `libs.versions.toml`                  |
| `activity-compose`                                                                              | 1.13.0                       | `setContent`, `enableEdgeToEdge`, SAF launchers     | `MainActivity.kt`, `MainAppScreen.kt` |
| `navigation-compose`                                                                            | 2.10.1                       | Typed `@Serializable` routes                        | `MainAppScreen.kt`                    |
| `lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose`                                          | 2.11.0                       | `ViewModel` + `viewModelFactory`, `viewModelScope`, `viewModel(factory = …)` | `ui/screens/*/*ViewModel.kt`          |
| Room (`runtime`, `ktx`, `compiler` via KSP 2.3.12)                                              | 2.8.5                        | Local DB `kupass_database`                          | `data/local/db/*`                     |
| `kotlinx-serialization-json` (+ Kotlin serialization plugin 2.4.20)                             | 1.11.0                       | Backup JSON format and nav routes                   | `BackupCodec.kt`                 |
| `core-ktx`                                                                                      | 1.19.0                       | `toUri()` and other extensions                      | `Util.kt`                             |
| `core-splashscreen`                                                                             | 1.2.0                        | `installSplashScreen()`                             | `MainActivity.kt`                     |
| `play-services-oss-licenses` (+ `oss-licenses` plugin 0.13.0)                                   | 17.5.2                       | `OssLicensesMenuActivity`                           | `SettingsScreen.kt`                   |
| **AppCompat** (`AppCompatActivity`, `AppCompatDelegate`)                                        | 1.8.0 (declared)             | Per-app locale, night mode                          | `MainActivity.kt`, `App.kt`           |
| **Material Components** (`com.google.android.material.color.DynamicColors`)                     | 1.14.0 (declared)            | Dynamic color check/apply                           | `App.kt`, `SettingsScreen.kt`         |
| Android Keystore / `javax.crypto` (platform)                                                    | platform                     | AES-GCM encryption of every vault field (`kp2:` format)        | `utils/CryptoManager.kt`              |
| `androidx.biometric` | 1.1.0 | `BiometricPrompt` for the UI-level app lock | `MainActivity.kt` |
| `lifecycle-runtime-compose` | 2.11.0 | `collectAsStateWithLifecycle` | `ui/screens/**` |

Bundled assets that affect APK size: `res/font/googlesansflex.ttf` (**~3.9 MB**, variable font) and `res/font/heming.ttf` (~30 KB), referenced in `ui/theme/Type.kt`.

## 3) Development Toolchain

| Tool                                                                           | Purpose                                                                                                                                | Evidence                       |
| ------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------ |
| JUnit 4.13.2                                                                   | Unit tests                                                                                                                             | `libs.versions.toml`           |
| kotlinx-coroutines-test 1.11.0 | ViewModel/repository tests (`runTest`, `MainDispatcherRule`) | `libs.versions.toml` |
| `room-testing` 2.8.5 + `androidx.room` Gradle plugin | `MigrationTestHelper`, schema export to `app/schemas/` | `app/build.gradle.kts` |
| Robolectric 4.17 (supports SDK 37)                                             | Android framework on the JVM for unit tests                                                                                            | `app/build.gradle.kts`         |
| AndroidX Test (`junit` 1.3.0, `espresso-core` 3.7.0, `compose-ui-test-junit4`) | Instrumented tests                                                                                                                     | `libs.versions.toml`           |
| R8 (`isMinifyEnabled`, `isShrinkResources` in release)                         | Shrinking/obfuscation                                                                                                                  | `app/build.gradle.kts`         |
| Android Lint                                                                   | Static checks (default config, no `lint.xml`)                                                                                          | [TODO] no lint config found    |
| google-java-format IDE plugin (enabled in `.idea`, which is gitignored)        | Formatting in the IDE only                                                                                                             | `.idea/google-java-format.xml` |
| `kotlin.code.style=official`                                                   | Kotlin style hint for the IDE                                                                                                          | `gradle.properties`            |
| CI/CD                                                                          | Release workflow only (`.github/workflows/release.yml`, tag `v*`). Dependabot (`.github/dependabot.yml`). `ci.yml`: build + unit tests + lint on push/PR to `master`, and dependency-graph submission on `master` | `.github/`                     |

## 4) Key Commands

```bash
./gradlew :app:assembleDebug              # build debug APK
./gradlew :app:assembleRelease            # needs release-key.jks + secrets.properties (falls back to debug signing)
./gradlew :app:testDebugUnitTest          # JVM/Robolectric unit tests
./gradlew :app:lintDebug                  # Android lint
./gradlew :app:connectedDebugAndroidTest  # instrumented tests (device/emulator)
```

## 5) Environment and Config

- Config sources: `local.properties` (SDK path, gitignored), `secrets.properties` (gitignored; keys `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`), `release-key.jks` (gitignored).
- `BuildConfig` fields (same values in debug and release): `GIT_URL`, `DEV_URL`, `DEV_NAME`, `FAVGET_API_URL`. None are secrets.
- No environment variables are read. There is no `.env` template.
- Runtime constraints: offline-first. `INTERNET` permission is declared but no network client exists yet (see `INTEGRATIONS.md`).

## 6) Evidence

- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
- `gradle/wrapper/gradle-wrapper.properties`, `gradle/gradle-daemon-jvm.properties`
- `app/src/main/java/com/nielcode/kupass/App.kt`, `MainActivity.kt`, `ui/theme/Type.kt`
- `docs/codebase/.codebase-scan.txt` (CODE METRICS, CI/CD sections)
