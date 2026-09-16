# Security policy

## Supported versions

| Version | Support status |
| --- | --- |
| 0.1.1 | Legacy security and correctness fixes |
| 0.1.0 and older | Unsupported |

## Scope

The npm package contains no production or development dependencies. A clean
`npm audit` therefore covers the dependency tree distributed by this plugin.

Version 0.1.1 intentionally supports end-of-life Android and cordova-android
versions. Vulnerabilities in the Android operating system, WebView, Java,
Gradle, Cordova CLI, or host application's dependencies are outside this
plugin's security boundary and must be assessed by the application maintainer.

## Reporting a vulnerability

Do not open a public issue for an undisclosed vulnerability. Use GitHub's
private vulnerability reporting feature for this repository. Include affected
versions, reproduction steps, impact, and any suggested mitigation.
