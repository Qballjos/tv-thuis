#!/usr/bin/env python3
"""Set the APK version above the latest published release, without committing generated changes."""
import pathlib
import re
import sys

path = pathlib.Path(sys.argv[1])
text = path.read_text()
current = re.search(r'android:versionName="(\d+\.\d+\.\d+)"', text)
code = re.search(r'android:versionCode="(\d+)"', text)
latest = re.fullmatch(r'v?(\d+)\.(\d+)\.(\d+)', sys.argv[2])
if not current or not code or not latest:
    sys.exit('Expected a three-part release version and an Android version code.')
source = tuple(map(int, current[1].split('.')))
published = tuple(map(int, latest.groups()))
version = max(source, (published[0], published[1], published[2] + 1))
major, minor, patch = version
if minor >= 100 or patch >= 10000:
    sys.exit('Version exceeds the Android version code allocation.')
# Do not add one to an already bumped source code: that would consume the next patch's code too.
number = major * 1000000 + minor * 10000 + patch
if number < int(code[1]):
    sys.exit('Source version code exceeds its release version: raise versionName before publishing.')
if number > 2100000000:
    sys.exit('Android version code limit exceeded.')
name = '.'.join(map(str, version))
text = text[:code.start(1)] + str(number) + text[code.end(1):]
text = re.sub(r'android:versionName="[^"]+"', 'android:versionName="' + name + '"', text, count=1)
path.write_text(text)
print(name)
