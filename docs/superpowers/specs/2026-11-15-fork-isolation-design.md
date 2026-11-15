# Fork isolation

User-approved direction: preserve all current AIO/profile/screensaver/clock behavior while reducing conflicts
with Free My TV. Keep fork features in dedicated modules with small integration points; maintain fork-owned
translation overlays and release metadata; retain upstream documentation separately. Upstream automation
must build and test a candidate branch before main changes. A conflict must leave main and releases intact,
retain a useful conflict report, and avoid repeatedly creating unrelated branches.

No force-pushes to main, credential migration, TV operations, or feature removals. Preserve minSdk 26,
Java 8, ES5 web compatibility, profile isolation, AIO network restrictions, TV update signing identity,
and existing Immich migration behavior. Existing signed releases and installed preferences remain valid.
A historical upstream baseline is necessary to split files safely; use the last upstream ancestor
26d8cb7c4f8c72a1548cd502540b3d8222175193, then integrate current upstream main.

This reduces recurring merge conflicts but cannot guarantee compatibility with arbitrary future upstream
changes. Integration hooks and behavior tests remain maintained parts of the fork.
