# Testing Patterns

> Generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`).

## 1) Test Stack and Commands

- Primary framework: JUnit 4.13.2 + Robolectric 4.17 (`@RunWith(RobolectricTestRunner::class)`).
- Assertions: `org.junit.Assert` (`assertEquals`, `assertTrue`). No mocking library.
- Instrumented: AndroidX Test JUnit 1.3.0, Espresso 3.7.0, `compose-ui-test-junit4` (dependencies exist, no real tests yet).

```bash
./gradlew :app:testDebugUnitTest            # all unit tests (verified: BUILD SUCCESSFUL, 9/9 pass, 2026-09-18)
./gradlew :app:testDebugUnitTest --tests "com.nielcode.kupass.utils.CryptoManagerTest"
./gradlew :app:connectedDebugAndroidTest    # instrumented (device/emulator)
# coverage: [TODO] not configured (no JaCoCo/Kover)
```

Results land in `app/build/test-results/testDebugUnitTest/*.xml` and `app/build/reports/tests/`.

## 2) Test Layout

- Unit tests: `app/src/test/java/` mirroring the source package (`utils/CryptoManagerTest.kt`, `data/local/json/JsonExportImportTest.kt`).
- Instrumented: `app/src/androidTest/java/.../ExampleInstrumentedTest.kt` (template only).
- Naming: `<ClassUnderTest>Test`, with backtick sentence method names and Arrange/Act/Assert comments.
- Setup files: none (no shared rules, fixtures, or `robolectric.properties`).

## 3) Test Scope Matrix

| Scope               | Covered? | Typical target                                                                | Notes                                                                                                                      |
| ------------------- | -------- | ----------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Unit: crypto        | Yes (4)  | `CryptoManager` round-trip, blank handling, fallback on invalid input         | Runs on the **in-memory test key** because AndroidKeyStore is absent in Robolectric, so the real Keystore path is untested |
| Unit: backup format | Yes (4)  | `JsonExportImport` export shape, encryption, import mapping, legacy plaintext | One test asserts the fail-open fallback (see CONCERNS C-1)                                                                 |
| Unit: ViewModels    | No       | `HomeViewModel`, `PasswordEditorViewModel`, `PasswordDetailViewModel`         | Blocked by the lack of DI (ViewModels construct Room themselves)                                                           |
| Unit: repository    | No       | `PasswordRepository` encrypt/decrypt wiring                                   | Needs a fake DAO or in-memory Room                                                                                         |
| Integration: Room   | No       | DAO queries, search, migrations                                               | No `MigrationTestHelper`, and schema export is off                                                                         |
| UI / E2E            | No       | create/edit/delete/export/import flows                                        | Compose test deps present, unused                                                                                          |
| Placeholder         | Yes (1)  | `ExampleUnitTest.addition_isCorrect`                                          | Template noise                                                                                                             |

## 4) Mocking and Isolation Strategy

- No mocks. The tests use real singletons.
- `CryptoManager` isolation comes from a production-code branch: when `KeyStore.getInstance("AndroidKeyStore")` throws, it generates a process-local AES key (`testKey`). This test hook lives in production code (`CryptoManager.kt`, comment "ponytail: degrade gracefully...").
- Shared state: the `CryptoManager` object and its `testKey` persist across tests in the same JVM. The tests don't reset them.
- Common failure mode: tests pass on the JVM while real-device Keystore behavior (key invalidation, `KeyPermanentlyInvalidatedException`, StrongBox) stays untested.

## 5) Coverage and Quality Signals

- Coverage tool/threshold: [TODO] none.
- Current coverage: [TODO] unmeasured. By inspection, only `CryptoManager` and `JsonExportImport` are exercised.
- CI: none, so tests only run locally.
- Gaps to prioritize: instrumented Keystore crypto test, Room DAO + migration tests, ViewModel tests (after introducing a factory/DI), and a regression test for C-1 (a backup from a different key must not import ciphertext as a password).

## 6) Evidence

- `app/build.gradle.kts` (test dependencies)
- `app/src/test/java/com/nielcode/kupass/utils/CryptoManagerTest.kt`
- `app/src/test/java/com/nielcode/kupass/data/local/json/JsonExportImportTest.kt`
- `app/src/main/java/com/nielcode/kupass/utils/CryptoManager.kt` (test key branch)
- `./gradlew :app:testDebugUnitTest` output on 2026-09-18: BUILD SUCCESSFUL. Suites: ExampleUnitTest 1, JsonExportImportTest 4, CryptoManagerTest 4. 0 failures
