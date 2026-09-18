# Changelog

All notable changes to Kupass are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Updated the build toolchain to the latest stable releases: Android Gradle Plugin 9.4.0, Gradle 9.7.1, Kotlin 2.4.20, and KSP 2.3.12.
- Updated libraries: Compose BOM 2026.09.00, Navigation 2.10.1, Room 2.8.5, OSS Licenses 17.5.2 (plugin 0.13.0), and Robolectric 4.17 (adds Android 17 / API 37 support for tests).
- AppCompat 1.8.0 and Material Components 1.14.0 are now declared explicitly instead of arriving transitively.
- Swipe-to-delete uses the current Material 3 `SwipeToDismissBox` API. The confirmation dialog behaves the same as before.

### Fixed

- Copied passwords are now cleared from the clipboard without crashing on Android 8.1 (API 27), where `clearPrimaryClip()` is unavailable.

## [2.0.0] - 2024-02-11

See the [GitHub release](https://github.com/kudanilll/kupass/releases/tag/v2.0.0).

## [1.0.0] - 2023-10-26

See the [GitHub release](https://github.com/kudanilll/kupass/releases/tag/v1.0.0).

[Unreleased]: https://github.com/kudanilll/kupass/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/kudanilll/kupass/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/kudanilll/kupass/releases/tag/v1.0.0
