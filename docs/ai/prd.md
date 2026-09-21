# Product Requirements (PRD)

> Living document. Items marked **(proposed)** are agent suggestions that Danil has not yet approved. Confirm them before building. Confirmed decisions are listed in §8.

## 1. Vision

Kupass is a **simple, trustworthy, offline password manager** for Android. Users should be able to hand it their most sensitive data without worrying about it, and it should stay small and fast on low-end devices.

## 2. Product Goals

| Goal                 | What it means in practice                                                                                                                                                                                                     |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Secure**           | All secrets encrypted at rest and in backups. No plaintext leaves the sandbox. Vault locked behind biometric/device-credential authentication. Open source (GPL-3.0) and auditable. **Zero telemetry.**                       |
| **Few bugs**         | No data loss (migrations, confirmations, atomic imports). Logic covered by unit tests, and CI green on every push. Crashes are found through tests, lint, and user-initiated bug reports, **never through in-app telemetry**. |
| **Good performance** | Cold start < 1 s on mid-range devices. Smooth 60/120 fps lists. No main-thread I/O or crypto on large lists.                                                                                                                  |
| **Lightweight**      | Release APK kept small (target < 8 MB **(proposed)**). Minimal dependencies. R8 + resource shrinking on. Bundled fonts optimized (subset/compressed) without changing the design font.                                        |

## 3. Target Users

- Everyday Android users (ID and EN locales) who want to stop reusing passwords and do not want a cloud account.
- Privacy-conscious users who prefer open-source, offline tools.

## 4. Current Features (v3.1.0)

- Vault list with search (site name, username, URL, notes), empty state, and swipe-to-delete with a confirmation dialog.
- Create/edit an entry: site name, username, password (show/hide), URL, and notes.
- Detail view with copy-to-clipboard per field. The password copy is flagged sensitive on API 33+ and auto-cleared after 45 s on older APIs.
- JSON export/import through the Storage Access Framework. The password field is encrypted with the device Keystore key.
- Settings: language (EN/ID), theme (system/light/dark), Material You dynamic color, OSS licenses, and developer/GitHub links.
- Screenshot blocking (`FLAG_SECURE`), and `allowBackup="false"`.

## 5. Requirements Backlog

Priority: **P0** blocks a trustworthy release, **P1** is important, **P2** is nice to have.

### Security

| ID    | Pri | Requirement                                                                                                                                                                                                                                                       |
| ----- | --- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SEC-1 | P0  | ✅ _Confirmed._ **Portable encrypted backup**: export encrypted with a user-chosen password (e.g. PBKDF2/Argon2 → AES-GCM) instead of the device-bound Keystore key, so a backup can be restored after reinstall or on a new phone. Keep a versioned file format. |
| SEC-2 | P0  | ✅ _Confirmed._ **App lock**: `BiometricPrompt` with `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` (fallback to the device PIN/pattern/password) on launch and after a background timeout. No separate master password.                                                 |
| SEC-3 | P0  | Encrypt **all** sensitive fields at rest (at minimum `notes`, `username`, `url`), or move to full-DB encryption **(proposed: evaluate SQLCipher vs field-level cost/size)**.                                                                                      |
| SEC-4 | P0  | Crypto must fail closed: no silent plaintext fallback on encrypt, and a decrypt error must be surfaced, not returned as data.                                                                                                                                     |
| SEC-5 | P1  | Exclude the database and prefs from `device-transfer` in `data_extraction_rules.xml`.                                                                                                                                                                             |
| SEC-6 | P1  | Clipboard auto-clear on all API levels (configurable timeout).                                                                                                                                                                                                    |
| SEC-7 | P2  | Password generator with a strength indicator.                                                                                                                                                                                                                     |

### Tooling & Quality

| ID   | Pri | Requirement                                                                                                                                                     |
| ---- | --- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| QA-1 | P1  | ✅ _Confirmed._ Enforced formatting and static analysis (ktfmt or ktlint + detekt) wired into Gradle.                                                           |
| QA-2 | P1  | ✅ _Confirmed._ GitHub Actions CI: build, unit tests, lint, and format check on push/PR (hardened: pinned actions, least-privilege token).                      |
| QA-3 | P1  | ✅ _Done 2026-09-19 (236 KB)._ Optimize `googlesansflex.ttf` (~3.9 MB): subset to the needed glyphs (Latin + Indonesian) and trim unused variation axes. Keep the font itself. |

### Reliability

| ID    | Pri | Requirement                                                                                                   |
| ----- | --- | ------------------------------------------------------------------------------------------------------------- |
| REL-1 | P0  | Room schema export enabled, with migrations and migration tests before any schema change.                     |
| REL-2 | P1  | Atomic import (single transaction), duplicate detection, and a user-visible result (imported/skipped counts). |
| REL-3 | P1  | Undo for deletes (snackbar) in addition to confirmation.                                                      |
| REL-4 | P1  | All user-facing strings localized (EN + ID).                                                                  |

### Features

| ID     | Pri | Requirement                                                                                                                                                                                                               |
| ------ | --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| FEAT-1 | P1  | **Android Autofill service** **(proposed)**. This is the core value of a password manager on Android.                                                                                                                     |
| FEAT-2 | P2  | ✅ _Done 2026-09-19; revised 2026-09-21._ Site favicons via the Favget API (keep `BuildConfig.FAVGET_API_URL`) are always enabled. Send only the bare public domain and cache icons in memory only. |
| FEAT-5 | P1  | ✅ _Confirmed._ **Feedback & bug report** in Settings. User-initiated only (e.g. a prefilled email or GitHub issue link, with app version/device info the user can see and edit before sending). Never attach vault data. |
| FEAT-3 | P2  | Categories/tags and favorites **(proposed)**.                                                                                                                                                                             |
| FEAT-4 | P2  | Adaptive layout for tablets/foldables (list-detail) **(proposed)**.                                                                                                                                                       |

## 6. Non-Goals (for now)

- **Any telemetry**: analytics, crash-reporting SDKs, or automatic usage collection.
- Cloud sync or accounts.
- Sharing passwords between users.
- Non-Android platforms.

## 7. Definition Of Done

A change is done when it builds, passes unit tests and lint, has tests for new logic, has strings in both locales, leaves no secret in logs or plaintext storage, and has `docs/ai/` updated if behavior changed.

## 8. Decision Log

| Date       | Decision                                                                          | By    |
| ---------- | --------------------------------------------------------------------------------- | ----- |
| 2026-09-18 | Backups must be portable, protected by a user-chosen backup password (SEC-1).     | Danil |
| 2026-09-18 | App lock = biometric with device-credential fallback. No master password (SEC-2). | Danil |
| 2026-09-18 | Keep the Favget favicon config/plan, not used yet (FEAT-2).                       | Danil |
| 2026-09-18 | Keep the Google Sans Flex font. Optimize its size (QA-3).                         | Danil |
| 2026-09-19 | Favget: embed the API key in BuildConfig (extractable; server rate limiting). Site icons opt-in, off by default. | Danil |
| 2026-09-18 | Add enforced formatting/lint and CI (QA-1, QA-2).                                 | Danil |
| 2026-09-18 | 100% telemetry-free. Manual feedback/bug report feature instead (FEAT-5).         | Danil |
| 2026-09-18 | License is GPL-3.0. The AGPLv3 mention in commit `d1f7eff` is incorrect.          | Danil |
| 2026-09-18 | Search after full-field encryption decrypts in memory. No SQLCipher (SEC-3). | Danil |
| 2026-09-18 | Backup key derivation uses PBKDF2-HMAC-SHA256 (SEC-1). | Danil |
| 2026-09-18 | App lock is UI-level only. The vault key isn't bound to user auth yet (SEC-2). | Danil |
| 2026-09-18 | The Autofill service is an important feature (FEAT-1). | Danil |
| 2026-09-18 | The app version stays `3.1.0`. Agents never bump `versionName`/`versionCode`. | Danil |
| 2026-09-21 | Site icons are always enabled and no longer have a Settings toggle. This supersedes the 2026-09-19 opt-in decision. | Danil |
