"use strict";

var fs = require("fs");
var path = require("path");

var appDirectory = process.argv[2];
var fixtureDirectory = path.join(__dirname, "www");

if (!appDirectory) {
    throw new Error("Usage: node prepare-app.js <cordova-app-directory>");
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
    '    <preference name="NavigationBarLight" value="false" />'
].join("\n");

configXml = configXml.replace("</widget>", preferences + "\n</widget>");
fs.writeFileSync(configPath, configXml);
