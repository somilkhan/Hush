# COMPONENT REUSE MATRIX — CloudStream

Owner: Agent 1. Reviewer: Claude.
Classes: **REUSE** (use as-is, legally permitted), **PORT** (adapt existing code), **REIMPLEMENT** (clean-room from documented behaviour), **REJECT**.

Important: under the GPL-3.0 licence of CloudStream (`LICENSING_AUDIT.md` §2), **no row marked PORT or REUSE is permitted to proceed** until the licence decision (D-A and the GPL question) is made by Claude and the user. Until then every row is provisionally **REIMPLEMENT** or **REJECT**. Nothing has been copied into HUSH.

Reference: CloudStream `a28e6468`. Verification status: [V] read in source; [NT] not tested.

| # | Subsystem | Original location (CloudStream) | Purpose | Key dependencies | Licence of source | Class (provisional) | Required modifications | Integration requirement | Testing strategy | Risks | Status |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Extension SDK API (`MainAPI`, `BasePlugin`, `ExtractorApi`, response types) | `library/src/commonMain/.../MainAPI.kt`, `plugins/BasePlugin.kt`, `utils/ExtractorApi.kt` | Contract between extensions and host | kotlinx, Jackson, nicehttp | GPL-3.0 | **Compatibility-required name surface, REIMPLEMENT shape** (see EXTENSION_ARCHITECTURE §3, decision D-A) | Clean-room ABI shim | Must match names and signatures extensions link against | Load and call each of 5 published extensions (NT) | D-A unresolved; legal review | Escalated |
| 2 | Extension loader (`PathClassLoader` + manifest) | `app/.../plugins/PluginManager.kt` `loadPlugin` (593-687) | Load `.cs3` as dex and instantiate plugin class | Android `dalvik.system` | GPL-3.0; provenance from Aliucord **[NV]** | REIMPLEMENT | Add sandboxing, per-plugin isolation, process model | Requires Android runtime | Load test on API 23, 29, 34+ (NT) | Possible Aliucord-derived code (OSL-3.0) | Design only |
| 3 | Repository model and index fetch | `app/.../plugins/RepositoryManager.kt` (`Repository`, `SitePlugin`, `getRepoPlugins`, `parseRepository`, `parsePlugins`) | Read `repo.json` and `plugins.json` | Jackson/kotlinx, nicehttp | GPL-3.0 | REIMPLEMENT from the **public format**, which is documented by the data in `EXTENSION_ARCHITECTURE.md` §2 | HTTPS enforcement, schema validation, honour `manifestVersion` | Repository install test (NT) | Format drift upstream | Design only |
| 4 | Short-code repo resolution (`cutt.ly`, `py.md`) | `RepositoryManager.parseRepoUrl` | Turn short codes into URLs | Third-party redirectors | GPL-3.0 | **REJECT** | Do not replicate third-party shortener dependency | None | None | Privacy, MITM, link rot | Rejected |
| 5 | jsDelivr URL rewrite | `RepositoryManager.convertRawGitUrl` | Optional CDN routing | jsdelivr.net | GPL-3.0 | REIMPLEMENT as an explicit user setting, off by default | Make it visible in settings | Setting UI (Agent 3) | None | Third-party CDN sees traffic | Design only |
| 6 | Download and hash verification | `RepositoryManager.downloadPluginToFile`, `sha256` | Integrity of `.cs3` | java.security | GPL-3.0 | REIMPLEMENT (SHA-256 check is standard) | Add signature check, not only hash | Index must carry signatures or be pinned | Tamper and corruption tests (NT) | Hash is from the same index as the file | Design only |
| 7 | Global HTTP client `app` (nicehttp) | `library/.../MainActivity.kt` line 30 | Shared client for extensions | nicehttp (GPL-3.0), OkHttp | GPL-3.0 | **Compatibility-required, REIMPLEMENT** the API as a wrapper over OkHttp | Provide `Requests`/`NiceResponse`-shaped surface | ABI shim (decision D-A) | Call surface tested against extension use (NT) | Copyleft if nicehttp is used | Escalated |
| 8 | Host-side extractor dispatch | `ExtractorApi.kt` `loadExtractor` (~930) | Pick extractor by URL prefix, last-registered wins | None | GPL-3.0 | REIMPLEMENT, behaviour documented in REVERSE_ENGINEERING §4.8 | Keep "last registered wins" only if extensions depend on it | Compatibility tests | Priority order test (NT) | Subtle ordering behaviour | Design only |
| 9 | Per-operation timeouts and error wrapping | `ui/APIRepository.kt`, `library/.../mvvm/ArchComponentExt.kt` (`safeApiCall`, `Resource`) | Bounded calls, error states | kotlinx coroutines | GPL-3.0 | REIMPLEMENT | Map to HUSH `Result`/error taxonomy (Agent 2) | Agent 2 contract C2 | Timeout and error tests (NT) | Low | Design only |
| 10 | Rolling load-response cache | `ui/APIRepository.kt` `SavedLoadResponse`, `cache`, `CACHE_SIZE` | Avoid refetching details | None | GPL-3.0 | REIMPLEMENT (simple LRU) | Size and TTL policy | Agent 2 owns cache | Unit tests (NT) | Stale data | Design only |
| 11 | Subtitle decoding (`juniversalchardet`) | Library use in `app/` | Detect subtitle encoding | juniversalchardet (MPL-1.1 / GPL / LGPL tri-licence) | Tri-licence | **REUSE candidate** (choose MPL-1.1 or LGPL-2.1) | Keep notices | Gradle dependency | Encoding tests (NT) | Licence choice must be recorded | Pending Agent 2 decision |
| 12 | Trailers (NewPipe Extractor) | `:library` `jvmCommonMain` | YouTube trailers | NewPipe (GPL-3.0) | GPL-3.0 | **REJECT** for now | Not needed for extension compatibility | None | None | GPL-3.0 | Rejected |
| 13 | Player extensions (nextlib) | `app/.../ui/player/UpdatedDefaultExtractorsFactory.kt`, `UpdatedMatroskaExtractor.kt` | Extra codecs and containers | nextlib (GPL-3.0) | GPL-3.0 | **REJECT** the code. Re-evaluate use of media3 defaults first. | Agent 2 to test media3 coverage | Playback contract | Playback tests on device (NT) | Copyleft | Rejected pending media3 evaluation |
| 14 | Player extractor job (`extractorVerifierJob`) | `GeneratorPlayer.kt` `loadExtractorJob` (~260) | Keep provider links alive | None | GPL-3.0 | REIMPLEMENT | Keep as provider-optional hook | Contract C6 | Playback test (NT) | Provider-specific | Design only |
| 15 | Metadata providers (TMDB, Trakt) | `library/.../metaproviders/` | Enrich media details | TMDB/Trakt APIs | GPL-3.0 | REJECT for v1 | Not required for extension compatibility | Would need API keys | None | Credentials, licensing of API terms | Rejected for v1 |
| 16 | Sync and tracking (AniList, MAL, Simkl, Kitsu) | `library/.../syncproviders/`, `app/.../syncproviders/` | Watch-state sync | Vendor APIs, BuildConfig keys | GPL-3.0 | REJECT for v1 | Out of scope for the first milestone | None | None | Credentials in BuildConfig | Rejected for v1 |
| 17 | Extension SDK HTML parsing helpers (jsoup, ksoup) | Dependencies | Parsing in providers | MIT | MIT | **REUSE** as dependency (MIT, keep notices) | Pinned versions for ABI | Required by extensions (see DEPENDENCY_MAP §3) | ABI tests (NT) | Version mismatch | Dependency-level reuse approved in principle |
| 18 | Compose migration UI (`shared/`, `COMPOSE.md`) | `shared/src/commonMain/...` | Future UI | Compose MP | GPL-3.0 | **REJECT** code. Use the MVI guidance only as a public idea. | Agent 3 designs independently | Agent 3 contract | N/A | Copying UI would conflict with the "independent UI" rule | Rejected |

## Summary

- **Escalated (needs decision):** 1 (SDK surface), 7 (nicehttp surface), linked to decision D-A in `LICENSING_AUDIT.md`.
- **Reimplement from behaviour:** 2, 3, 5, 6, 8, 9, 10, 14.
- **Reuse as permissive dependency:** 11 (choose MPL-1.1 or LGPL-2.1), 17.
- **Reject:** 4, 12, 13 (code), 15, 16, 18 (code).

## Clean-room discipline (proposed rule for Agents 1 and 2)

Before implementing any REIMPLEMENT row, the implementer records in `docs/reverse-engineering/` which **public** sources were used (this file, the public extension data, and HUSH's own spec). Where CloudStream source was read during design, the implementer must not transcribe it. This is not legal advice; counsel should confirm the process.
