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

- [x] Split translations into unchanged upstream catalogues and fork overlays; test merged catalogue equality,
  English fallback, new upstream languages, writes/pruning and Android/web packaging before and after.
- [x] Extract AIO recommendation and screensaver behavior into dedicated Java modules using small shared hooks;
  preserve and extend focused behavioral tests before refactoring.
- [x] Separate fork web logic where feasible without introducing circular globals or modern browser dependencies.
- [x] Move release/package identity to fork metadata applied in build staging; separate fork readme/technical notes;
  test staging, upstream version changes, generic builds and update certificate behavior.
- [x] Replace direct upstream-to-main push with candidate preparation, complete checks, then guarded fast-forward
  only if main has not moved. Report conflicts without changing main. Test with disposable local Git repos.
- [x] Integrate current upstream with the reduced overlap; resolve actual behavior conflicts, run all checks,
  request independent review, and prepare a reviewable commit. Publish only within authorized session scope.

## Execution evidence

Migration preserves all original 27 x 572 effective translations; permanent regressions pin only fork-owned
values and allow future upstream edits/new languages. Upstream0e33f168 was then integrated with source
catalogues, manifests, README and CLAUDE merging unchanged by household additions.

Modules now include AioRecommendations, SaverSources, AioSettings and web/fork-aio.js. Remaining shared hooks
are intentional: rendering, preference integration, artwork/cache fields and lifecycle calls still need review
when upstream changes their contracts. This is isolation, not a promise that future merges cannot conflict.

Candidate automation has nine executable disposable-Git tests, including a real push-time race that rejects both
refs atomically. Complete Android/web checks passed after integration; final controller extraction was reviewed
independently and recompiled. No TVs contacted. Release metadata is fork/package.json (1.3.13), applied only to
build outputs. Upstream documentation stays upstream; household documentation is FORK.md and fork/DEVELOPMENT.md.
