# CLAUDE.md

Overview of **Kupass** for AI/code agents. Keep this file short: it is loaded into every session. Details live in `docs/ai/` and are read on demand.

## What Kupass Is

Kupass is an open-source (GPL-3.0) offline password manager for Android by **nielcode**, written in Kotlin with Jetpack Compose. Everything is stored locally; there is no account or sync server.

**North star:** a password manager that is secure, has few bugs, performs well, and stays lightweight. Every change is judged against those four goals, with security first.

- Package: `com.nielcode.kupass`
- Branch: `master` (main/default). Never code directly on `master`: every feature or fix gets its own branch
- Version: `3.1.0` (versionCode `5`), minSdk 27, compile/target SDK 37
- Stack: Kotlin 2.4.20, AGP 9.4, Gradle 9.7.1, Compose BOM 2026.09, Material 3, Navigation Compose (typed routes), Lifecycle ViewModel, Room 2.8 + KSP, Kotlinx Serialization, Android Keystore (AES-GCM)
- People: Danil (founder, lead software engineer at nielcode) owns product decisions.

## Architecture In One Glance

```plaintext
MainActivity -> MainAppScreen (NavHost)
  ├─ HomeBase: HorizontalPager [Home | Data | Settings] + BottomNav
  ├─ PasswordDetail(passwordId)
  └─ PasswordEditor(passwordId = -1 for create)

Screen (Compose) -> AndroidViewModel -> PasswordRepository -> PasswordDao (Room)
                                              └─ CryptoManager (encrypts `password` field)
```

No DI framework. ViewModels build their own repository from `KupassDatabase.getInstance()`.

## Where To Read More

Read the relevant file **before** you work in that area:

**How we work** (`docs/ai/`, intent and rules):

| File                                                     | Read it when                                                                               |
| -------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| [`docs/ai/rules.md`](docs/ai/rules.md)                   | Always, at the start of a task. Covers workflow, tool/skill/MCP usage, git, and reporting. |
| [`docs/ai/prd.md`](docs/ai/prd.md)                       | You are building a feature or making a product decision.                                   |
| [`docs/ai/best-practices.md`](docs/ai/best-practices.md) | You are writing or reviewing Kotlin/Compose/Room/security code.                            |

**What the code is** (`docs/codebase/`, evidence-based map generated with the `acquire-codebase-knowledge` skill):

| File                                               | Read it when                                                                                    |
| -------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| [`STACK.md`](docs/codebase/STACK.md)               | Versions, dependencies, build commands, config/secrets files.                                   |
| [`STRUCTURE.md`](docs/codebase/STRUCTURE.md)       | Finding code, entry points, routes, data model, where new files go.                             |
| [`ARCHITECTURE.md`](docs/codebase/ARCHITECTURE.md) | Layers, data flow (save/export/import), patterns, architectural risks.                          |
| [`CONVENTIONS.md`](docs/codebase/CONVENTIONS.md)   | Naming, error handling, logging, localization as actually practiced.                            |
| [`INTEGRATIONS.md`](docs/codebase/INTEGRATIONS.md) | Keystore, SAF, clipboard, data stores, the planned Favget API.                                  |
| [`TESTING.md`](docs/codebase/TESTING.md)           | Test stack, layout, what is and isn't covered.                                                  |
| [`CONCERNS.md`](docs/codebase/CONCERNS.md)         | **Known bugs, security risks, tech debt, open `[ASK USER]` questions.** Single source of truth. |

Regenerate the scan with the skill's `scan.py` (output: `docs/codebase/.codebase-scan.txt`) when the structure changes significantly.

## Non-Negotiables

1. **Secrets never leave the device in plaintext** and are never logged. That covers vault data, backups, the clipboard, and logs.
2. **Never lose user data.** Room schema changes need a migration, and destructive actions need confirmation.
3. **Verify, don't assume.** Check library APIs with context7 or the docs, and check claims against the code. These docs can go stale, so fix them when they do.
4. **Use the available tooling:** skills, MCP servers, and plugins (details in `rules.md`).
5. **Zero telemetry.** No analytics or crash-reporting SDKs. Feedback and bug reports are user-initiated only.
6. License is **GPL-3.0**. New dependencies must be compatible with it.
7. Do not commit `secrets.properties`, `*.jks`, or `local.properties`.

Confirmed product decisions (app lock, portable backups, font, CI, etc.) are in the `docs/ai/prd.md` §8 Decision Log.

## Build & Test

```bash
./gradlew :app:assembleDebug        # build debug APK
./gradlew :app:testDebugUnitTest    # JVM + Robolectric unit tests
./gradlew :app:lintDebug            # Android lint
./gradlew :app:connectedDebugAndroidTest   # instrumented tests (device/emulator required)
```

## Agent Protocol

- **Reporting requirement:** at the end of every message that completes a task, append a brief report of which skills, MCP servers, and plugins were actively used during the turn.
- When code changes invalidate anything in this file or in `docs/ai/`, update the docs in the same change.
