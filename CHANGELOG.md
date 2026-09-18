# Changelog

All notable changes to Kupass are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security

- Vault data is excluded from Android cloud backup and device-to-device transfer on every Android version. Use the password-protected export to move your vault.
- Site names, usernames, URLs, and notes are now encrypted on the device like passwords. Existing vaults are upgraded automatically on first launch.

### Added

- App lock: the vault opens only after fingerprint/face or the device PIN/pattern/password, and locks again after leaving the app (Settings → Security → Auto-lock: immediately, 30 seconds, 1 minute, or 5 minutes). Devices without a screen lock are asked to set one up.
- Portable encrypted backups: export asks for a backup password and encrypts the whole vault (PBKDF2-HMAC-SHA256 + AES-256-GCM), so a backup can be restored on a new phone or after reinstalling.

### Changed

- Updated the build toolchain to the latest stable releases: Android Gradle Plugin 9.4.0, Gradle 9.7.1, Kotlin 2.4.20, and KSP 2.3.12.
- Updated libraries: Compose BOM 2026.09.00, Navigation 2.10.1, Room 2.8.5, OSS Licenses 17.5.2 (plugin 0.13.0), and Robolectric 4.17 (adds Android 17 / API 37 support for tests).
- AppCompat 1.8.0 and Material Components 1.14.0 are now declared explicitly instead of arriving transitively.
- Swipe-to-delete uses the current Material 3 `SwipeToDismissBox` API. The confirmation dialog behaves the same as before.

### Fixed

- Importing is now all-or-nothing, skips entries that already exist (or appear twice in the file), and reports how many were imported and skipped.
- Importing an old backup made on another device no longer imports encrypted text as passwords. The import is refused with an explanation.
- Encryption failures no longer fall back to storing plaintext, and unreadable entries are no longer shown as ciphertext.

- Copied passwords are now cleared from the clipboard without crashing on Android 8.1 (API 27), where `clearPrimaryClip()` is unavailable.

## [2.0.0] - 2024-02-11

See the [GitHub release](https://github.com/kudanilll/kupass/releases/tag/v2.0.0).

## [1.0.0] - 2023-10-26

See the [GitHub release](https://github.com/kudanilll/kupass/releases/tag/v1.0.0).

[Unreleased]: https://github.com/kudanilll/kupass/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/kudanilll/kupass/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/kudanilll/kupass/releases/tag/v1.0.0
