# AIO clock and date

Add a compact glass panel in place of the AIO heading on Android and the shared LG/Samsung web app.
The user refined the chosen compact panel to replace the AIO heading entirely, keeping the tabs and
media cards clear. Reuse the existing glass rendering, typography, light/dark palettes and localized
clock/date formatting. Show time above a smaller date. Preserve the TV region independently of the UI
language, use regional date order and automatic hour cycle, and respect explicit 12/24-hour overrides.

The panel is informational, without a remote-focus stop. Update on minute changes and language/format
changes, using cached text outside drawing. Bound its width so long localized dates cannot overlap the
heading, top buttons, sleep timer or five tabs. Preserve the existing non-AIO clock view.

Keep Java 8 / minSdk 26 support and the lighter rendering fallback; web code remains ES5 without CSS
variables, grid or backdrop-filter. Verify both themes in a browser preview and run tools/check.sh.
No TV installation or GitHub publication is part of this UI change.
