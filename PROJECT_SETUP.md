# Project development setup

## Scope and authorization

Use this guide for development and maintenance of Qballjos/tv-thuis. Read CLAUDE.md for architecture,
compatibility requirements, security rules, and TV-specific behavior. Its installation walkthroughs apply
when installing or operating a TV, not when managing this repository.

A request to implement or fix something authorizes the routine repository work needed to prepare and
verify the change: edit files, use a development branch, resolve merge conflicts, and run checks.
Do not ask for permission again for those steps. Ask when a required decision is missing or an action
goes beyond the request. Do not install on, connect to, or change a TV as part of repository verification
unless the user requests it. Never expose credentials or commit config.properties, keystores, or keys.

## Project map

- app/: Android TV and Google TV app, Java 8 language level; build.sh builds without Gradle.
- web/: shared ES5 web app for LG and Samsung; preserve support for older TV browsers.
- webos/ and tizen/: packaging and device installation tools.
- docs/: published website; keep English and Portuguese pages in step when changing their content.
- tools/ and tests/: validation scripts and regression checks.
- .github/workflows/: checks, website publishing, releases, and scheduled upstream merges.

## Skills to use

Use available skills by their names below; read their SKILL.md before applying them. Installation paths
are machine-specific and must not be hardcoded here. If a skill is unavailable, say so and follow the
equivalent repository workflow. Select skills for the actual task; do not invoke every skill for every change.

Project skill preferences also include **Ponytail Full** and **Caveman Lite**. Discover their installed
skill names and read their SKILL.md files before using them; follow their documented scope and triggers.
These two skills were not available in the session that added this guidance, so their exact identifiers
and behavior still need verification. Do not invent instructions for them or claim they are installed.

| Task | Skills |
| --- | --- |
| Start a session and select workflows | superpowers:using-superpowers |
| Look up current library or framework documentation and API examples | Context7 connected tools |
| Diagnose failures, bugs, or failed GitHub Actions | superpowers:systematic-debugging |
| Design a new feature or change behavior | superpowers:brainstorming |
| Plan a substantial change with agreed requirements | superpowers:writing-plans |
| Implement a feature or bug fix | superpowers:test-driven-development |
| Execute an agreed implementation plan | superpowers:executing-plans |
| Work that needs an isolated checkout | superpowers:using-git-worktrees |
| Improve app or website UI | impeccable:impeccable |
| Review substantial changes | superpowers:requesting-code-review |
| Assess and apply review feedback | superpowers:receiving-code-review |
| Verify before claiming completion | superpowers:verification-before-completion |
| Finish and integrate a tested development branch | superpowers:finishing-a-development-branch |

Use Context7 to verify library or framework APIs against current documentation when relevant to the task.
Use Impeccable for frontend design, UX review, accessibility, responsive behavior, and visual polish.

For small documentation or configuration edits, keep the workflow proportional. Use additional platform
skills only when the task involves that platform. This project is not a Gradle, React, or Vercel app.
Delegate to subagents only when the user requests it or an applicable skill explicitly requires it.

## Development workflow

1. Inspect git status, the current branch, remotes, and relevant files before editing. Preserve existing work.
2. Reuse a suitable checkout. Use the codex/ prefix for new branches unless the user specifies another name.
3. For GitHub Actions failures, inspect the failed run and job logs before changing code. Prefer connected
   GitHub tools; use Git or gh when the connector lacks a needed operation. Report network or access failures.
4. For upstream merges, preserve this fork's AIO, profiles, photo integrations, and update source/signing
   requirements. Resolve conflicts by understanding both sides; never blanket-select one side or force-push main.
   Keep the upstream workflow's protection against publishing unresolved merges.
5. Make the smallest coherent change. Follow CLAUDE.md's Android and old-browser constraints. Keep translations
   valid and preserve hand translations when merging catalogues.
6. Run tools/check.sh before pushing. Its android and web modes can help investigate failures, but do not
   replace the complete pre-push check. Android builds must end with Built FreeMyTV.apk. Check prerequisites
   first; report missing tools rather than silently installing system dependencies.
7. Review the final diff and report the change, checks actually run, and anything still blocked. A local fix
   is not a successful GitHub run: verify the relevant remote checks after publishing an authorized change.

Pushing to main can publish the website and, after successful checks, create a release. Treat publishing,
merging, and release operations according to the user's requested scope; do not assume that permission to
prepare a fix also requests a release.
