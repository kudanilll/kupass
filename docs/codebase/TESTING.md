# Testing Patterns

> Generated with the `acquire-codebase-knowledge` skill on 2026-09-18 (commit `8de433b`).

## 1) Test Stack and Commands

- Primary framework: JUnit 4.13.2 + Robolectric 4.17 (`@RunWith(RobolectricTestRunner::class)`).
- Assertions: `org.junit.Assert` (`assertEquals`, `assertTrue`). No mocking library.
- Instrumented: AndroidX Test JUnit 1.3.0, Espresso 3.7.0, `compose-ui-test-junit4` (dependencies exist, no real tests yet).

```bash
./gradlew :app:testDebugUnitTest            # all unit tests (verified: BUILD SUCCESSFUL, 9/9 pass, 2026-09-18)
./gradlew :app:testDebugUnitTest --tests "com.nielcode.kupass.security.CryptoManagerTest"
ANDROID_SERIAL=<device> ./gradlew :app:connectedDebugAndroidTest   # instrumented. It UNINSTALLS the app afterwards, so use a dedicated/read-only emulator, never a device with real vault data
./gradlew :app:testDebugUnitTest -Pkupass.snapshots --tests "*UiSnapshotTest"   # renders screens to app/build/ui-snapshots/*.png (own JVM, opt-in)
# coverage: [TODO] not configured (no JaCoCo/Kover)
```

Results land in `app/build/test-results/testDebugUnitTest/*.xml` and `app/build/reports/tests/`.

## 2) Test Layout

- Unit tests: `app/src/test/java/` mirroring the source package (`security/CryptoManagerTest.kt`, `data/backup/BackupCodecTest.kt`).
- Instrumented: `app/src/androidTest/java/.../ExampleInstrumentedTest.kt` (template only).
- Naming: `<ClassUnderTest>Test`, with backtick sentence method names and Arrange/Act/Assert comments.
- Setup files: none (no shared rules, fixtures, or `robolectric.properties`).

## 3) Test Scope Matrix

| Scope               | Covered? | Typical target                                                                | Notes                                                                                                                      |
| ------------------- | -------- | ----------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Unit: crypto | Yes (11) | v2 round-trip, fresh IV, empty/whitespace, legacy v1 + plaintext reads, tamper/wrong-key/malformed → `CryptoException`, fail-closed encrypt | Injected test key. Real Keystore covered by `androidTest/.../CryptoManagerKeystoreTest` (2) |
| Unit: backup format | Yes (13) | v2 round trip, cross-device restore, no plaintext leak, wrong/missing password, tampered header, future version, legacy same-device/plaintext/foreign (C-1 regression), malformed, too large | `BackupCodecTest`. Production KDF cost verified on device by `androidTest/.../BackupCodecDeviceTest` |
| Unit: ViewModels | Yes (6) | Editor create/update/encryption failure, Home list + search, `BackupViewModel` empty-export event, Detail delete | `ui/screens/ViewModelsTest.kt` with `testing/FakePasswordDao` + `MainDispatcherRule` (kotlinx-coroutines-test) |
| Unit: app lock | Yes (11) | Initial lock, timeout boundaries, immediate, rotation, exempt trips (grace, long-absence still locks, single use, expiring), locked departures | `security/AppLockTest.kt` (fake clock) |
| Unit: clipboard | Yes (5) | Sensitive flag + clear at 45 s (API 33), clear on API 27, non-sensitive kept, newer user clip kept, timer restart | `security/SecureClipboardTest.kt` (Robolectric `@Config(sdk)` + `ShadowLooper`) |
| Manual E2E | Yes | Android 17 emulator: no screen lock → guidance; PIN set → system prompt; wrong PIN stays locked; correct PIN opens vault; back after 5 s stays open; back after 32 s re-locks | Done with the `android-cli` skill (`android layout`); screenshots are black by design (`FLAG_SECURE`) |
| Unit: repository | Yes (7) | Full-field encryption at rest, case-insensitive sort, in-memory search on all fields, legacy upgrade + idempotence, undecryptable rows untouched , import dedupe/validation, atomic import on crypto failure | `data/repository/PasswordRepositoryTest.kt`. Device: `androidTest/.../PasswordRepositoryUpgradeTest` (real Keystore + Room) |
| Integration: Room | Yes (1) | Schema v1 opens with the current schema + all `MIGRATIONS`, rows preserved | `androidTest/.../KupassDatabaseMigrationTest.kt` via `MigrationTestHelper` (schemas from `app/schemas/`) |
| Unit: site icons | Yes (19) | Domain extraction (credentials/path stripped, one-word site name → `.com`, IPs/private names rejected), Favget client against a local `HttpServer` (key only in header, no non-HTTPS redirect, status mapping, size cap), repository (dedupe, memory cache, miss/failure retry, no-key fallback) | `data/siteicon/*Test.kt` |
| UI snapshots | Yes (9, opt-in) | Home (entries, site icons, empty, no results), Data, Settings, Editor, Detail, BottomNav rendered to PNG for review | `ui/UiSnapshotTest.kt`: Robolectric native graphics on SDK 35 (Espresso lacks SDK 37). Device screenshots are blank (`FLAG_SECURE`), so this is how UI is checked |
| UI / E2E            | No       | create/edit/delete/export/import flows                                        | Compose test deps present, unused                                                                                          |

## 4) Mocking and Isolation Strategy

- No mocks. The tests use real singletons.
- `CryptoManager` isolation: JVM tests inject a fixed AES key with `CryptoManager.setKeyProviderForTesting { key }` in `@Before`. The real Keystore path is covered by the instrumented `CryptoManagerKeystoreTest`.
- Shared state: the `CryptoManager` object and its `testKey` persist across tests in the same JVM. The tests don't reset them.
- Common failure mode: tests pass on the JVM while real-device Keystore behavior (key invalidation, `KeyPermanentlyInvalidatedException`, StrongBox) stays untested.

## 5) Coverage and Quality Signals

- Coverage tool/threshold: [TODO] none.
- Current coverage: [TODO] unmeasured. By inspection, only `CryptoManager` and `BackupCodec` are exercised.
- CI: `.github/workflows/ci.yml` runs `assembleDebug`, `testDebugUnitTest`, and `lintDebug` on every push/PR to `master`. Reports are uploaded as an artifact on failure.
- Gaps to prioritize: instrumented Keystore crypto test, Room DAO + migration tests, ViewModel tests (after introducing a factory/DI), and a regression test for C-1 (a backup from a different key must not import ciphertext as a password).

## 6) Evidence

- `app/build.gradle.kts` (test dependencies)
- `app/src/test/java/com/nielcode/kupass/security/CryptoManagerTest.kt`
- `app/src/test/java/com/nielcode/kupass/data/backup/BackupCodecTest.kt`
- `app/src/main/java/com/nielcode/kupass/security/CryptoManager.kt` (test key branch)
- `./gradlew :app:testDebugUnitTest` output on 2026-09-18: BUILD SUCCESSFUL. Suites: ExampleUnitTest 1, JsonExportImportTest 4, CryptoManagerTest 4. 0 failures
