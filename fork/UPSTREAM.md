# Maintaining the upstream boundary

This fork follows [Free My TV](https://github.com/simbasjungles/free-my-tv) while keeping household media
features separate. The boundary reduces routine conflicts; shared APIs and screen integration hooks can
still need manual updates when upstream changes their contracts.

## Where changes belong

| Concern | Household-owned files | Shared integration |
| --- | --- | --- |
| AIO paging, filtering, ordering and source identity | `app/src/io/freemytv/AioRecommendations.java` | `Recs.java` |
| AIO settings, connection testing, search and request dialogs | `app/src/io/freemytv/AioSettings.java` | `SettingsActivity.java` |
| Screensaver source selection and AIO launching | `app/src/io/freemytv/SaverSources.java` | `DreamView.java` |
| Collage rendering and Immich source identity | `PhotoCollage.java`, `ImmichSource.java` | Photo import and screensaver lifecycle |
| Web AIO transport, artwork, rails and paging | `web/fork-aio.js` | Callback adapter in `web/data.js`; script loaded before it |
| Household translations | `app/assets/fork-i18n/*.json` | Android loader in `Ui.java`; Python/web catalogue merging |
| Display name, release version, repository and certificate | `fork/package.json`, `tools/fork_package.py` | Build staging; generated `ForkBuild.java` |
| Additional Android components | `fork/android-manifest.xml` | Merged into `out/AndroidManifest.xml` during build |
| Household regression checks | `tools/check-fork.sh` | Called after upstream's `tools/check.sh` checks |
| Household documentation | `FORK.md`, `PROJECT_SETUP.md`, `fork/` | `AGENTS.md` routes assistants to both guides |

The upstream `README.md`, `CLAUDE.md`, `app/assets/i18n/`, Android manifest, resource strings and LG/Samsung
package templates should follow upstream. Do not reapply household names, versions or translations directly
to those source files. Android staging preserves new upstream components while adding household ones; webOS
and Tizen apply package metadata in memory. Do not edit generated files under `out/`.

## Translation maintenance

Run `python3 tools/translate.py --check` to validate the effective catalogues. The merge order is upstream
English, household English, selected upstream language, then selected household language. This gives newly
introduced upstream languages English household text until a translation is supplied. Android continues to
exclude unsupported RTL languages; the web app supports upstream's RTL behavior.

Use the translation tool to extract, fill or prune household overlays. Preserve hand translations and
placeholders. Optional `--fill` sends missing strings to the configured translation service; it is not part
of ordinary builds or checks. Upstream catalogue edits must remain visible unless the household explicitly
overrides that key. Do not pin upstream wording in household regression snapshots.

## How the Upstream workflow works

1. Fetch the current main and upstream main, and record their exact commit IDs. If upstream is already
   included, finish successfully without creating a candidate or publishing anything.
2. Prepare a local `codex/upstream-<upstream-sha-prefix>` candidate from main and merge upstream into it.
   Preparation does not push a remote branch. A conflict aborts the merge and retains a diagnostic report.
3. Validate workflow syntax with pinned actionlint, then run the complete Android/web/household checks.
   Checkout does not persist credentials; publication credentials are configured only after these checks pass.
4. Confirm the working candidate still matches the recorded tested commit and origin/main has not moved.
   Atomically push the candidate and main using ordinary fast-forward rules. A rejected update changes neither ref.
5. Explicitly dispatch Check on main. Its successful completion makes the commit eligible for a signed release.

The sync state lives in the runner's temporary directory because Android builds replace `out/`. A failure
uploads `upstream-conflict-<run-id>` with available state and conflict details; artifacts are retained for 30 days.
Build failures can have state without a merge-conflict report. A conflict, failed check or main race must never
be converted into a successful publication by selecting a side wholesale or using a force push.

## Investigating a failed run

Start with read-only inspection:

```sh
gh run list --workflow upstream.yml --limit 5
gh run view <run-id> --log-failed
gh run download <run-id> --name upstream-conflict-<run-id> --dir <temporary-folder>
```

A failed merge needs a reviewed integration change. A main-race rejection needs a new candidate based on the
new main, with checks rerun. A build or workflow-validation error needs its underlying failure fixed before
publication. Do not repeatedly rerun a deterministic conflict.

When authorized to run the sync, use `gh workflow run upstream.yml --ref main`. This is a publishing action:
a successful candidate can update main and lead to a signed release. It is not a read-only diagnostic.
For local experiments, use a clean disposable clone and keep state outside `out/`. The separate `publish`
command is intended for the tested workflow, not as a shortcut around checks.

## Verification and releases

`tools/check.sh` runs upstream checks and then household checks. Its `android` and `web` modes help diagnose
failures; run the full command before pushing. GitHub Check also validates workflows with actionlint. Use the
same pinned actionlint version shown in `.github/workflows/check.yml` when validating workflows locally.

The Release workflow uses the checked main commit, updates the version in an ephemeral copy of
`fork/package.json`, and generates a higher Android version code. It does not rewrite source manifests or
commit a version-bump loop. The APK is signed with the existing household key and verified against the public
certificate in package metadata. Never substitute an upstream or development key.

Do not rerun historical release jobs as a way to repair current main. The duplicate-release guard checks the
latest published commit; a historical rerun is not a general rollback mechanism. Verify the resulting release
commit, `FreeMyTV.apk` asset, and final workflow result before claiming an update is available. Android release
updates are separate from locally built LG/Samsung packages and upstream Homebrew/TizenBrew distributions.
