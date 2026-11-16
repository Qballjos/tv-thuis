#!/usr/bin/env bash
# Household behavior checks, called once after upstream's own checklist.
set -euo pipefail
cd "$(dirname "$0")/.."
case "${1:-all}" in
  all|android)
    sdk_path="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
    build_tools="$(ls "$sdk_path/build-tools" | sort -V | tail -1)"
    python3 tools/check-packaged-manifest.py FreeMyTV.apk "$sdk_path/build-tools/$build_tools/aapt"
    bash tools/test-recent.sh
    bash tools/check-profiles.sh
    bash tools/check-watch-next.sh
    bash tools/check-photo-collage.sh
    bash tools/check-immich-source.sh
    bash tools/check-key-pages.sh
    bash tools/check-aio-pairing.sh
    bash tools/check-aio-library.sh
    bash tools/check-fork-media.sh
    bash tools/check-recs-services.sh
    bash tools/check-media-order.sh
    ;;
esac
case "${1:-all}" in
  all|web)
    python3 tools/test-release-version.py
    python3 tests/test_fork_package.py
    python3 tests/test_translation_overlays.py
    python3 tests/test_upstream_sync.py
    node tools/test-aio-library.js
    node tools/test-artwork.js
    node tests/RegionalClockTest.js
    ;;
esac
