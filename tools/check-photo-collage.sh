#!/usr/bin/env bash
# Host checks after build.sh; optional graphics checks target only an explicitly named disposable emulator.
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
AJ="$(ls -d "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)"
BT="$SDK/build-tools/$(ls "$SDK/build-tools" | sort -V | tail -1)"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
javac -encoding UTF-8 -cp "out/classes:$AJ" -d "$test_dir" tools/PhotoCollageTest.java
java -ea -cp "$test_dir:out/classes:$AJ" io.freemytv.PhotoCollageTest
if [ "$#" -eq 0 ]; then exit 0; fi
case "$1" in emulator-*) ;; *) echo 'Graphics checks require a disposable emulator, never a TV.' >&2; exit 2 ;; esac
mkdir "$test_dir/dex"
"$BT/d8" --lib "$AJ" --classpath out/classes --output "$test_dir/dex" "$test_dir/io/freemytv/PhotoCollageTest.class"
(cd "$test_dir/dex" && zip -q "$test_dir/runner.jar" classes.dex)
remote="/data/local/tmp/tv-thuis-collage-test"
adb -s "$1" shell mkdir -p "$remote"
adb -s "$1" push FreeMyTV.apk "$remote/app.apk" >/dev/null
adb -s "$1" push "$test_dir/runner.jar" "$remote/runner.jar" >/dev/null
adb -s "$1" shell "CLASSPATH=$remote/app.apk:$remote/runner.jar app_process /system/bin io.freemytv.PhotoCollageTest $remote"
echo "Graphics fixtures are in $remote on $1."
