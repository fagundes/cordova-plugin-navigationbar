"use strict";

var assert = require("assert");
var childProcess = require("child_process");
var fs = require("fs");
var Module = require("module");
var os = require("os");
var path = require("path");

var root = path.resolve(__dirname, "..");
var tests = [];

function test(name, callback) {
    tests.push({ name: name, callback: callback });
}

function withNavigationBar(initialVisibility, callback) {
    var calls = [];
    var scheduled = [];
    var errors = [];
    var originalLoad = Module._load;
    var originalWindow = global.window;
    var originalCordova = global.cordova;
    var originalConsoleError = console.error;
    var modulePath = path.join(root, "www", "navigationbar.js");

    function exec(success, fail, service, action, args) {
        calls.push({
            success: success,
            fail: fail,
            service: service,
            action: action,
            args: args
        });

        if (action === "_ready" && typeof success === "function") {
            success(initialVisibility);
        }
    }

    Module._load = function (request, parent, isMain) {
        if (request === "cordova/exec") {
            return exec;
        }
        return originalLoad.call(this, request, parent, isMain);
    };

    global.window = {
        setTimeout: function (scheduledCallback) {
            scheduled.push(scheduledCallback);
        }
    };
    global.cordova = {
        fireWindowEvent: function () {}
    };
    console.error = function (message) {
        errors.push(message);
    };

    delete require.cache[require.resolve(modulePath)];

    try {
        var navigationBar = require(modulePath);
        scheduled.forEach(function (scheduledCallback) {
            scheduledCallback();
        });
        callback(navigationBar, calls, errors);
    } finally {
        delete require.cache[require.resolve(modulePath)];
        Module._load = originalLoad;
        global.window = originalWindow;
        global.cordova = originalCordova;
        console.error = originalConsoleError;
    }
}

test("initializes isVisible from the native _ready action", function () {
    withNavigationBar(false, function (navigationBar, calls) {
        assert.strictEqual(navigationBar.isVisible, false);
        assert.strictEqual(calls.length, 1);
        assert.strictEqual(calls[0].service, "NavigationBar");
        assert.strictEqual(calls[0].action, "_ready");
    });
});

test("normalizes supported RGB and ARGB color formats", function () {
    withNavigationBar(true, function (navigationBar, calls) {
        calls.length = 0;

        navigationBar.backgroundColorByHexString("abc", true);
        navigationBar.backgroundColorByHexString("#1a2b", false);
        navigationBar.backgroundColorByHexString("112233", false);
        navigationBar.backgroundColorByHexString("#80112233", true);

        assert.deepStrictEqual(calls.map(function (call) {
            return call.args;
        }), [
            ["#aabbcc", true],
            ["#11aa22bb", false],
            ["#112233", false],
            ["#80112233", true]
        ]);
    });
});

test("maps named colors without changing the public API", function () {
    withNavigationBar(true, function (navigationBar, calls) {
        calls.length = 0;
        navigationBar.backgroundColorByName("blue", true);

        assert.strictEqual(calls.length, 1);
        assert.strictEqual(calls[0].action, "backgroundColorByHexString");
        assert.deepStrictEqual(calls[0].args, ["#0000FF", true]);
    });
});

test("rejects invalid colors without invoking native code", function () {
    withNavigationBar(true, function (navigationBar, calls, errors) {
        calls.length = 0;

        navigationBar.backgroundColorByHexString("#12", false);
        navigationBar.backgroundColorByHexString(null, false);
        navigationBar.backgroundColorByName("not-a-color", false);

        assert.strictEqual(calls.length, 0);
        assert.strictEqual(errors.length, 3);
    });
});

test("hide and show preserve their synchronous isVisible behavior", function () {
    withNavigationBar(true, function (navigationBar, calls) {
        calls.length = 0;

        navigationBar.hide();
        assert.strictEqual(navigationBar.isVisible, false);
        navigationBar.show();
        assert.strictEqual(navigationBar.isVisible, true);

        assert.deepStrictEqual(calls.map(function (call) {
            return call.action;
        }), ["hide", "show"]);
    });
});

test("browser proxy exposes asynchronous no-op implementations", function () {
    var registered;
    var scheduled = [];
    var originalLoad = Module._load;
    var originalSetTimeout = global.setTimeout;
    var originalConsoleLog = console.log;
    var modulePath = path.join(root, "src", "browser", "NavigationBarProxy.js");

    Module._load = function (request, parent, isMain) {
        if (request === "cordova/exec/proxy") {
            return {
                add: function (service, proxy) {
                    registered = { service: service, proxy: proxy };
                }
            };
        }
        return originalLoad.call(this, request, parent, isMain);
    };
    global.setTimeout = function (scheduledCallback) {
        scheduled.push(scheduledCallback);
    };
    console.log = function () {};

    delete require.cache[require.resolve(modulePath)];

    try {
        require(modulePath);
        assert.strictEqual(registered.service, "NavigationBar");
        assert.strictEqual(typeof registered.proxy.hide, "function");
        assert.strictEqual(typeof registered.proxy.show, "function");

        var completed = false;
        registered.proxy.hide(function () {
            completed = true;
        });
        assert.strictEqual(completed, false);
        scheduled[0]();
        assert.strictEqual(completed, true);

        var initialVisibility;
        registered.proxy._ready(function (visible) {
            initialVisibility = visible;
        });
        assert.strictEqual(initialVisibility, undefined);
        scheduled[1]();
        assert.strictEqual(initialVisibility, false);
    } finally {
        delete require.cache[require.resolve(modulePath)];
        Module._load = originalLoad;
        global.setTimeout = originalSetTimeout;
        console.log = originalConsoleLog;
    }
});

test("keeps package and plugin metadata aligned", function () {
    var packageJson = JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8"));
    var packageLock = JSON.parse(fs.readFileSync(path.join(root, "package-lock.json"), "utf8"));
    var pluginXml = fs.readFileSync(path.join(root, "plugin.xml"), "utf8");

    assert.strictEqual(packageJson.version, "0.1.1");
    assert.strictEqual(packageLock.version, packageJson.version);
    assert.strictEqual(packageLock.packages[""].version, packageJson.version);
    assert.deepStrictEqual(packageLock.packages[""].engines, packageJson.engines);
    assert.ok(pluginXml.indexOf('version="0.1.1"') !== -1);
    assert.ok(pluginXml.indexOf('version=">=7.1.4 &lt;10.0.0"') !== -1);
    assert.deepStrictEqual(
        Object.keys(packageJson.engines.cordovaDependencies),
        ["0.1.0", "0.1.1"]);
    assert.strictEqual(packageJson.dependencies, undefined);
    assert.strictEqual(packageJson.devDependencies, undefined);
    assert.deepStrictEqual(Object.keys(packageLock.packages), [""]);
});

test("does not modify status-bar fullscreen flags", function () {
    var javaSource = fs.readFileSync(
        path.join(root, "src", "android", "NavigationBar.java"), "utf8");

    assert.strictEqual(javaSource.indexOf("FLAG_FULLSCREEN"), -1);
    assert.strictEqual(javaSource.indexOf("FLAG_FORCE_NOT_FULLSCREEN"), -1);
    assert.strictEqual(javaSource.indexOf("FLAG_TRANSLUCENT_STATUS"), -1);
    assert.strictEqual(javaSource.indexOf("callbackContext.error"), -1);
    assert.ok(javaSource.indexOf("FLAG_TRANSLUCENT_NAVIGATION") !== -1);
    assert.ok(javaSource.indexOf("FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS") !== -1);
    assert.ok(javaSource.indexOf("[0-9a-fA-F]{3}") !== -1);
});

test("rewrites retired repositories in generated legacy Android projects", function () {
    var fixture = fs.mkdtempSync(path.join(os.tmpdir(), "navigationbar-platform-test-"));
    var androidDirectory = path.join(fixture, "platforms", "android");
    var cordovaLibDirectory = path.join(androidDirectory, "CordovaLib");
    var canonicalRepository =
        'maven { url "https://repo.maven.apache.org/maven2" }';

    fs.mkdirSync(cordovaLibDirectory, { recursive: true });
    fs.writeFileSync(path.join(androidDirectory, "build.gradle"),
        "repositories { jcenter() }\n");
    fs.writeFileSync(path.join(cordovaLibDirectory, "build.gradle"), [
        "repositories { jcenter() }",
        "cdvBuildToolsVersion = privateHelpers.findLatestInstalledBuildTools()"
    ].join("\n"));
    fs.writeFileSync(path.join(cordovaLibDirectory, "cordova.gradle"), [
        "repositories { jcenter() }",
        'implementation "com.g00fy2:versioncompare:1.3.4"',
        "com.g00fy2.versioncompare.Version"
    ].join("\n"));

    try {
        var result = childProcess.spawnSync(process.execPath, [
            path.join(root, "tests", "integration", "prepare-platform.js"),
            fixture,
            "27.0.3"
        ], { encoding: "utf8" });
        assert.strictEqual(result.status, 0, result.stderr);

        var rootBuild = fs.readFileSync(
            path.join(androidDirectory, "build.gradle"), "utf8");
        var cordovaBuild = fs.readFileSync(
            path.join(cordovaLibDirectory, "build.gradle"), "utf8");
        var cordovaGradle = fs.readFileSync(
            path.join(cordovaLibDirectory, "cordova.gradle"), "utf8");

        assert.ok(rootBuild.indexOf(canonicalRepository) !== -1);
        assert.ok(cordovaBuild.indexOf(canonicalRepository) !== -1);
        assert.ok(cordovaGradle.indexOf(canonicalRepository) !== -1);
        assert.strictEqual(rootBuild.indexOf("jcenter()"), -1);
        assert.strictEqual(cordovaBuild.indexOf("jcenter()"), -1);
        assert.strictEqual(cordovaGradle.indexOf("jcenter()"), -1);
        assert.ok(cordovaBuild.indexOf("cdvBuildToolsVersion = '27.0.3'") !== -1);
        assert.ok(cordovaGradle.indexOf(
            "io.github.g00fy2:versioncompare:1.5.0") !== -1);
    } finally {
        fs.rmSync(fixture, { recursive: true, force: true });
    }
});

test("all shipped JavaScript parses successfully", function () {
    [
        path.join(root, "www", "navigationbar.js"),
        path.join(root, "src", "browser", "NavigationBarProxy.js"),
        path.join(root, "tests", "integration", "prepare-app.js"),
        path.join(root, "tests", "integration", "prepare-platform.js"),
        path.join(root, "tests", "integration", "www", "app.js"),
        path.join(root, "tests", "integration", "inspector", "www", "inspector.js"),
        __filename
    ].forEach(function (file) {
        var result = childProcess.spawnSync(process.execPath, ["--check", file], {
            encoding: "utf8"
        });
        assert.strictEqual(result.status, 0, result.stderr);
    });
});

var failures = 0;

tests.forEach(function (currentTest) {
    try {
        currentTest.callback();
        process.stdout.write("ok - " + currentTest.name + "\n");
    } catch (error) {
        failures += 1;
        process.stderr.write("not ok - " + currentTest.name + "\n");
        process.stderr.write(error.stack + "\n");
    }
});

if (failures > 0) {
    process.exitCode = 1;
} else {
    process.stdout.write("\n" + tests.length + " tests passed\n");
}
