## Summary

<!-- What changes, and why? -->

## Related issue

<!-- For example: Closes #12. Write "None" for small fixes without an issue. -->

## How did you test it?

<!-- Commands you ran, devices or emulators and their Android versions, and what you checked by hand. -->

## Checklist

- [ ] I ran `./gradlew spotlessApply`, and `./gradlew spotlessCheck detektMain detektTest testDebugUnitTest lintDebug assembleDebug` passes.
- [ ] Logic changes come with new or updated tests.
- [ ] New user-facing text is in both `values/strings.xml` and `values-in/strings.xml`.
- [ ] Room schema changes include a `Migration` and a migration test (or there is no schema change).
- [ ] No passwords, vault data, or decrypted values end up in logs, toasts, or plaintext files.
- [ ] No telemetry, and no new network request that carries vault data.
- [ ] User-visible changes are listed under `## [Unreleased]` in `CHANGELOG.md`.
- [ ] Every commit is signed off (`git commit -s`).

<!-- Kupass blocks screenshots in the app. For UI changes, describe what changed or attach screenshots from a debug build where you disabled FLAG_SECURE locally. -->
