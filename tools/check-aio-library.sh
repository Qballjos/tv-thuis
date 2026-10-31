#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
AJ="$(ls -d "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
# Recs reads the display width at class load; graphics are outside this page-selection test.
cat > "$classes/Backdrop.java" <<'JAVA'
package io.freemytv;
final class Backdrop { static final int W = 1920; }
JAVA
javac -encoding UTF-8 -cp "out/classes:$AJ" -d "$classes" "$classes/Backdrop.java" tools/AioLibraryTest.java
java -ea -cp "$classes:out/classes:$AJ" io.freemytv.AioLibraryTest
