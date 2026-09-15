#!/usr/bin/env bash
# Preserve visual evidence even when an instrumentation assertion fails.
set -uo pipefail
result=0
./gradlew connectedDebugAndroidTest || result=$?
mkdir -p app/build/device-screenshots
adb pull /sdcard/Pictures/FrontierQA app/build/device-screenshots/ || true
exit "$result"
