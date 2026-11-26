# CloudStream integration audit: HUSH addons vs NuvioMobile-Enhanced

- **Date:** 2026-11-26, 05:37 UTC
- **Role:** CloudStream Integration (Phase 1, read-only audit)
- **HUSH commit audited:** `887cdea` on branch `fix/root-theme-wrap`
- **Reference commit audited:** NuvioMobile-Enhanced `7aca5a018` (task text said `7aca5a0`; the full hash is used here)
- **Scope:** Stremio-protocol addon path in HUSH (`core/data/.../addon/`) compared with Nuvio's addon and stream-source handling, plus what CloudStream compatibility would require. No code was edited, committed or pushed. This file is the only file written.

## Legend

- **TAKE:** adopt the behaviour as a requirement; implement with HUSH's own code. No code is copied.
- **ADAPT:** adopt the idea, changed to fit HUSH's model or security posture.
- **ALREADY IMPLEMENTED:** HUSH already does this, or does it more strictly.
- **SKIP:** not for now, with reason.
- **NEEDS INVESTIGATION:** cannot be settled from the repos on disk, or needs an owner decision.

Verification tags: **[V]** read in source this session. **[C]** carried from existing HUSH docs (`docs/compatibility/EXTENSION_ARCHITECTURE.md`, `docs/reverse-engineering/LICENSING_AUDIT.md`); the CloudStream reference repo is not on disk this session, so these were not re-read. **[NT]** not tested at run time.

## 1. HUSH side (current state)

| Area | File | Current behaviour [V] |
|---|---|---|
| HTTP client | `core/data/src/main/kotlin/app/hush/placeholder/core/data/addon/StremioClient.kt` | OkHttp 4.12.0. Requires `https://`. Reads at most 2,000,000 bytes, JSON only. Builds `catalog`, `meta`, `stream` paths. |
| Repository | `.../addon/AddonMediaRepository.kt` | Fan-out over installed addons for featured and search, with per-addon `runCatching`. Max 40 featured items, max 4 catalogs per addon. Stream list keeps only `https://` URLs with no torrent support. |
| Registry | `.../addon/PrefsAddonRegistry.kt` | Device-local SharedPreferences list (`hush_addons`). `add` fetches the manifest first. `normalise` requires https and appends `/manifest.json`. |
| Item id | `.../addon/ItemKey.kt` | Base64url of base URL, type and meta id. Only `https://` bases decode. |
| Domain | `core/domain/.../addon/` and `.../model/MediaModels.kt` | `InstalledAddon(manifestUrl, name, description)`. `StreamSource(url, label)` only. |

## 2. Nuvio side (reference state)

| Area | File (under `composeApp/src/commonMain/kotlin/com/nuvio/app/features/`) | Behaviour [V] |
|---|---|---|
| Manifest model | `addons/AddonModels.kt` | Keeps `resources`, `types`, `idPrefixes`, catalog `extra` with `isRequired` and `options`, `behaviorHints` (`configurable`, `configurationRequired`, `adult`, `p2p`). `isTrackingAddon` is true when a resource is `player` or `library`. |
| Manifest parser | `addons/AddonManifestParser.kt` | Requires `id` and `name`. Resolves relative `logo` against the manifest URL. Ignores unknown keys. |
| URL building | `addons/AddonTransportUrls.kt` | Strips query from the base for the path, re-attaches the manifest query to resource URLs. Encodes ids to RFC 3986 unreserved set. |
| Repository | `addons/AddonRepository.kt` | `normalizeManifestUrl` accepts `http://` and `https://`. Install flow returns `TrackingConsentRequired` for tracking addons. Enable/disable and reorder. Server sync through Supabase (`pullFromServer`, `sync_push_addons`). |
| Consent UI | `addons/TrackingAddonConsentDialog.kt` | Checkbox consent before installing a tracking addon. |
| Stream parsing | `streams/StreamParser.kt` | Parses `url`, `infoHash`, `fileIdx`, `externalUrl`, `ytId`, `sources`, `behaviorHints` (including proxy headers and binge group), `clientResolve`. |
| Stream loading | `streams/StreamSourceLoader.kt` | `loadStreams` fans out per source with `withTimeoutOrNull(timeoutMs)`; late addons are dropped, not awaited. |
| Plugin runtime (separate system) | `plugins/runtime/` (`androidFull`, `fullCommonMain`) | JavaScript scrapers in QuickJS, with a fetch bridge (`FetchBridge.kt`) and an evaluation timeout (`PluginRuntime.android.kt`). Not a Stremio addon path. |

## 3. Licence facts verified this session

- **[V]** HUSH `LICENSE` (674 lines) begins "GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007".
- **[V]** Nuvio `LICENSE` begins with the same GPL-3.0 header. Nuvio `README.md` says "GPL-3.0, same as upstream" (line 277).
- **[C]** CloudStream `LICENSE` is GPL-3.0 (`LICENSING_AUDIT.md` §1). Not re-read this session.
- **[C]** Per-file CloudStream headers are sparse; copyright holder is not named in its LICENSE (the FSF template). Not re-read.
- **[C]** NiceHttp (GPL-3.0), NewPipe Extractor (GPL-3.0), nextlib (GPL-3.0), Rhino (MPL-2.0), juniversalchardet (tri-licence), jsoup (MIT), Jackson (Apache-2.0). Dependency licences from upstream repos, not from Maven POMs. Not re-read.
- **Not verified:** the licence of any community extension in the CloudStream extensions repository. Per `LICENSING_AUDIT.md` §2 this is [NV].

Note for the owner: the licence facts above are recorded, not interpreted. Whether any of them constrains HUSH is a question for the owner and counsel.

## 4. Item classification

### 4.1 TAKE (adopt the behaviour, write our own code)

| ID | Item | Nuvio reference | HUSH location | Reason |
|---|---|---|---|---|
| T1 | Consent gate before installing addons whose manifest declares `player` or `library` resources | `addons/AddonModels.kt:38-41`, `addons/TrackingAddonConsentDialog.kt`, `addons/AddonRepository.kt:271-313` | `PrefsAddonRegistry.kt` `add()` has no gate | These addons can receive watch data. HUSH has no privacy gate, and its no-accounts stance makes the consent step more important. |
| T2 | Respect manifest `resources` before calling `stream` or `meta` | `addons/AddonManifestParser.kt` `resources()` | `AddonMediaRepository.kt` calls `stream` for every addon; `StremioClient.kt` has no resource check | Avoids avoidable errors and noise from catalog-only addons. Errors are currently swallowed, so the failure is silent. |
| T3 | Per-addon enable/disable and ordering | `addons/AddonRepository.kt` `setAddonEnabled`, `moveAddon` | `PrefsAddonRegistry.kt` offers only `add` and `remove` | Provides the kill switch recommended in `EXTENSION_ARCHITECTURE.md` §5.2(7) without deleting data. |
| T4 | Handle `behaviorHints.configurationRequired` and `configurable` | `addons/AddonModels.kt:59`, `addons/AddonsScreen.kt:157` | Not parsed | Addons that need configuration fail with no explanation. Show a state instead of an empty list. |

### 4.2 ADAPT (adopt with changes to HUSH's model or posture)

| ID | Item | Nuvio reference | HUSH location | Reason |
|---|---|---|---|---|
| A1 | Keep the manifest query string; derive the base and resource URLs from it | `addons/AddonTransportUrls.kt` | `PrefsAddonRegistry.kt` `normalise` and `InstalledAddon.base`. A manifest URL with `?query` becomes `...?query/manifest.json`, which is broken | Some addons carry configuration in a query string. HUSH currently breaks them. Keep https-only. |
| A2 | Honour catalog `extra` with `isRequired` and `options` | `addons/AddonManifestParser.kt` `catalogs()`, `addons/AddonModels.kt:43-52` | `AddonMediaRepository.kt` `catalogItems` ignores `extra` except for search | A catalog with a required extra fails silently today. Add a model field and skip with a reason. |
| A3 | Fetch addons in parallel with a total timeout; drop late results | `streams/StreamSourceLoader.kt:252-323` | `AddonMediaRepository.kt` `featured()` and `search()` use a serial `flatMap` | One slow addon blocks the whole list. HUSH only has OkHttp read timeouts (15 s per call). Use a structured `async` fan-out with `withTimeoutOrNull`. |
| A4 | Do not swallow `CancellationException` | Not a Nuvio item; Nuvio uses `runCatchingUnlessCancelled` (`streams/StreamFetchSupport.kt:76`) | `AddonMediaRepository.kt` `safeCatalog`, `details`, `streams`; `PrefsAddonRegistry.kt` `add` all use `runCatching`, which catches cancellation | Pre-existing HUSH bug. Cancelled searches keep running and can return stale results. Use a helper that rethrows cancellation. |
| A5 | Widen `StreamSource` to carry headers, referer, quality and stream type | `streams/StreamParser.kt:105-117` (`proxyHeaders`); CloudStream `ExtractorLink` shape `EXTENSION_ARCHITECTURE.md` §3.3 [C] | `core/domain/.../model/MediaModels.kt:10` `StreamSource(url, label)` | Many HLS and DASH streams need headers or a referer. Without this, playback fails for those addons, and CloudStream parity is blocked. Do this before any CloudStream adapter. |
| A6 | Parse `behaviorHints.bingeGroup` and `notWebReady` | `streams/StreamParser.kt:38-60` | Dropped in `AddonMediaRepository.kt` `streams()` | `bingeGroup` drives auto-next across episodes. Parse now; use when the player work lands (Playback role). |
| A7 | Accept `externalUrl` streams and open them outside the app | `streams/StreamParser.kt:29` | Dropped (only `url` with https is kept) | Lower risk than embedding. Needs a UI decision: open in browser or hand off to an external player. |
| A8 | Serialise registry writes | Nuvio uses an in-memory lock (`addons/AddonRepository.kt`, `SynchronizedObject`) | `PrefsAddonRegistry.kt` `add` and `remove` do read-modify-write on SharedPreferences with `apply()` and no lock | Two concurrent adds or an add and a remove can lose an entry. Use a `Mutex`. |
| A9 | De-duplicate installs by manifest `id`, not only by URL | `addons/AddonManifestParser.kt` requires `id` | `PrefsAddonRegistry.kt` de-duplicates only by `manifestUrl` | The same addon at two URLs (e.g. with and without a trailing slash, or different config) shows twice and doubles requests. |

### 4.3 ALREADY IMPLEMENTED

| ID | Item | HUSH location | Notes |
|---|---|---|---|
| I1 | HTTPS-only manifest and resource fetches | `StremioClient.kt` `getJson` (`require`), `PrefsAddonRegistry.kt` `normalise` | Stricter than Nuvio, which accepts `http://` (`AddonRepository.kt` `normalizeManifestUrl`). Matches the security posture in `EXTENSION_ARCHITECTURE.md` §5.2(1). Trade-off: see risk 2. |
| I2 | Bounded JSON read (2 MB) | `StremioClient.kt` `MAX_BYTES` and `request(MAX_BYTES + 1)` | Checked logic: `request` returns true only when more than the limit is available, so `readUtf8` is bounded. Nuvio's equivalent was not verified. [NT] |
| I3 | Per-addon failure isolation (ADR-018) | `AddonMediaRepository.kt` `safeCatalog` | One failing addon does not empty the others. The cancellation issue is A4. |
| I4 | Search capability detection from `extra` and legacy `extraSupported` | `AddonMediaRepository.kt` `supportsSearch` | Handles both shapes. |
| I5 | Stream filter: https only, torrent and infoHash excluded | `AddonMediaRepository.kt` `streams()` | Deliberate; see S1. |
| I6 | Series episodes from `videos[]` with season and number sort; single item for movies | `AddonMediaRepository.kt` `toDetails` | Matches the Stremio meta model. |
| I7 | Manifest fetched and validated before an addon is saved | `PrefsAddonRegistry.kt` `add` | Nuvio does the same (`AddonRepository.kt` `addAddon`). |
| I8 | Id encoding for path segments | `StremioClient.kt` `encodeSegment` | Keeps `:` unencoded (Stremio ids such as `tt123:1:2`). Nuvio encodes `:` to `%3A`. Both work for normal ids. [NT] |
| I9 | Demo fallback is flagged and separate from addon ids | `ItemKey.kt` (demo ids never decode), `MediaItem.isDemo` | Matches CLAUDE.md rule 4. |

### 4.4 SKIP

| ID | Item | Reason |
|---|---|---|
| S1 | `infoHash` and torrent streams (Nuvio `StreamParser.kt:28`) | Peer-to-peer exposure and a separate security analysis. Not in scope for the current milestone. Reopen only with an owner decision. |
| S2 | Loading CloudStream `.cs3` dex archives in the app process | Full app privilege and no isolation (`EXTENSION_ARCHITECTURE.md` §5.1). Blocked until a security analysis and an isolation design are approved. |
| S3 | Copying any CloudStream or Nuvio source file | Rejected in `LICENSING_AUDIT.md` (option "Copy CloudStream Kotlin source"). Independence requirement (handover §0). |
| S4 | Porting Nuvio's QuickJS plugin runtime (`plugins/runtime/`) | Different extension model, its own GPL-3 obligations, and the fetch bridge needs its own host-restriction review. Reference only. |
| S5 | Accepting `http://` manifest URLs (Nuvio behaviour) | Conflicts with HUSH's https-only rule. Revisit only if risk 2 is resolved by owner decision. |
| S6 | Manifest `logo` resolution (`AddonManifestParser.kt` `resolveAgainstManifest`) | HUSH does not render addon logos yet. Revisit with the UI work. |

### 4.5 NEEDS INVESTIGATION (or owner decision)

| ID | Item | Why it is open |
|---|---|---|
| N1 | CloudStream repository and index format (`repo.json`, `plugins.json`, `.cs3` with `fileHash`) | Documented in `EXTENSION_ARCHITECTURE.md` §2 [C], not re-verified this session. Re-check against the live index before any parser work. |
| N2 | Extension API name compatibility (`com.lagradost.cloudstream3.*` shim) | M3 legal review is **waived** by the owner (`DECISION_LOG.md` D-013, owner statement, no written record). This audit does not treat M3 as done. Owner should decide whether to record the waiver again in `docs/compatibility/LEGAL_REVIEW.md`. Dependency chain in §3 also needs review. |
| N3 | CloudStream repositories or plugin URLs using `http://` | HUSH refuses them (I1). Count how many published CloudStream entries use http before deciding. Affects "existing repositories must keep working". |
| N4 | HTTPS to HTTP redirect behaviour of OkHttp in `StremioClient` | Not checked in this session. Needs a test with a local server. [NT] |
| N5 | Addon list sync to a server (Nuvio `pullFromServer` / `sync_push_addons`) | Conflicting decisions: D-008 says "no backend and no accounts", D-010 approves Supabase. Also the sign-out policy in `PROJECT_STATUS.md` (preserve device-local data) is pending. Owner decision required before any sync design. |
| N6 | `ytId` streams (Nuvio `StreamParser.kt:30`) | Third-party content and terms of use. Owner decision required. |
| N7 | Stream URLs expire; download re-resolution | `SOFTWARE_ARCHITECTURE.md` says the worker re-resolves at run time (ADR-019, still PROPOSED). Depends on the ADR-019 downloads licence question. |
| N8 | Catalog pagination (`skip`) | Neither side was verified. HUSH currently returns the first page only. |

## 5. Security analysis needed before any integration

Required before implementation. Not done in this audit.

1. **Addon responses are untrusted data.** Titles, descriptions, stream labels and URLs come from third parties. HUSH should render them as text only and must not use them as navigation targets outside the app.
2. **Stream URLs.** Only https is accepted today. A5 (headers and referer) can leak addon-chosen headers to other hosts. Strip or allowlist header names before sending.
3. **Plugin or extension code.** Any dex or JS execution needs an isolation design (`EXTENSION_ARCHITECTURE.md` §5.2 items 4 and 5). Not designed yet.
4. **Privacy.** T1 and N5 both affect what data leaves the device. Log only host names, not stream URLs (`EXTENSION_ARCHITECTURE.md` §5.2(9)).
5. **Downloads.** Any download of an addon stream needs the same review (N7).

## 6. Top integration risks

1. **CloudStream compatibility requires a `com.lagradost` ABI shim, executing third-party dex in the app process.** This is the largest risk. It depends on N2 (waiver, not review), on the licence chain in §3, and on an isolation design (S2). Nothing in HUSH should depend on it until these are decided.
2. **Transport and model gaps block real streams.** HUSH refuses `http://` repositories (I1, N3) and its `StreamSource` cannot carry headers or referer (A5). Existing CloudStream repositories may fail, and HLS streams that need headers will not play.
3. **Untrusted addon data without a consent or enable gate.** No tracking-addon consent (T1), no enable/disable (T3), no resource gating (T2), and a cancellation bug (A4) that keeps stale work running. These are the first things to fix, independent of CloudStream.

## 7. Owner questions

1. Confirm whether the M3 waiver (D-013) should be re-recorded in `docs/compatibility/LEGAL_REVIEW.md` with the wording "waived, not performed".
2. Decide the https-only rule for CloudStream repositories (N3): keep strict, or allow `http://` with a warning (as Nuvio does).
3. Decide on addon sync (N5) given D-008 and D-010 conflict, and the pending sign-out policy.
4. Decide whether `ytId` (N6) and `externalUrl` (A7) streams are in scope.
5. Confirm that torrent and infoHash streams stay out of scope (S1).

## 8. Not done

- No code was changed, built or run. Build and test status: UNVERIFIED (no Android SDK in this environment, per `PROJECT_STATUS.md`).
- The CloudStream reference repository was not on disk; CloudStream statements are carried from existing HUSH docs [C].
- No CloudStream or community extension was downloaded, executed or decompiled.
- The Nuvio plugin runtime was read as source only.
