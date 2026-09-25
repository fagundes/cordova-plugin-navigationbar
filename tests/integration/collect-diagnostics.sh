#!/bin/sh
# Best effort: never replace the original test failure with a collection error.
set -u

output_directory=$1
mkdir -p "$output_directory" || exit 0

capture() {
    output_name=$1
    shift
    timeout 15 "$@" >"$output_directory/$output_name.txt" 2>&1 || true
}

capture devices adb devices -l
capture properties adb shell getprop
capture logcat adb logcat -b all -d -v threadtime
capture window adb shell dumpsys window
capture activity adb shell dumpsys activity activities
capture last-anr adb shell dumpsys activity lastanr
capture system-anr adb shell dumpsys dropbox --print system_app_anr
capture app-anr adb shell dumpsys dropbox --print data_app_anr
capture webview adb shell dumpsys webviewupdate
capture cpu adb shell dumpsys cpuinfo
capture memory adb shell dumpsys meminfo
capture host-memory free -m
capture host-disk df -h
capture host-cpu lscpu
if [ -n "${ANDROID_HOME:-}" ]; then
    capture emulator-version "$ANDROID_HOME/emulator/emulator" -version
    capture acceleration "$ANDROID_HOME/emulator/emulator" -accel-check
fi
timeout 15 adb exec-out screencap -p >"$output_directory/screen.png" 2>/dev/null || true
timeout 90 adb bugreport "$output_directory/bugreport.zip" \
    >"$output_directory/bugreport-command.txt" 2>&1 || true
echo "Emulator diagnostics saved at: $output_directory"
exit 0
