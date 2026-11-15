# TV Thuis: fork development rules

Read this after PROJECT_SETUP.md and before the upstream CLAUDE.md. These household-specific rules take precedence for this fork. See FORK.md for user documentation.

## Release identity and packaging

The household package is defined in fork/package.json. Never use upstream's signing certificate, release
repository, version or display name for TV Thuis builds. Android stages upstream's manifest plus
fork/android-manifest.xml into out/AndroidManifest.xml; it leaves upstream files untouched. webOS and Tizen
apply the same package metadata in memory. ForkBuild is generated from this metadata for Updates.

The APK asset stays FreeMyTV.apk on Qballjos/tv-thuis. Keep the existing certificate
94ea93a97b298f691f232eab656ec096d2d2701b4790b7e0308912632fd45138, signing secrets, package ID io.freemytv,
and installed preferences. Automatic version bumps write only the ephemeral build metadata, never a loop commit.
A local development APK is not a signed release and must never replace the published asset.

## Feature rules

- AIO pairing uses the existing temporary `KeyPage` QR form with a one-use AIO dashboard link. `AioPairing` validates
  the origin/link and saves a scoped device token atomically; legacy manual login remains available. Device credentials
  (`aioDeviceId` included) are shared across launcher profiles. Paired Continue Watching goes through AIO, with no
  Jellyfin key on the TV. `Aio.test` must never discard a paired token. Server administration is outside its scope.
  AIO home tabs are Resume, Recent, Movies (owned library), Shows (owned library), Trending (requestable picks).
  Library pages append by stable item ID as focus nears the end; never apply recommendation/franchise limits to them.
  Prepare only nearby card artwork and grow card springs/bounds outside drawing when pages append.
  Recent rows sort by parsed addition date (ISO or Unix, newest first), never hourly rotation or franchise limits.
  Resume refreshes separately every minute. Older AIO poster-only trending rows may use the existing TMDB key for
  landscape art; failed or undecodable landscape downloads must fall back to the poster.
  `AioHttp` keeps HTTPS certificate checks and disables credential redirects. Runtime plain HTTP is pinned to private
  IPv4 addresses at the socket boundary; never relax global `network_security_config` for arbitrary hosts.


- `ImmichPhotos` (with `immich_server` and the key sent to that TV; without the key My Photos is unchanged): every 6
  hours, on the home screen's resume or while the screensaver is showing, a random choice from the server (`POST /api/search/random`, or one album,
  `Prefs.photoSource`) is downloaded one photo at a time into `Photos.privateInbox` and imported right away, so at most
  one original is on disk (Immich's preview instead on low-RAM and 720p TVs). Nothing goes before something new came:
  at `Photos.MAX` the oldest of Immich's make room (`-immich.jpg` names). `ImmichSource` snapshots the server/album for
  the query, photo filename tag and refresh stamp. `Photos.files` exposes only that selection (manual photos only
  outside album mode); untagged legacy caches stay hidden. A stale sync cannot mark a new album fresh or re-enable
  photos. `tools/photos.py`'s are never deleted. Redirects are never followed (`x-api-key` would go
  along to another host).
- Screensaver's `Screensaver source` is per profile (`auto`, `aio`, `immich`): explicit sources never fall back to
  other sources; empty/loading sources use the clock. Immich's collage uses the same filtered collection.
  `Photo layout` is per profile: fullscreen stays the default; own photos take priority over AIO art.
  An empty/loading selected album uses the clock fallback, never other photos or movie art.
  `PhotoCollage` prepares up to three framed photos on the worker; the existing slideshow handles fades and keys.
  Imports retain the full aspect ratio and capture date only (no GPS); older cropped copies still work.

## Modules and translations

AioRecommendations and SaverSources own fork recommendation and screensaver policies. web/fork-aio.js
owns the equivalent web media behavior. Preserve the narrow hooks in shared core files; add new feature
logic to these modules rather than expanding upstream screens.

Upstream translations stay in app/assets/i18n. Household text/overrides stay in app/assets/fork-i18n.
Android and Python/web builds merge English fallbacks and selected-language overlays. Use tools/translate.py;
do not write household strings into upstream catalogues. Preserve hand-edited translations and placeholders.

Profiles remain local launcher profiles, not parental control. Device credentials and Immich server settings
stay shared, while shelf/content/photo selections stay per profile. Queued work must retain originating identity.

## Upstream updates

Prepare a candidate branch, run the complete checks, and only then advance main if its head has not moved.
Conflicts and failed checks must leave main and releases unchanged. Use the conflict artifact to resolve an
actual behavior collision; never blanket-select a side, force-push main, or mark an untested merge successful.
