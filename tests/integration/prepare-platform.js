"use strict";

var fs = require("fs");
var path = require("path");

var appDirectory = process.argv[2];
var buildToolsVersion = process.argv[3];
var rootBuildFile = path.join(
    appDirectory || "", "platforms", "android", "build.gradle");
var buildFile = path.join(
    appDirectory || "", "platforms", "android", "CordovaLib", "build.gradle");
var cordovaGradleFile = path.join(
    appDirectory || "", "platforms", "android", "CordovaLib", "cordova.gradle");
var gradlePropertiesFile = path.join(
    appDirectory || "", "platforms", "android", "gradle.properties");

if (!appDirectory || !buildToolsVersion) {
    throw new Error(
        "Usage: node prepare-platform.js <cordova-app-directory> <build-tools-version>");
}

if (!fs.existsSync(buildFile)) {
    process.exit(0);
}

var source = fs.readFileSync(buildFile, "utf8");
var mavenCentralRepository =
    'maven { url "https://repo.maven.apache.org/maven2" }';

// cordova-android 7 configures obsolete Bintray publishing plugins during every
// application build. Bintray no longer serves one of those artifacts. They are
// unrelated to compiling an app, so remove only the generated fixture's
// publishing configuration. The plugin source under test is never modified.
source = source.replace(
    /^\s*classpath 'com\.github\.dcendents:android-maven-gradle-plugin:[^']+'\s*$/m,
    "");
source = source.replace(
    /^\s*classpath 'com\.jfrog\.bintray\.gradle:gradle-bintray-plugin:[^']+'\s*$/m,
    "");
source = source.replace(/^apply plugin: 'com\.github\.dcendents\.android-maven'\s*$/m, "");
source = source.replace(/^apply plugin: 'com\.jfrog\.bintray'\s*$/m, "");

// JCenter now redirects to Maven Central. Old Gradle versions can treat
// artifacts reached through that redirect as missing, so use Maven Central's
// canonical endpoint directly in the generated legacy project.
source = source.replace(/\bjcenter\(\)/g, mavenCentralRepository);

function removeSection(startMarker, nextMarker) {
    var start = source.indexOf(startMarker);
    if (start === -1) {
        return;
    }

    var end = nextMarker === null ? source.length : source.indexOf(nextMarker, start);
    if (end === -1) {
        throw new Error("Unable to remove obsolete publishing section from " + buildFile);
    }

    source = source.slice(0, start) + "\n" + source.slice(end);
}

removeSection("\ninstall {", "\ntask sourcesJar");
removeSection("\nbintray {", null);
source = source.replace(
    "cdvBuildToolsVersion = privateHelpers.findLatestInstalledBuildTools()",
    "cdvBuildToolsVersion = '" + buildToolsVersion + "'");

fs.writeFileSync(buildFile, source);

if (fs.existsSync(rootBuildFile)) {
    source = fs.readFileSync(rootBuildFile, "utf8");
    source = source.replace(/\bjcenter\(\)/g, mavenCentralRepository);
    fs.writeFileSync(rootBuildFile, source);
}

if (fs.existsSync(cordovaGradleFile)) {
    source = fs.readFileSync(cordovaGradleFile, "utf8");

    // cordova-android 9 uses a build-time version comparator that existed only
    // on the retired JCenter service. Its maintained Maven Central coordinate
    // has the same API used by cordova.gradle. This alters only the generated
    // integration fixture, not this plugin's dependency tree.
    if (source.indexOf("com.g00fy2:versioncompare:1.3.4") !== -1) {
        source = source.replace(
            "com.g00fy2:versioncompare:1.3.4",
            "io.github.g00fy2:versioncompare:1.5.0");
        source = source.replace(/\bjcenter\(\)/g, mavenCentralRepository);
    }

    source = source.replace(
        "com.g00fy2.versioncompare.Version",
        "io.github.g00fy2.versioncompare.Version");
    fs.writeFileSync(cordovaGradleFile, source);
}

if (fs.existsSync(gradlePropertiesFile)) {
    source = fs.readFileSync(gradlePropertiesFile, "utf8");
    if (!/^cdvBuildToolsVersion=/m.test(source)) {
        source += "\ncdvBuildToolsVersion=" + buildToolsVersion + "\n";
        fs.writeFileSync(gradlePropertiesFile, source);
    }
}
