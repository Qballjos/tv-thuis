#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -encoding UTF-8 -d "$classes" \
  app/src/io/freemytv/ServiceNames.java tools/RecsServiceTest.java
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -ea -cp "$classes" io.freemytv.RecsServiceTest
