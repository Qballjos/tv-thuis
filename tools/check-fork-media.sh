#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
AJ="$(ls -d "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
cat > "$classes/Backdrop.java" <<'JAVA'
package io.freemytv;
final class Backdrop { static final int W = 1920; }
JAVA
# HomeApps constructs two component names at class load. The SDK stub throws on construction.
cat > "$classes/ComponentName.java" <<'JAVA'
package android.content;
public final class ComponentName { public ComponentName(String pkg, String name) {} }
JAVA
javac -encoding UTF-8 -cp "out/classes:$AJ" -d "$classes" "$classes/Backdrop.java" "$classes/ComponentName.java" tools/ForkMediaTest.java
java -ea -cp "$classes:out/classes:$AJ" io.freemytv.ForkMediaTest
