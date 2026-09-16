#!/bin/sh
set -eu

# A native activity must receive focus before Cordova or the plugin is installed.
echo "Checking Android UI health with Settings..."
if ! timeout 30 adb shell am start -W -a android.settings.SETTINGS; then
    echo "EMULATOR_HEALTH_FAIL: Android Settings could not start" >&2
    exit 1
fi

attempt=0
while [ "$attempt" -lt 15 ]; do
    windows=$(timeout 5 adb shell dumpsys window windows) || {
        echo "EMULATOR_HEALTH_FAIL: WindowManager did not respond" >&2
        exit 1
    }
    focus=$(printf '%s\n' "$windows" | grep 'mCurrentFocus=' || true)
    if printf '%s\n' "$focus" | grep -q 'Application Not Responding'; then
        echo "EMULATOR_HEALTH_FAIL: $focus" >&2
        exit 1
    fi
    if printf '%s\n' "$focus" | grep -q 'com.android.settings/'; then
        echo "EMULATOR_HEALTH_PASS: Android Settings received focus"
        timeout 10 adb shell input keyevent KEYCODE_HOME || true
        exit 0
    fi
    attempt=$((attempt + 1))
    sleep 2
done

echo "EMULATOR_HEALTH_FAIL: Android Settings did not receive focus: $focus" >&2
exit 1
