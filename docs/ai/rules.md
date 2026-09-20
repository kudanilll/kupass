# Agent Rules

Working rules for AI agents contributing to Kupass. They apply to every task.

## 1. Use The Available Resources

Before relying on memory, check what the session provides and use it.

| Need                                                                      | Use                                                                                                                                                                                            |
| ------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Library/API docs (Compose, Room, Navigation, AGP, Kotlin, Keystore, etc.) | **context7 MCP** (`resolve-library-id`, then `query-docs`). Training data may be outdated, and this project is on very recent versions (AGP 9, Kotlin 2.4, Compose BOM 2026.06).               |
| Understanding or locating code                                            | Read `docs/codebase/` first. Use **codegraph** when `.codegraph/` exists at the repo root, and the `codebase-memory-mcp` skill when its MCP server is connected. Otherwise use Grep/Glob/Read. |
| Re-mapping / re-documenting the codebase                                  | `acquire-codebase-knowledge` skill (writes `docs/codebase/*`). `doc-and-modernize` for a single deep architecture doc or a migration plan.                                                     |
| Planning features/epics                                                   | `breakdown-feature-prd`, `breakdown-feature-implementation`, `breakdown-plan`, `breakdown-test` skills                                                                                         |
| CI / GitHub                                                               | `github-actions-hardening`, `github-actions-efficiency`, `github-issues`, `github-release` skills                                                                                              |
| Compose UI work                                                           | `android-jetpack-compose` skill, `edge-to-edge` skill, `adaptive` skill (tablets/foldables)                                                                                                    |
| Build tooling / Gradle / AGP                                              | `agp-9-upgrade`, `kotlin-tooling-agp9-migration`, `r8-analyzer` (shrinking/keep rules)                                                                                                         |
| Security                                                                  | `android-intent-security` skill, `/security-review`                                                                                                                                            |
| Performance                                                               | `android-profiler` skill (traces, startup, jank, memory)                                                                                                                                       |
| Navigation changes                                                        | `navigation-3` skill (if migrating from Navigation Compose)                                                                                                                                    |
| Running the app / devices                                                 | `android-cli` skill, `run` skill                                                                                                                                                               |
| Tests                                                                     | `testing-setup` skill                                                                                                                                                                          |
| Reviewing a diff                                                          | `/code-review`, `/simplify`                                                                                                                                                                    |

If a skill or MCP server fits the task, use it and name it in the end-of-turn report. If none fits, say so in the report.

## 2. Workflow

1. **Understand first.** Read `CLAUDE.md`, the relevant file in `docs/ai/`, and the code you are about to change. Treat the docs as a snapshot and trust the code when they disagree.
2. **Plan non-trivial work.** Anything touching crypto, the Room schema, import/export, or more than about 3 files gets a short plan first. Confirm product-level choices with Danil.
3. **Keep changes small and focused.** Don't refactor unrelated code in a feature or bugfix change.
4. **Verify.** For code changes run `./gradlew spotlessApply`, then `spotlessCheck :app:detektMain :app:detektTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. All must pass: detekt 0 issues (fix findings; a `@Suppress` needs a written reason next to it, and there is no baseline), lint 0 errors. Add or update unit tests for logic changes (repository, crypto, import/export, ViewModels).
5. **Report honestly.** If a build or test fails or a step was skipped, say so, with the output.
6. **Update docs.** If behavior, structure, or known issues change, update `docs/codebase/CONCERNS.md` (and whichever other `docs/codebase/*` or `docs/ai/*` file is affected) in the same change.

## 3. Hard Rules

- Never log, toast, or print secrets (passwords, notes, decrypted values, keys). No `printStackTrace()` in crypto paths.
- Never write vault data in plaintext outside the app sandbox: exports, the clipboard, share intents, and logs all count.
- Never ship a Room schema change without a `Migration` and a migration test. Do not use `fallbackToDestructiveMigration()`.
- Never add a network call that sends vault data (site names, URLs, usernames) off-device without explicit approval from Danil.
- **Never add telemetry**: no analytics, crash-reporting SDKs (Firebase Crashlytics, Sentry, etc.), or automatic usage collection. Feedback and bug reports are user-initiated only.
- Site icons (Favget, `data/siteicon/`) must stay opt-in and off by default, send only the bare domain, keep the API key off every host except Favget, and cache in memory only.
- Never add a dependency without checking its size, maintenance status, and license (GPL-3.0 compatible). Prefer AndroidX and the Kotlin stdlib.
- User-facing text goes in `res/values/strings.xml` **and** `res/values-in/strings.xml`. No hardcoded UI strings.
- Do not touch `secrets.properties`, `release-key.jks`, or signing config values.

## 4. Git

- Main/default branch: `master` (renamed from `compose` on 2026-09-18). **Never code directly on `master`.**
- Every change, however small, starts on a new branch from an up-to-date `master`: `git switch master && git pull && git switch -c <type>/<topic>`.
- Branch names use the commit-type prefixes: `feat/`, `fix/`, `refactor/`, `chore/`, `docs/`, `test/`, `ci/`, `build/` (e.g. `feat/app-lock`, `fix/backup-portability`).
- Changes reach `master` through a pull request.
- Commit or push only when asked.
- Commit with `git commit -s` (sign-off; commits are GPG-signed, so Danil enters the passphrase). Never add `Co-Authored-By` or other AI attribution to commits or PR descriptions.
- Conventional-style messages, as already used in history: `feat(crypto): ...`, `fix: ...`, `refactor: ...`, `style: ...`, `docs: ...`, `test: ...`.
- Never skip hooks or force-push without explicit instruction.
- Add user-visible changes under `## [Unreleased]` in `CHANGELOG.md` (Keep a Changelog).
- **Never change `versionName`/`versionCode`** (currently `3.1.0` / `5`). Only Danil decides versions. Releasing (Danil only): move `[Unreleased]` to `## [X.Y.Z] - date`, then push tag `vX.Y.Z` matching `versionName`. `.github/workflows/release.yml` builds, verifies, and publishes the signed APK. See the `github-release` skill.

## 5. Communication

- Danil usually writes in Indonesian. Reply in the same language as the message.
- Code, identifiers, comments, and docs in the repo stay in English.
- At the end of every message that completes a task, append a brief report of the **skills, MCP servers, and plugins** used during the turn.
