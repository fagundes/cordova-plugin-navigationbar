# Security policy

## Supported versions

| Version | Support status |
| --- | --- |
| 0.2.x | Supported |
| 0.1.1 | Legacy security and correctness fixes |
| 0.1.0 and older | Unsupported |

## Scope

The npm package contains no production or development dependencies. A clean
`npm audit` therefore covers the dependency tree distributed by this plugin.

Android, Cordova, Java, Gradle, WebView, and application dependencies are host
toolchain components and are outside the plugin package's dependency tree.
Several compatible Android and cordova-android versions are end of life, so
application maintainers must assess those components separately.

## Reporting a vulnerability

Do not open a public issue for an undisclosed vulnerability. Use GitHub's
private vulnerability reporting feature for this repository. Include affected
versions, reproduction steps, impact, and any suggested mitigation.
