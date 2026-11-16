# TV Thuis: guide for AI assistants

## Project development

Read these in order:

1. [PROJECT_SETUP.md](PROJECT_SETUP.md): workflow, skill selection, checks and release policy.
2. [fork/DEVELOPMENT.md](fork/DEVELOPMENT.md): household feature rules and signing identity.
3. [CLAUDE.md](CLAUDE.md): upstream architecture, compatibility and TV safety guidance.

Household rules take precedence over upstream release/build instructions for this fork. User-facing
household documentation is [FORK.md](FORK.md); upstream maintenance is described in
[fork/UPSTREAM.md](fork/UPSTREAM.md).

Keep AIO logic in `AioRecommendations`, `AioSettings` and `web/fork-aio.js`, and screensaver policy in
`SaverSources`. Preserve the small integration hooks in shared screens. Put household strings in
`app/assets/fork-i18n/`, package identity/version in `fork/package.json`, and Android additions in
`fork/android-manifest.xml`. Keep upstream catalogues, README, CLAUDE and package templates upstream-owned.

Use the skills selected by PROJECT_SETUP.md when available; report unavailable requested skills rather
than inventing their behavior. Run `tools/check.sh` before pushing, and validate changed GitHub workflows
with actionlint. Upstream candidates must pass checks before publication; never force-push main or bypass
a failed/conflicted candidate. Do not commit keys, credentials or generated build files.

## Installing or running on a TV

Follow [FORK.md](FORK.md) for household features and [CLAUDE.md](CLAUDE.md) for the TV's installation steps.
Most people ask in plain words and do not know what a terminal is: explain each step simply, and ask
before installation actions that change their TV or computer. Let the person perform the TV's taps and
mode switch. Keep Original mode and the watchdog working.

That installation approval rule does not apply to routine project development requested by the owner.
