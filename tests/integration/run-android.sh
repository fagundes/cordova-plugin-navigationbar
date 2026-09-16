#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
    echo "Usage: tests/integration/run-android.sh <cordova-android-version>" >&2
    exit 2
fi

platform_version=$1
case "$platform_version" in
    7.1.4) build_tools_version=27.0.3 ;;
    8.1.0) build_tools_version=28.0.3 ;;
    9.1.0) build_tools_version=29.0.3 ;;
    *)
        echo "Unsupported cordova-android version: $platform_version" >&2
        exit 2
        ;;
esac
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
        /tmp/navigationbar-integration.*) rm -rf -- "$test_root" ;;
        *) echo "Refusing to remove unexpected test path: $test_root" >&2 ;;
    esac
}
trap cleanup EXIT INT TERM

cordova create "$app_directory" io.github.fagundes.navigationbartest NavigationBarTest
node "$repository_root/tests/integration/prepare-app.js" "$app_directory"

cd "$app_directory"
cordova platform add "android@$platform_version"
cordova plugin add "$repository_root"
cordova plugin add "$repository_root/tests/integration/inspector"
node "$repository_root/tests/integration/prepare-platform.js" \
    "$app_directory" "$build_tools_version"
cordova build android --debug

if [ "${RUN_EMULATOR_TESTS:-false}" != "true" ]; then
    exit 0
fi

adb logcat -c || true
logcat_file="$test_root/logcat.txt"
adb logcat -v brief chromium:I SystemWebChromeClient:I '*:S' \
    >"$logcat_file" 2>&1 &
logcat_pid=$!

apk_path="$app_directory/platforms/android/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$apk_path" ]; then
    apk_path="$app_directory/platforms/android/build/outputs/apk/android-debug.apk"
fi
if [ ! -f "$apk_path" ]; then
    echo "Unable to locate the integration APK" >&2
    exit 1
fi

adb install -r "$apk_path"
adb shell am force-stop io.github.fagundes.navigationbartest
timeout 15 adb shell am start \
    -n io.github.fagundes.navigationbartest/.MainActivity

wait_for_marker() {
    expected_marker=$1
    max_attempts=$2
    timeout_message=$3
    attempt=0

    echo "Waiting for $expected_marker"
    while [ "$attempt" -lt "$max_attempts" ]; do
        if grep -q "NAVIGATIONBAR_TEST_FAIL" "$logcat_file"; then
            tail -n 500 "$logcat_file" >&2
            return 1
        fi
        if grep -q "$expected_marker" "$logcat_file"; then
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
    done

    tail -n 500 "$logcat_file" >&2
    timeout 10 adb logcat -d -v brief >&2 || true
    echo "$timeout_message" >&2
    return 1
}

wait_for_marker \
    "NAVIGATIONBAR_TEST_READY" 60 \
    "Timed out waiting for the core integration tests"

restore_rotation=true
timeout 10 adb shell settings put system accelerometer_rotation 0 || true
timeout 10 adb shell settings put system user_rotation 1

wait_for_marker \
    "NAVIGATIONBAR_TEST_ROTATED" 60 \
    "Timed out waiting for the rotation test"

adb shell input keyevent KEYCODE_HOME

wait_for_marker \
    "NAVIGATIONBAR_TEST_PAUSED" 30 \
    "Timed out waiting for the pause event"

timeout 15 adb shell am start \
    -n io.github.fagundes.navigationbartest/.MainActivity

wait_for_marker \
    "NAVIGATIONBAR_TEST_PASS" 60 \
    "Timed out waiting for the resume test"

timeout 10 adb shell settings put system accelerometer_rotation 1 || true
echo "NavigationBar integration test passed"
