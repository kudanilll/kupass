# Changelog

All notable changes to Kupass are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security

- Account access and background exemptions are isolated per activity instance. Starting or authenticating another Kupass activity cannot revive an account whose background timeout has expired.
- Copied passwords are cleared from the clipboard after 45 seconds on every Android version, including when Kupass is in the background.
- Vault data is excluded from Android cloud backup and device-to-device transfer on every Android version. Use the password-protected export to move your vault.
- Site names, usernames, URLs, and notes are now encrypted on the device like passwords. Existing vaults are upgraded automatically on first launch.

### Added

- Local import from Google Password Manager CSV exports, including notes and Android app entries. Quoted multiline fields and exact password whitespace are preserved; invalid records and duplicates are reported as skipped.
- Account authentication: each new account open requires fingerprint/face or the device PIN/pattern/password before its details load. Editing from that detail and returning shares the same authorization. Sensitive account screens lock after the selected background timeout; launch, browsing, search, tabs, and new-entry creation stay available without authentication.
- Site icons in the vault list, with domain-only requests to the Favget icon service and in-memory caching.
- Empty states for an empty vault (with an "Add password" button) and for searches without results.
- Portable encrypted backups: export asks for a backup password and encrypts the whole vault (PBKDF2-HMAC-SHA256 + AES-256-GCM), so a backup can be restored on a new phone or after reinstalling.

### Changed

- Encrypted backups now use the name `kupass-backup.kupass`, with the same portable v2 encrypted JSON format. Older `.json` backups remain importable, and import detects the format from validated content rather than filenames or provider MIME types.
- Every export requires fresh native authentication before the backup-password dialog. Permission is bound to one export request, expires after five minutes, and is consumed before writing the encrypted backup. Cancelled or stale pickers cannot export.
- Restored detail/editor destinations require authentication before any sensitive screen or ViewModel loads. Devices without a screen lock show guidance only when opening an existing account or exporting. An existing account that fails to load in the editor can never become an accidental new account.
- Site icons are always enabled when an entry has a public domain and the build has a Favget API key; the Settings toggle was removed. When the URL is empty, a single-word site name such as `GitHub` tries `github.com`.
- Dynamic colors now default to enabled on supported devices, and the open-source licenses screen follows Kupass's selected color scheme.
- Tightened the spacing between the bottom navigation and add button, added a subtle navigation shadow, and removed the duplicate add button from the empty-vault state.
- Updated the build toolchain to the latest stable releases: Android Gradle Plugin 9.4.1, Gradle 9.8.1 (checksum-verified wrapper), Kotlin 2.4.21 (Compose compiler and serialization plugin), and KSP 2.3.12.
- Updated development tooling: Spotless 8.10.4, Compose detekt rules 0.6.8, and the SHA-pinned upload-artifact action 7.0.2.
- The developer entry in Settings now opens `https://achmaddaniel.nielcode.com`.
- Updated libraries: Compose BOM 2026.09.00, Navigation 2.10.1, Room 2.8.5, OSS Licenses 17.5.2 (plugin 0.13.0), and Robolectric 4.17 (adds Android 17 / API 37 support for tests).
- AppCompat 1.8.0 and Material Components 1.14.0 are now declared explicitly instead of arriving transitively.
- Redesigned bottom navigation: smoother spring animations, Material 3 tonal colors, and shadows on the navigation bar and the add button.
- The password detail screen puts Edit and Delete in one overflow menu.
- Text fields in the editor and backup dialogs match the password detail cards, and the editor stays usable with the keyboard open.
- Settings and Data pages fill the screen and scroll clear of the floating navigation.
- Settings shares Home's bottom content gradient. The navigation shadow is centered, and Data keeps navigation visible while providing native overscroll stretch on short content.
- Backup loading dialogs show the current import/export operation with centered progress and explanatory text.
- Search filters the already-decrypted vault in memory instead of decrypting every entry on each keystroke.
- Sites/apps with multiple accounts use the same vault list item with a count badge at the left of its icon. Tapping opens a separate account list; individual accounts still require authentication. Single-account sites remain normal account items.
- Search uses one input without an expanding popup. The heading collapses smoothly as search moves upward, results animate in place, and group badges retain their total account count when filtering.
- The app font is 94% smaller (3.9 MB → 236 KB) with identical rendering.
- Swipe an individual account physically left-to-right to request its existing delete confirmation; leftward swipes on Home reach Data, and vertical swipes still scroll. Site/app items never perform bulk deletion; accessibility delete uses the same confirmation.

### Fixed

- Google CSV import accepts normalized headers, `note`/`notes`, blank record separators, comma/semicolon delimiters, and BOM-marked UTF-16 as well as strict UTF-8. Credential fields are preserved exactly; malformed data still inserts nothing.
- CSV errors identify the supported columns instead of incorrectly referring to a JSON file.
- Editing imported entries preserves username and note formatting and accepts nonempty whitespace-only passwords. Empty passwords and blank site names still disable Save.
- Import waits for a cancelled native export prompt's terminal callback before accessing the selected file or vault.
- Imports reject malformed UTF-8, CSV headers, quotes, and row widths before any insertion, and limit input to 32 MiB and 10,000 data entries including skipped rows. Duplicate submits are ignored and cancelled import passwords are cleared.
- Android app facet URLs never trigger site-icon lookups for package names or guessed website domains.
- Rapid returns from the Android launcher now enforce the account background timeout even when the activity resumes without stopping first.
- Type-aware detekt analysis now resolves generated Java classes such as `BuildConfig` instead of reporting compiler errors.
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
