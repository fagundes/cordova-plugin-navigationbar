"use strict";

var fs = require("fs");
var path = require("path");

var appDirectory = process.argv[2];
var compileSdk = process.argv[3];
var buildTools = process.argv[4];
var fixtureDirectory = path.join(__dirname, "www");

if (!appDirectory || !compileSdk || !buildTools) {
    throw new Error(
        "Usage: node prepare-app.js <cordova-app-directory> " +
        "<compile-sdk> <build-tools>");
}

["index.html", "app.js"].forEach(function (file) {
    fs.copyFileSync(
        path.join(fixtureDirectory, file),
        path.join(appDirectory, "www", file));
});

var configPath = path.join(appDirectory, "config.xml");
var configXml = fs.readFileSync(configPath, "utf8");
var preferences = [
    '    <preference name="NavigationBarBackgroundColor" value="#123456" />',
    '    <preference name="NavigationBarLight" value="false" />',
    '    <preference name="NavigationBarTransparent" value="false" />',
    '    <preference name="android-compileSdkVersion" value="' + compileSdk + '" />',
    '    <preference name="android-targetSdkVersion" value="' + compileSdk + '" />',
    '    <preference name="android-buildToolsVersion" value="' + buildTools + '" />'
].join("\n");

configXml = configXml.replace("</widget>", preferences + "\n</widget>");
fs.writeFileSync(configPath, configXml);
