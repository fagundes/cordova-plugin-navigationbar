#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
    echo "Usage: tests/integration/run-android.sh <cordova-android-version>" >&2
    exit 2
fi

platform_version=$1

case "$platform_version" in
    10.1.2) compile_sdk=30; build_tools_version=30.0.3 ;;
    11.0.0) compile_sdk=32; build_tools_version=32.0.0 ;;
    12.0.1) compile_sdk=33; build_tools_version=33.0.2 ;;
    13.0.0) compile_sdk=34; build_tools_version=34.0.0 ;;
    14.0.1) compile_sdk=35; build_tools_version=35.0.0 ;;
    15.1.0) compile_sdk=36; build_tools_version=36.0.0 ;;
    *)
        echo "Unsupported cordova-android version: $platform_version" >&2
        exit 2
        ;;
esac

package_name="io.github.fagundes.navigationbartest"

echo "Integration test environment:"
echo "  cordova-android: $platform_version"
echo "  compileSdk:      $compile_sdk"
echo "  build-tools:     $build_tools_version"
echo "  JAVA_HOME:       ${JAVA_HOME:-<unset>}"
echo "  ANDROID_API:     ${ANDROID_API:-<unset>}"
java -version
gradle --version
cordova --version
echo

repository_root=$(CDPATH= cd -- "$(dirname "$0")/../.." && pwd)
test_root=$(mktemp -d /tmp/navigationbar-integration.XXXXXX)
app_directory="$test_root/app"

restore_rotation=false
logcat_pid=

cleanup() {
    if [ -n "$logcat_pid" ]; then
        kill "$logcat_pid" >/dev/null 2>&1 || true
        wait "$logcat_pid" 2>/dev/null || true
    fi

    if [ "$restore_rotation" = "true" ]; then
        timeout 10 adb shell settings put system accelerometer_rotation 1 \
            >/dev/null 2>&1 || true
    fi

    if [ "${KEEP_INTEGRATION_APP:-false}" = "true" ]; then
        echo "Integration app kept at: $test_root"
        return
    fi

    case "$test_root" in
        /tmp/navigationbar-integration.*)
            rm -rf -- "$test_root"
            ;;
        *)
            echo "Refusing to remove unexpected test path: $test_root" >&2
            ;;
    esac
}

trap cleanup EXIT INT TERM

wait_for_boot() {
    echo "Waiting for emulator boot completion..."

    timeout 180 sh -c '
        until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d "\r")" = "1" ]; do
            sleep 2
        done
    '

    echo "Emulator reports sys.boot_completed=1"

    echo "Waiting for Android package manager..."

    timeout 60 sh -c '
        until adb shell service check package 2>/dev/null | grep -q "found"; do
            sleep 2
        done
    '

    echo "Android package manager is ready"

    adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
    adb shell input keyevent 82 >/dev/null 2>&1 || true

    echo "Allowing emulator services to settle..."
    sleep 15
}

dump_app_state() {
    echo "=== App diagnostics ===" >&2

    echo "--- Package ---" >&2
    adb shell pm path "$package_name" >&2 || true

    echo "--- PID ---" >&2
    adb shell pidof "$package_name" >&2 || true

    echo "--- Foreground activity ---" >&2
    adb shell dumpsys activity activities 2>/dev/null \
        | grep -E 'mResumedActivity|topResumedActivity|ResumedActivity' \
        >&2 || true

    echo "--- Process state ---" >&2
    adb shell dumpsys activity processes 2>/dev/null \
        | grep -A 8 -B 2 "$package_name" \
        >&2 || true
}

dump_relevant_logs() {
    echo "=== Relevant integration log ===" >&2

    if [ -f "$logcat_file" ]; then
        grep -i -E \
            'NAVIGATIONBAR_TEST_|navigationbartest|Cordova|chromium|SystemWebChromeClient|WebChrome|AndroidRuntime|FATAL EXCEPTION' \
            "$logcat_file" \
            >&2 || true
    fi

    echo "=== Last 500 captured log lines ===" >&2
    tail -n 500 "$logcat_file" >&2 || true

    echo "=== Current device log ===" >&2
    timeout 10 adb logcat -d -v brief \
        | grep -i -E \
            'NAVIGATIONBAR_TEST_|navigationbartest|Cordova|chromium|SystemWebChromeClient|WebChrome|AndroidRuntime|FATAL EXCEPTION' \
        >&2 || true
}

start_app() {
    echo "Starting integration application for API ${ANDROID_API:-unknown}..."

    if [ "${ANDROID_API:-0}" -le 30 ]; then
        timeout 30 adb shell am start -W \
            -n "$package_name/.MainActivity"

    elif [ "${ANDROID_API:-0}" -eq 32 ]; then
        timeout 30 adb shell monkey \
            -p "$package_name" \
            -c android.intent.category.LAUNCHER \
            1

    else
        launch_activity=$(
            adb shell cmd package resolve-activity \
                --brief \
                -a android.intent.action.MAIN \
                -c android.intent.category.LAUNCHER \
                "$package_name" \
                | tail -n 1 \
                | tr -d '\r'
        )

        if [ -z "$launch_activity" ]; then
            echo "Unable to resolve launcher activity" >&2
            dump_app_state
            exit 1
        fi

        echo "Resolved launcher activity: $launch_activity"

        timeout 30 adb shell am start -W \
            -n "$launch_activity"
    fi

    sleep 2
    dump_app_state
}

wait_for_marker() {
    expected_marker=$1
    max_attempts=$2
    timeout_message=$3

    attempt=0

    echo "Waiting for $expected_marker"

    while [ "$attempt" -lt "$max_attempts" ]; do
        if grep -q "NAVIGATIONBAR_TEST_FAIL" "$logcat_file"; then
            echo "Integration test reported failure" >&2
            dump_app_state
            dump_relevant_logs
            return 1
        fi

        if grep -q "$expected_marker" "$logcat_file"; then
            echo "Found marker: $expected_marker"
            return 0
        fi

        attempt=$((attempt + 1))
        sleep 2
    done

    echo "$timeout_message" >&2
    dump_app_state
    dump_relevant_logs

    return 1
}

cordova create \
    "$app_directory" \
    "$package_name" \
    NavigationBarTest

node "$repository_root/tests/integration/prepare-app.js" \
    "$app_directory" \
    "$compile_sdk" \
    "$build_tools_version"

cd "$app_directory"

cordova platform add "android@$platform_version"

cordova plugin add "$repository_root"
cordova plugin add "$repository_root/tests/integration/inspector"

cordova build android --debug

if [ "${RUN_EMULATOR_TESTS:-false}" != "true" ]; then
    exit 0
fi

wait_for_boot

apk_path="$app_directory/platforms/android/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$apk_path" ]; then
    apk_path="$app_directory/platforms/android/build/outputs/apk/android-debug.apk"
fi

if [ ! -f "$apk_path" ]; then
    echo "Unable to locate the integration APK" >&2
    exit 1
fi

echo "Installing integration APK:"
echo "  $apk_path"

adb install -r "$apk_path"

echo "Verifying package installation..."
adb shell pm path "$package_name"

adb shell am force-stop "$package_name" || true

echo "Preparing logcat capture..."

adb logcat -c || true

logcat_file="$test_root/logcat.txt"

# Intentionally capture all logcat output here.
# This avoids depending on WebView/Chromium tag differences
# between local environments and CI/emulator images.
adb logcat -v brief >"$logcat_file" 2>&1 &
logcat_pid=$!

sleep 2

start_app

wait_for_marker \
    "NAVIGATIONBAR_TEST_READY" 120 \
    "Timed out waiting for the core integration tests"

restore_rotation=true

echo "Disabling automatic rotation..."
timeout 10 adb shell settings put system accelerometer_rotation 0 || true

echo "Rotating device..."
timeout 10 adb shell settings put system user_rotation 1

wait_for_marker \
    "NAVIGATIONBAR_TEST_ROTATED" 90 \
    "Timed out waiting for the rotation test"

echo "Sending application to background..."
adb shell input keyevent KEYCODE_HOME

wait_for_marker \
    "NAVIGATIONBAR_TEST_PAUSED" 60 \
    "Timed out waiting for the pause event"

start_app

wait_for_marker \
    "NAVIGATIONBAR_TEST_PASS" 90 \
    "Timed out waiting for the resume test"

timeout 10 adb shell settings put system accelerometer_rotation 1 || true

restore_rotation=false

echo "NavigationBar integration test passed"