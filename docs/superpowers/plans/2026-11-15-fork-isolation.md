# Fork Isolation Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans for integration and superpowers:dispatching-parallel-agents for disjoint translation and Java module work.

**Goal:** Isolate fork features and verify upstream before merging.
**Architecture:** Fork modules + translation overlays + build metadata overlay, with small upstream hooks.
**Tech Stack:** Java 8 Android Canvas, ES5 web, Python standard library, Bash and GitHub Actions.
**Spec:** docs/superpowers/specs/2026-11-15-fork-isolation-design.md

## Global constraints

Preserve every existing feature, existing signing identity, and credentials; no TV interaction. Keep
minSdk 26 and Java 8; web ES5, no grid, variables or backdrop-filter. Do not rewrite existing commits.

## Review focus

Translations preserve every hand-edited value and fallback; signed update identity/version stay stable;
async profile/library source switches stay isolated; failed/conflicted candidates never change main;
upstream documentation and generated package feeds cannot overwrite this household's release settings.

## Tasks

- [ ] Split translations into unchanged upstream catalogues and fork overlays; test merged catalogue equality,
  English fallback, new upstream languages, writes/pruning and Android/web packaging before and after.
- [ ] Extract AIO recommendation and screensaver behavior into dedicated Java modules using small shared hooks;
  preserve and extend focused behavioral tests before refactoring.
- [ ] Separate fork web logic where feasible without introducing circular globals or modern browser dependencies.
- [ ] Move release/package identity to fork metadata applied in build staging; separate fork readme/technical notes;
  test staging, upstream version changes, generic builds and update certificate behavior.
- [ ] Replace direct upstream-to-main push with candidate preparation, complete checks, then guarded fast-forward
  only if main has not moved. Report conflicts without changing main. Test with disposable local Git repos.
- [ ] Integrate current upstream with the reduced overlap; resolve actual behavior conflicts, run all checks,
  request independent review, and prepare a reviewable commit. Publish only within authorized session scope.
