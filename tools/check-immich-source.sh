#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
javac -encoding UTF-8 -d "$classes" app/src/io/freemytv/ImmichSource.java tools/ImmichSourceTest.java
java -ea -cp "$classes" io.freemytv.ImmichSourceTest
