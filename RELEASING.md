# Release process

Version 0.2.0 supports cordova-android 10.1.2 through 15.x and Android APIs
22–36. Some combinations remain end of life and must not be described as
secure or currently maintained.

## Local checks

Run from the repository root:

```sh
npm ci --ignore-scripts --no-audit
npm test
npm audit --audit-level=low
npm run test:package
```

Confirm that `package.json`, `package-lock.json`, and `plugin.xml` contain
the same version and inspect the package file list. Tests, CI files, and
integration fixtures must not be included in the npm package.

## Android build matrix

| cordova-android | JDK | Compile API | Build Tools |
| --- | ---: | ---: | --- |
| 10.1.2 | 11 | 30 | 30.0.3 |
| 11.0.0 | 11 | 32 | 32.0.0 |
| 12.0.1 | 11 | 33 | 33.0.2 |
| 13.0.0 | 17 | 34 | 34.0.0 |
| 14.0.1 | 17 | 35 | 35.0.0 |
| 15.1.0 | 17 | 36 | 36.0.0 |

Build a temporary application with:

```sh
tests/integration/run-android.sh 10.1.2
tests/integration/run-android.sh 11.0.0
tests/integration/run-android.sh 12.0.1
tests/integration/run-android.sh 13.0.0
tests/integration/run-android.sh 14.0.1
tests/integration/run-android.sh 15.1.0
```

## Emulator matrix

Run APIs 22, 23, 26, 29, 30, 32, 35, and 36. Verify:

- initial color, icon, and transparency preferences;
- valid and invalid colors;
- light and dark navigation icons;
- stable size results while shown and hidden;
- repeated hide/show cycles;
- status bar preservation;
- rotation, pause/resume, focus changes, and keyboard display.

Android 15 and newer enforce edge-to-edge behavior at the platform level.
Tests on API 35–36 must verify visibility and insets without assuming that the
system will honor every opaque navigation bar color.

## Publication handoff

After all checks pass, review the diff and release notes. Tagging, creating a
GitHub Release, and running `npm publish` remain manual actions and are not
performed by CI.
