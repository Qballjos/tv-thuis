# Security policy

## Supported versions

Security fixes go on the latest release of this fork (`FreeMyTV.apk` on [Releases](https://github.com/Qballjos/tv-thuis/releases)). Older APKs on the TVs should update via **Home Settings → Updates**.

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Use [GitHub private vulnerability reporting](https://github.com/Qballjos/tv-thuis/security/advisories/new) on this repository (Security → Advisories → Report a vulnerability). Describe the issue, the affected version, and steps to reproduce when you can.

You can expect an acknowledgement when the report is seen. Fixes are released as a new version when needed.

## What not to put in this repository

Keep these out of git and out of public chat:

- `config.properties` (already gitignored)
- `*.keystore` and signing passwords
- TMDB, Watchmode, Jellyfin, Plex, Immich, Home Assistant, and AIO credentials
- Home network addresses, hostnames, and personal details in docs

Local config on your machine and settings typed on the TV are fine. The published documentation must not include those secrets or home addresses.
