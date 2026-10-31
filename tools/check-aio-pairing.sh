#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
AJ="$(ls -d "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
javac -encoding UTF-8 -cp "out/classes:$AJ" -d "$classes" tools/AioPairingTest.java
java -ea -cp "$classes:out/classes:$AJ" io.freemytv.AioPairingTest
