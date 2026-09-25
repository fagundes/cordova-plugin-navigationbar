# Changelog

All notable changes to this project are documented in this file.

## 0.2.0

### Added

- Add transparent navigation bar and WebView overlay support on Android 11+,
  based on the contribution in PR #18 by @ollm.
- Add NavigationBar.size() with density-independent and physical dimensions
  plus the navigation bar position.
- Add native WindowInsetsController behavior for Android 11+.

### Changed

- Support cordova-android 10.1.2 through 15.x and Android APIs 22–36.
- Reapply requested navigation bar state after resume, configuration, and focus
  changes.
- Test modern Cordova, JDK, SDK, and emulator combinations.
- Install Gradle directly in CI to preserve compatibility with older
  cordova-android versions.

### Fixed

- Keep status bar flags unchanged.
- Complete all Cordova callbacks and avoid background executor leaks.
- Preserve light/dark icon appearance while applying other system UI flags,
  building on PR #20 by @fquirin.
- Modernize system bar handling for Android versions newer than API 30,
  addressing issue #22.

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
