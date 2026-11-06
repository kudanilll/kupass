# Contributing to Kupass

Thanks for helping. Kupass holds people's passwords, so every change is judged on security first, then on bugs, performance, and app size. This guide covers what a pull request needs before it can be merged.

## Ground rules

- Vault data never leaves the device in plaintext. That includes logs, toasts, the clipboard, exports, and share intents.
- No telemetry: no analytics, no crash-reporting SDKs, and no automatic usage collection. Bug reports come from users who choose to file them.
- Don't add a network call that sends vault data (site names, URLs, usernames) off the device. Open an issue first if you think one is needed.
- Never lose user data. A Room schema change needs a `Migration` and a migration test, and `fallbackToDestructiveMigration()` is not allowed. Destructive actions ask for confirmation.
- New dependencies must be compatible with GPL-3.0, actively maintained, and small. Prefer AndroidX and the Kotlin standard library.
- Don't change `versionName` or `versionCode`. The maintainer sets versions when releasing.

Cloud sync, accounts, sharing passwords between users, and platforms other than Android are out of scope for now.

## Before you start

For anything bigger than a small fix, open an issue first so we can agree on the approach before you write code. Security problems go through the private process in [SECURITY.md](SECURITY.md), never through public issues.

[`docs/codebase/`](docs/codebase) describes the architecture, conventions, and tests. Open problems are listed in [`docs/codebase/CONCERNS.md`](docs/codebase/CONCERNS.md).

## Setup

Follow [Build from source](README.md#build-from-source) in the README. You need JDK 21 and the Android SDK with platform 37. A Favget API key is optional.

Never commit `secrets.properties`, `local.properties`, or `*.jks` files. They are already in `.gitignore`.

## Making a change

1. Branch from an up-to-date `master`. Name the branch after the kind of change: `feat/`, `fix/`, `refactor/`, `docs/`, `test/`, `build/`, `ci/`, or `chore/`, for example `fix/clipboard-autoclear`.
2. Keep each pull request to one change. Unrelated refactoring belongs in its own pull request.
3. Put user-facing text in `app/src/main/res/values/strings.xml` and add the Indonesian translation to `app/src/main/res/values-in/strings.xml`. Don't hardcode UI strings.
4. Add or update unit tests when you change logic, especially the repository, crypto, import/export, and ViewModels.
5. List user-visible changes under `## [Unreleased]` in [CHANGELOG.md](CHANGELOG.md), following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## Checks

Format the code, then run the same checks as CI:

```bash
./gradlew spotlessApply
./gradlew spotlessCheck detektMain detektTest testDebugUnitTest lintDebug assembleDebug
```

Everything must pass. Detekt has no baseline and must report 0 issues; if you suppress a rule, write the reason next to the `@Suppress`. Lint must report 0 errors.

If you touch the database or the Keystore code, also run the instrumented tests on an emulator or a device:

```bash
./gradlew :core:data:connectedDebugAndroidTest :core:security:connectedDebugAndroidTest
```

## Commits

Write commit messages in the conventional style used in the history, for example `feat(backup): add import progress tracking and display` or `fix(build): patch Dependabot-alerted build tooling dependencies`. Sign off every commit with `git commit -s`.

## Pull requests and review

`master` is protected, so every change goes in through a pull request. A pull request can be merged once the CI job passes, the maintainer has approved it, and every review thread is resolved. Pushing new commits dismisses an earlier approval, so the approved code is the code that gets merged.

The pull request template has a checklist that matches this guide. Fill it in, and add a short note on how you tested the change.

## License

By contributing, you agree that your contributions are licensed under the [GNU General Public License v3.0](LICENSE).
