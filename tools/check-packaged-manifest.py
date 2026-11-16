#!/usr/bin/env python3
"""Check component placement in the compiled APK, not just the source manifest."""
import re
import subprocess
import sys

run = subprocess.run([sys.argv[2], 'dump', 'xmltree', sys.argv[1], 'AndroidManifest.xml'],
                     capture_output=True, text=True, check=True)
applications = []
activity_depth = None
profile_in_application = False
for line in run.stdout.splitlines():
    element = re.match(r'^(\s*)E: (\S+)', line)
    if element:
        depth, tag = len(element[1]), element[2]
        if tag == 'application':
            applications.append(depth)
        activity_depth = depth if tag == 'activity' else None
    if 'A: android:name(' in line and '".ProfileManagerActivity"' in line:
        profile_in_application = len(applications) == 1 and activity_depth == applications[0] + 2
if len(applications) != 1 or not profile_in_application:
    sys.exit('APK must declare ProfileManagerActivity directly inside its sole application block.')
print('Packaged manifest: one application, profile activity registered inside it.')
