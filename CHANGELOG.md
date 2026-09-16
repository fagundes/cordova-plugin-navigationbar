# Changelog

All notable changes to this project are documented in this file.

## 0.1.1

### Fixed

- Restore the navigation bar correctly by clearing all hide and immersive flags.
- Detect navigation bar visibility instead of checking the unrelated fullscreen
  window flag.
- Set navigation bar colors with the correct Android window flags.
- Keep status bar visibility unchanged when showing or hiding the navigation bar.
- Reject invalid color names and hexadecimal strings without crashing.
- Complete native Cordova actions after their UI-thread work finishes.
- Provide browser no-op implementations for `show()` and `hide()`.

### Security

- Remove JSHint and its vulnerable transitive dependency tree.
- Keep the published plugin free of npm runtime and development dependencies.

### Compatibility

- Define the supported range as cordova-android 7.1.4 through 9.x.
- Document behavior boundaries for Android APIs 19, 21, and 26.
