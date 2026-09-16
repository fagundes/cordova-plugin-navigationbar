# Release process

Version 0.1.1 is a legacy release for cordova-android 7.1.4-9.1.0. Do not
describe the host Android or Cordova toolchains as secure or currently
supported.

## Local checks

Run from the repository root:

```sh
npm test
npm audit --audit-level=low
npm run test:package
```

Confirm that `package.json`, `package-lock.json`, and `plugin.xml` all contain
the same version and inspect the `npm pack --dry-run` file list. Tests, CI files,
and integration fixtures must not be included in the package.

## Android build matrix

Build a temporary Cordova application with each supported platform:

| cordova-android | JDK | compile SDK | Build Tools | bootstrap Gradle |
| --- | --- | --- | --- | --- |
| 7.1.4 | 8 | 27 | 27.0.3 | 4.10.3 |
| 8.1.0 | 8 | 28 | 28.0.3 | 4.10.3 |
| 9.1.0 | 8 | 29 | 29.0.3 | 6.5 |

The CI fixture uses Cordova CLI 9 under Node.js 12 to install those legacy
platform releases, then runs the repository plugin directly:

```sh
tests/integration/run-android.sh 7.1.4
tests/integration/run-android.sh 8.1.0
tests/integration/run-android.sh 9.1.0
```

Run the integration application on API 19, 21, 26, and 29 emulators. Verify
initial preferences, color validation, light/dark icons, repeated hide/show,
rotation, pause/resume, focus changes, keyboard display, and that status bar
visibility never changes.

The integration fixture removes cordova-android 7's unused publishing-only
Bintray plugins from its generated `CordovaLib/build.gradle`. For
cordova-android 9, it redirects the build-time `versioncompare` library from
its retired JCenter coordinate to the maintained Maven Central coordinate.
These compatibility adjustments do not modify the plugin or the application
code being compiled.

## Publication handoff

After all checks pass, review the diff and release notes. Tagging, creating a
GitHub Release, and running `npm publish` are intentionally manual actions and
are not performed by the repository's validation workflow.
