# EXTENSION ARCHITECTURE — CloudStream-compatible extension system

Owner: Agent 1. Consumers: Agent 2 (core), Agent 3 (UI, through the adapter boundary only).
Reference: CloudStream `a28e6468`. Verification status tags as in `REVERSE_ENGINEERING.md`: **[V]** read in source, **[D]** observed in public data or bytes, **[I]** inference, **[NV]** not verified, **[NT]** not tested.

## 1. Overview

CloudStream extensions are Android dex archives (`.cs3`) that:
1. Are listed in a **repository** (a JSON index reached by URL).
2. Are downloaded, hash-checked and stored in app-private storage.
3. Are loaded at run time by a `PathClassLoader` whose parent is the host app's class loader.
4. Register provider objects (`MainAPI`) and extractors (`ExtractorApi`) with the host.
5. Are called by the host for search, details and stream resolution.

Two boundaries matter for HUSH:
- **ABI boundary**: what the `.cs3` code links against. Fixed by the extensions, not by HUSH.
- **UI boundary**: what HUSH's Compose UI sees. HUSH chooses this and it must not expose CloudStream types.

## 2. Repository and package format [D]

All of this was verified against the public extensions repository on 2026-11-24.

### 2.1 Repository manifest (`repo.json`)

URL: `https://raw.githubusercontent.com/recloudstream/extensions/master/repo.json`. Content (summary):

```
name: "Cloudstream providers repository"
description: "Cloudstream extension Repository"
manifestVersion: 1
pluginLists: ["https://raw.githubusercontent.com/recloudstream/extensions/builds/plugins.json"]
```

Matches the host model `Repository` in `RepositoryManager.kt` (fields `name`, `description`, `manifestVersion`, `pluginLists`, `iconUrl`).

### 2.2 Plugin list (`plugins.json`)

A JSON **array** of entries. Five entries at the time of audit. Fields seen on every entry: `url`, `status`, `version`, `apiVersion`, `name`, `internalName`, `authors`, `description`, `repositoryUrl`, `tvTypes`, `iconUrl`, `fileSize`, `fileHash`, plus `jarUrl`, `jarHash`, `jarFileSize`.

- `status`: `1` on all five. Host semantics per source comment: 0 Down, 1 OK, 2 Slow, 3 Beta only.
- `apiVersion`: `1` on all five. Source comment says "unused currently".
- `fileHash`: `sha256-<64 hex>`. Verified: the SHA-256 of each downloaded `.cs3` equals `fileHash` for all five entries.
- `jarUrl`, `jarHash`, `jarFileSize`: present in the public index but **not** in this commit's `SitePlugin` model. They are ignored at parse time (see §2.4). Their purpose is **[NV]**; they probably point at a `.jar` build of the same plugin.

### 2.3 Extension archive (`.cs3`)

Verified on `DailymotionProvider.cs3` (11,472 bytes) and listed for all five:

```
manifest.json   120 bytes
classes.dex     26,036 bytes (for Dailymotion)
```

`manifest.json` example:
```json
{"requiresResources":false,"version":4,"pluginClassName":"recloudstream.DailymotionPlugin","name":"DailymotionProvider"}
```

- It is a ZIP. `classes.dex` makes it Android-only. It cannot run on a desktop JVM.
- The host parses it with `parseJson<BasePlugin.Manifest>` after `getResourceAsStream("manifest.json")`.
- `pluginClassName` is the fully qualified name of a `BasePlugin` subclass in `classes.dex`.

### 2.4 Parsing tolerance [V][D]

- `library/.../MainAPI.kt`: kotlinx `Json { ignoreUnknownKeys = true }` (line ~103) and Jackson with `FAIL_ON_UNKNOWN_PROPERTIES = false` (line ~107). **Unknown fields are ignored**, so the `jar*` fields do not break parsing. HUSH must keep this forward-compatibility rule.

### 2.5 Version and compatibility rules [V]

- `Repository.manifestVersion`: parsed, not enforced.
- `SitePlugin.apiVersion`: parsed, not enforced ("used to make the api backwards compatible?").
- `SitePlugin.version`: integer; a change triggers auto-update. `PLUGIN_VERSION_ALWAYS_UPDATE = -1` forces update. `PLUGIN_VERSION_NOT_SET = Int.MIN_VALUE` is the placeholder.
- The real compatibility gate is therefore **the binary ABI**, not a declared version. A mismatch appears as a `Throwable` during load or call.

## 3. Host ABI that extensions link against [D]

Source: static survey of the `classes.dex` string tables of the five published extensions (hash-verified). This is the **minimum** set observed. It is not a full signature list.

### 3.1 Required class names

- `com.lagradost.cloudstream3`: `MainAPI`, `MainAPIKt`, `MainActivityKt` (for `getApp`), `LoadResponse`, `SearchResponse`, `SearchResponseList`, `MovieSearchResponse`, `MovieLoadResponse`, `TvSeriesLoadResponse`, `LiveStreamLoadResponse`, `LiveSearchResponse`, `HomePageResponse`, `HomePageList`, `MainPageRequest`, `MainPageData`, `TvType`, `SubtitleFile`, `Actor`, `ActorData`, `ActorRole`, `Episode`, `ErrorLoadingException`
- `com.lagradost.cloudstream3.plugins`: `BasePlugin`, `CloudstreamPlugin`
- `com.lagradost.cloudstream3.utils`: `ExtractorApi`, `ExtractorApiKt`, `ExtractorLink`, `ExtractorLinkType`, `Qualities`, `AppUtils`, `StringUtils`
- `com.lagradost.cloudstream3.mvvm`: `ArchComponentExtKt`
- `com.lagradost.nicehttp`: `Requests`, `ResponseParser`, `NiceResponse`
- Third-party: Jackson (`ObjectMapper`, `JsonMapper`, `TypeReference`, Kotlin module), jsoup (`org/jsoup/select/Elements`), OkHttp (`okhttp3/Interceptor`), Kotlin stdlib.

### 3.2 Overridden or called methods

`getMainPage`, `search`, `load`, `name`, `mainUrl`, `lang`, `hasMainPage`, `supportedTypes`, `mainPage`, `getUrl`, `requiresReferer`, `registerMainAPI`, `registerExtractorAPI`, `getApp`.

`loadLinks` is **not** observed in any of the five. Its shape is taken from the source (`MainAPI.kt`): `loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean`. **[NT]** until a sample extension implements it.

### 3.3 Data shapes that cross the boundary [V]

- `ExtractorLink(source, name, url, referer, quality: Int, headers: Map<String,String>, extractorData: String?, type: ExtractorLinkType, audioTracks)`. Build with `newExtractorLink` (the direct constructor is deprecated).
- `ExtractorLinkType`: `VIDEO`, `M3U8`, `DASH`, and others.
- `SubtitleFile`: `lang`, `url`, `headers`, `langTag`. Factory functions only.
- `Episode`: `data`, `name`, `season`, `episode`, `posterUrl`, `score`, `description`, `date`, `runTime`, `rating`.
- `Score`, `Actor`, `TvType` are also part of the surface.

### 3.4 Why this matters

The extension compiles against these names. If HUSH does not provide them with compatible shapes at runtime, the extension fails to link. There is no runtime negotiation. See `LICENSING_AUDIT.md` decision D-A before choosing an approach.

## 4. Loading lifecycle [V]

```
add repository  ->  parseRepoUrl / addRepository (store URL)
discover        ->  parseRepository (repo.json) -> parsePlugins (plugins.json) -> PluginWrapper list
install         ->  downloadPluginToFile (temp in cacheDir, SHA-256 check, atomic move)
load            ->  PathClassLoader(file, parent = app loader)
                    manifest.json -> pluginClassName -> newInstance -> load(context)
                    plugin registers MainAPI / ExtractorApi
run             ->  search / load / loadLinks through APIRepository wrappers (timeouts)
unload          ->  beforeUnload(); maps cleared; file deletion via deletePlugin
```

Lifecycle hooks: `BasePlugin.load()`, `Plugin.load(context)`, `beforeUnload()`. Background refresh runs from `SubscriptionWorkManager` (WorkManager).

## 5. Security analysis [V] with recommendations

### 5.1 Current CloudStream model (what HUSH must not inherit silently)

| Property | Current behaviour (source) | Risk |
|---|---|---|
| Execution privilege | Extension code runs in the **app process**, with the app's class loader as parent (`PathClassLoader(filePath, context.classLoader)`) | Full app privilege: any app class, any app-private file, any permission the app holds (INTERNET, storage, install packages) |
| Isolation | None. Extensions share the heap and threads. Load failures are caught, but runtime exceptions and infinite loops are not isolated. | A malicious or broken extension can crash or stall the app |
| Integrity | Optional SHA-256 from the index (`fileHash`). The index and the file come from the same source. | Protects against corruption, not against a compromised repository |
| Authenticity | No signature. Trust is in the repository URL. | Any repository owner can ship arbitrary code |
| Transport | `usesCleartextTraffic="true"` on the application (`AndroidManifest.xml` line 91). Repository URLs accept `http://` (`parseRepoUrl` keeps any `https?://`). | Repository and plugin downloads can be intercepted and modified over HTTP |
| Redirectors | Short codes resolved via `cutt.ly` and `py.md` (`parseRepoUrl`) | Third-party resolution of user input; link rot; traffic exposure |
| CDN | Optional `cdn.jsdelivr.net` rewrite (`convertRawGitUrl`) | Third-party CDN sees fetches |
| Storage | `MANAGE_EXTERNAL_STORAGE` declared, with the comment "Plugin API" | Broad storage access, not limited to the extension's own folder |
| Dynamic code | Dex files are set read-only before load (Android 14+ requirement, comment near line 528) | Mitigates tampering after install, not before |
| Settings and state | Repository list and plugin metadata in the app's key-value store | Standard app-private storage |

### 5.2 Recommended HUSH posture (proposal for Claude's decision)

1. **Transport:** HTTPS only for repository and plugin URLs, with cleartext blocked (`networkSecurityConfig`). Reject `http://` at input time. This is stricter than CloudStream and may break some existing repositories. Surface the break explicitly to the user.
2. **Authenticity:** a signature per extension (or a signed index) verified before load, with the key pinned at repository add time and shown to the user. A hash alone is not enough.
3. **Install consent:** show the extension's authors, permissions it needs, and its source repository, and require explicit confirmation on first install and on every update that changes the hash.
4. **Process isolation (preferred):** load extensions in a separate Android process (`android:process`) reached over a narrow IPC boundary (for example AIDL or a bound service carrying `ExtractorLink`-shaped data). A crash or hang then does not take down the UI. Cost: the ABI shim must also exist in that process, and IPC adds latency. **[I]** feasibility not tested.
5. **Narrow class loader:** if isolation is not used, the `PathClassLoader` parent should be a shim-only loader that does not expose HUSH's own internals. **[I]** feasibility not tested.
6. **Permissions:** extensions get **no** storage permissions beyond their own folder. HUSH should not declare `MANAGE_EXTERNAL_STORAGE`. Network access should go through a HUSH-owned client that logs destinations and enforces timeouts.
7. **Kill switch:** honour `status` (0 down, 3 beta gating) and allow the user to disable a repository or extension without deleting it.
8. **Error isolation:** wrap every extension call in a timeout and a `Throwable` catch. Treat a repeated failure as the extension being broken, not the app.
9. **Logging:** record extension name, version, and destination host for each network call, without logging stream URLs or user data.
10. **Third-party content:** HUSH must not bundle default repositories or extensions without a licence check (`LICENSING_AUDIT.md` §5).

### 5.3 Security test plan (not executed)

- Malicious `pluginClassName` (absent class, wrong type): expect a caught error, no crash. **[NT]**
- Extension that throws in `load()` and in `search()`: expect isolation. **[NT]**
- Hash mismatch and signature mismatch: expect refusal before load. **[NT]**
- `http://` repository URL: expect refusal. **[NT]**
- Extension attempts to read an app-private file outside its folder: expect denial under the chosen model. **[NT]**

## 6. Compatibility-layer boundary (proposal)

Two interfaces, deliberately separate.

### 6.1 HUSH-facing port (the only thing the UI may depend on)

Written in HUSH's own terms. No `com.lagradost` types cross this line. Sketch (signatures only, not code to be copied):

```kotlin
// Owned by Agent 2 (core); consumed by Agent 3 (UI) via view-model state only.
interface ExtensionRepositoryPort {
    suspend fun addRepository(address: RepositoryAddress): Outcome<RepositoryInfo>
    suspend fun refresh(repositoryId: RepositoryId): Outcome<List<ExtensionListing>>
    suspend fun install(listing: ExtensionListing): Outcome<InstalledExtension>
    suspend fun remove(extensionId: ExtensionId): Outcome<Unit>
    fun observeInstalled(): Flow<List<InstalledExtension>>
}

interface ProviderPort {
    fun observeProviders(): Flow<List<ProviderInfo>>            // enabled, status, language, types
    suspend fun mainPage(provider: ProviderId, page: Int): Outcome<List<MediaShelf>>
    suspend fun search(provider: ProviderId, query: String, page: Int): Outcome<SearchPage>
    suspend fun details(provider: ProviderId, mediaUrl: MediaRef): Outcome<MediaDetails>
    /** Streams are delivered as a flow so the UI can show progress and partial results. */
    fun resolveStreams(provider: ProviderId, episodeRef: EpisodeRef): Flow<StreamEvent>
}

sealed interface StreamEvent {
    data class Stream(val candidate: StreamCandidate) : StreamEvent
    data class Subtitle(val track: SubtitleTrack) : StreamEvent
    data class Failed(val reason: ProviderError) : StreamEvent
    data object Finished : StreamEvent
}

sealed interface Outcome<out T> { data class Ok<T>(val value: T) : Outcome<T>; data class Err(val error: ProviderError) : Outcome<Nothing> }

sealed interface ProviderError {
    data object Timeout : ProviderError
    data object Unavailable : ProviderError        // status Down or host unreachable
    data object NoData : ProviderError              // maps ErrorLoadingException / null result
    data class Broken(val message: String) : ProviderError  // extension threw
    data class Untrusted(val reason: String) : ProviderError // signature or hash failure
}
```

Domain types (`MediaDetails`, `StreamCandidate`, `SubtitleTrack`, `EpisodeRef`, `MediaRef`) are defined by Agent 2 in `docs/architecture/DOMAIN_MODEL.md`. Stream candidates must carry: URL, referer, headers, quality, container type (VIDEO / HLS / DASH), and extractor name, so the player can choose the right media source.

### 6.2 ABI shim (behind the port, for `.cs3` binary compatibility only)

- Lives in its own module (proposed `compat/cloudstream-abi`), so its licence status is isolated and reviewable.
- Provides the classes in §3.1 with compatible shapes, implemented over HUSH's adapters.
- Its implementation and the `ProviderPort` translation are Agent 1's work. The port it implements is Agent 2's.
- Not visible to the UI module. Enforced by Gradle module visibility.

### 6.3 Mapping (summary)

| CloudStream (extension side) | HUSH port side |
|---|---|
| `MainAPI.search` -> `List<SearchResponse>` | `ProviderPort.search` -> `SearchPage` |
| `MainAPI.load` -> `LoadResponse` (+ `Episode`s) | `ProviderPort.details` -> `MediaDetails` |
| `MainAPI.loadLinks` + `ExtractorLink` callback | `ProviderPort.resolveStreams` -> `Flow<StreamEvent>` |
| `SubtitleFile` callback | `StreamEvent.Subtitle` |
| `ErrorLoadingException` / null result | `ProviderError.NoData` |
| Thrown `Throwable` | `ProviderError.Broken` |
| `timeoutMs` properties | Port-level timeout policy (Agent 2) |

## 7. Contracts needed from Agent 2 and Agent 3

### 7.1 From Agent 2 (core application)

| # | Contract | Why | Needed before |
|---|---|---|---|
| C1 | `ExtensionRepositoryPort`, `ProviderPort`, `Outcome`, `ProviderError` exactly as in §6.1 (names may change, shape should not) | The compat layer implements these | Any implementation |
| C2 | Process model decision: in-process or isolated process (§5.2 item 4) | Determines where the ABI shim and loader live | Loader work |
| C3 | Storage locations: `filesDir/extensions/<repo-id>/`, `cacheDir` for temp downloads, key-value keys for repositories and installed-extension metadata | Install and removal | Install work |
| C4 | Domain models for media, episodes, streams and subtitles (`DOMAIN_MODEL.md`), with the field list in §6.1 | Mapping from extension types | Mapping work |
| C5 | Network policy: HTTPS-only, a single HUSH-owned HTTP client that the ABI shim wraps, timeouts | nicehttp shim and security posture | Network work |
| C6 | Playback contract: `StreamCandidate` shape, headers and referer propagation, HLS / DASH / progressive selection on media3 | Stream resolution | Playback work |
| C7 | Error taxonomy and retry rules (§6.1) | UI states | UI work |
| C8 | Timeouts per operation, with defaults when an extension specifies none | Avoid hangs | Core |
| C9 | Dependency decisions for JSON, HTML parsing and HTTP that match the extension ABI pins (see DEPENDENCY_MAP §3) | Binary compatibility | Build setup |

### 7.2 From Agent 3 (UI)

| # | Contract | Why |
|---|---|---|
| U1 | UI state for extension lists: loading, empty, error, per-extension status (`Down`, `Slow`, `Beta`, `Untrusted`) | Extension management screens |
| U2 | Consent screen content: authors, source repository, requested capabilities, hash or signature change | Install consent (§5.2 item 3) |
| U3 | Provider picker shows status and language from `ProviderInfo` only | No CloudStream types in UI |
| U4 | Stream selection UI reads `StreamCandidate` only | Player independence |
| U5 | Visible "Extension error" and "Repository blocked (HTTP)" states | Failure handling |

## 8. Open questions for Claude (blocking)

1. **D-A:** How does HUSH satisfy the ABI names in §3.1 without adopting GPL-3 (clean-room shim, GPL-3 for HUSH, or companion component)? (`LICENSING_AUDIT.md` §4)
2. **Namespace:** Does `com.lagradost.cloudstream3` appear in HUSH's runtime at all? Handover §0 asks for an independent namespace; the ABI requires these names at runtime. Both cannot be fully satisfied without the decision in D-A.
3. **HTTP:** Is HTTPS-only (§5.2 item 1) acceptable given existing repositories that use HTTP?
4. **Trust model:** Is a per-extension signature required for v1, or is the hash plus user consent enough?
5. **Process model:** Is process isolation (§5.2 item 4) a v1 requirement?

## 9. Verification status of this document

- Repository format, hash verification and ABI survey: **[D]**, done on five extensions, nothing executed.
- Loading, registration, search, details, stream extraction and subtitles: **[V]** from source only, **[NT]** at run time.
- Security posture: analysed from source; no exploit or isolation testing performed.
- See `COMPATIBILITY_MATRIX.md` for per-operation status.

## M2 answers (to Agent 2's INTEGRATION_CONTRACTS.md §5)

Reference: CloudStream commit `a28e6468f54cfe420feebd205d3d21d08ed0cde8` (checked out at the session scratchpad, read only, not executed). Paths are relative to that clone. Labels: **VERIFIED** = read in source; **INFERENCE** = reasoned, not checked in source; **NOT VERIFIED** = outside this repo or not checked.

### Q1. Does search and details split into two calls?
**VERIFIED: yes.** Search and details are separate operations on `MainAPI` (`library/src/commonMain/kotlin/com/lagradost/cloudstream3/MainAPI.kt`):
- `search(query: String, page: Int): SearchResponseList?` (line ~641). Default delegates to `search(query)`, which returns `List<SearchResponse>?` (line ~651). `quickSearch(query)` at line ~656.
- `load(url: String): LoadResponse?` (line ~665). Input is a URL string, not an id.
- Search results are summaries. `SearchResponseList` carries `hasNext`, so paging is per provider.
- Caller: `app/.../ui/search/SearchViewModel.kt` (`a.search(query, 1)` at ~235, `repo.search(query, nextPage)` at ~137). Details: `APIRepository.load` (`app/.../ui/APIRepository.kt`), which caches results for 10 minutes.

Impact on HUSH: `SearchPort` and `MediaDetailsPort` can keep their shapes. Two corrections are needed. Pagination is an `Int` page plus `hasNext`, not an opaque cursor, so `cursor: String?` is a poor fit; the compat layer can encode the page into the cursor. `MediaRef` must map to the provider's URL string.

### Q2. Are episodes always in details, or fetched separately?
**VERIFIED: episodes come with details.** There is no episode-only operation on `MainAPI`. A grep for `getEpisodes`, `loadEpisodes` and `episodes(` in `library/src` returns nothing.
- Series: `TvSeriesLoadResponse.episodes: List<Episode>` (`MainAPI.kt` ~2673).
- Each `Episode` has `data: String` (`MainAPI.kt` ~2550). That string is the only input `loadLinks` takes for playback.
- Movies: `MovieLoadResponse.dataUrl: String` (`MainAPI.kt` ~2418) plays the same role.
- There is no season parameter anywhere in the contract. Season structure is whatever the extension puts in the episode list.

Impact on HUSH: `EpisodePort.episodes(ref, season)` has no CloudStream equivalent. Drop it, or treat it as a filter over the details result. `EpisodeRef` should carry the episode `data` string. For a movie, the equivalent is `dataUrl`.
INFERENCE: some extensions may fetch episodes lazily inside their own code. That is not visible to the host and is not part of the contract.

### Q3. Is stream resolution incremental or one call? Are subtitles extracted inside it?
**VERIFIED: one suspend call that reports incrementally through callbacks.**
- Signature (`MainAPI.kt` ~692): `suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean`.
- The function returns only when it finishes. Links are delivered through `callback` as they are found.
- The player consumes it incrementally: `app/.../ui/player/PlayerGeneratorViewModel.kt` (~386-430) calls `modifyState { add(link) }` inside each callback. `RepoLinkGenerator.generateLinks` (`app/.../ui/player/RepoLinkGenerator.kt`, call at ~109) forwards both callbacks.
- **Subtitles are inside the same call.** `subtitleCallback` is passed through to `loadExtractor(url, referer, subtitleCallback, callback)` (`library/.../utils/ExtractorApi.kt` ~912-968). Extractors can emit subtitles during `loadLinks`.
- `SubtitleFile` (`MainAPI.kt` ~1203) has `lang`, `url` and `headers`. `ExtractorLink` (`library/.../utils/ExtractorApi.kt` ~698) has `url`, `referer`, `headers`, `quality`, `type`, `audioTracks`.

Impact on HUSH:
- `StreamResolutionPort` can stay a `Flow`. Wrap the callbacks with `callbackFlow`. The Flow is an adapter over the callbacks, not a native incremental API.
- `SubtitlePort.subtitles(...)` is redundant for CloudStream providers. Subtitles only exist as a side effect of `loadLinks`. Recommend making subtitles a property of `StreamSource` or an event (`SubtitleFound`), not a separate call.
- Failure detail is lost. `loadLinks` returns `Boolean`, and `APIRepository.loadLinks` catches `Throwable` and returns `false` (see Q5). `StreamEvent.Failed(AppError)` can only be built from errors the compat layer catches itself.
- `getVideoInterceptor(extractorLink): Interceptor?` (`MainAPI.kt` ~702) returns an OkHttp `Interceptor` used at playback. HUSH's `StreamSource` has no slot for it. Either the player must be OkHttp-based, or the contract needs a playback hook. Flag for Agent 3 and Claude.

### Q4. Which operations can an extension leave unimplemented?
**VERIFIED.** The `MainAPI` base class throws `NotImplementedError` by default for these:

| Operation | Default (`MainAPI.kt`) | Host gate before calling |
|---|---|---|
| `getMainPage` | throws (~633) | `hasMainPage` (~563) checked in `HomeViewModel.kt` (~136, ~329) |
| `search(query)` | throws (~651) | none; the search screen calls every provider (`SearchViewModel.kt` ~233-235) |
| `quickSearch` | throws (~656) | `hasQuickSearch` (~564) checked in `SearchViewModel.kt` (~233) and `QuickSearchFragment.kt` (~144) |
| `load` | throws (~665) | none |
| `extractorVerifierJob` | throws (~679) | none; background job during playback |
| `loadLinks` | throws (~692) | none |

- A thrown `NotImplementedError` becomes `Resource.Failure(false, "This operation is not implemented.")` (`library/.../mvvm/ArchComponentExt.kt`, `throwAbleToResource`). That is the host's own "unsupported" signal.
- Non-throwing defaults: `getVideoInterceptor` returns `null`. `getLoadUrl(name, id)` returns `null`. `supportedSyncNames` is empty. `supportedTypes` has a default set.
- **There is no capability flag for subtitles.** An extension with no subtitle support and one with no subtitles for a given title look the same: `subtitleCallback` never fires. HUSH's `SubtitlePort` distinction between `Unsupported` and empty cannot be made from CloudStream data. Compat can only report "no subtitles observed".
- Operations an extension must implement to be useful: `search` (or `quickSearch`), `load` and `loadLinks`. Without `loadLinks`, nothing can play.
- Matrix note: no sample extension among the five surveyed was observed to override `loadLinks` (`COMPATIBILITY_MATRIX.md` §2 row 9). That is a DATA-VERIFIED observation about the sample set only.

### Q5. Where does provider execution run, and what timeouts apply?
**Execution: VERIFIED.**
- In process. `app/.../plugins/PluginManager.kt` `loadPlugin` (~593-611) builds `PathClassLoader(filePath, context.classLoader)`, so the extension shares the app process, its UID and its heap. There is no separate process, no class-loader isolation beyond the Android loader, and no permission boundary.
- The file is set read-only before loading (~597).
- Thread: `safeApiCall` (`library/.../mvvm/ArchComponentExt.kt` ~207-218) calls `ioWork`, which uses `withContext(workerDispatcher)` (`library/.../utils/Coroutines.kt` ~46-50). On JVM, `workerDispatcher = Dispatchers.IO` (`library/src/jvmCommonMain/.../Coroutines.jvmCommon.kt:6`).
- `loadLinks` runs inside `safeApiCall` in the player's generator path, so it is also on `Dispatchers.IO`.

**Timeouts: VERIFIED.** Defined in `app/.../ui/APIRepository.kt`:
- `DEFAULT_TIMEOUT = 120_000L` (2 min), `MIN_TIMEOUT = 5_000L`, `MAX_TIMEOUT = 4 * DEFAULT_TIMEOUT` (8 min). `getTimeout(desired)` clamps the provider's value to [5 s, 8 min] and uses 2 min when unset.
- Per-operation overrides on `MainAPI`: `loadLinksTimeoutMs` (~573), `getMainPageTimeoutMs` (~580), `searchTimeoutMs` (~587), `quickSearchTimeoutMs` (~594), `loadTimeoutMs` (~601). The doc comment calls them a "hint" (~568), but the code clamps them as above.
- Enforced with `withTimeout` in `APIRepository`: `load`, `search`, `quickSearch`, `getMainPage`, `loadLinks` (~209). `extractorVerifierJob` has no timeout.
- The timeout covers the whole `loadLinks` call, not each stream. A slow provider can hold the stream flow for up to 8 minutes.

**Failure semantics: VERIFIED, important for HUSH's cancellation rule.**
- `safeApiCall` catches `Throwable`. `throwAbleToResource` maps `CancellationException` to `safeFail(...)` (`ArchComponentExt.kt` ~196-199, `safeFail` ~144-147), which returns `Resource.Failure`. **Cancellation is turned into a failure and is not rethrown.**
- `APIRepository.loadLinks` catches `Throwable` (including the timeout's `CancellationException`), logs it, and returns `false`. A timeout in `loadLinks` is silent.
- Consequence: HUSH's compat layer must not call these wrappers for `load`, `search` or `loadLinks`. It must call `MainAPI` methods directly and rethrow `CancellationException`. This conflicts with nothing in the contracts, but it is a real CloudStream behaviour that must not be copied.
- `SearchViewModel` (~233-250) fans out with `amap` on `Dispatchers.IO`. A new query does not cancel older provider calls. They are ignored through a `currentSearchIndex` check. HUSH's "cancelling the collector cancels all in-flight provider work" is stricter than CloudStream.

**INFERENCE:** `withTimeout` only interrupts at suspension points. A provider blocked in a non-suspending call (for example a synchronous socket read) is not stopped at the timeout. This follows from kotlinx.coroutines semantics, not from a test.

**Default timeouts to propose for HUSH (from CloudStream, not tested):** search 2 min, details 2 min, stream resolution 2 min per call, with the same 5 s floor and 8 min ceiling.

### Q6. Minimum SDK the extension runtime needs
**VERIFIED: 23.** `gradle/libs.versions.toml` line 67: `minSdk = "23"`. `app/build.gradle.kts` (~107) and `library/build.gradle.kts` (~30) both read `libs.versions.minSdk`. Other values: `compileSdk = "37"` (line 68), `targetSdk = "36"` (line 69), `jvmTarget = "11"` (line 65).
- CloudStream pins older libraries to keep API 23 working. Jackson `2.13.1` is pinned with the comment that later versions do not support minSdk below 26 and crash on Android TV and Fire TV (line 22). Rhino `1.8.1` is pinned because 1.9.0 and later need minSdk 26 (line 50).
- **INFERENCE:** HUSH at minSdk 26 (the provisional value) is compatible with the extension ABI. A higher minimum does not break extensions, and it lets HUSH drop the Jackson and Rhino pins. No CloudStream source checked here requires API below 26 at run time apart from those pins.
- Decision for Claude: the extension ABI does not force 23. Choose the minimum by the device target, not compatibility. Keep 26 provisional until Claude decides (EXECUTION_PLAN §8.5).

### Q7. Does the extension layer need a specific HTTP client or cookie store?
**VERIFIED: extensions rely on a global client, and cookies live in two places.**
- Global client: `library/src/commonMain/kotlin/com/lagradost/cloudstream3/MainActivity.kt` (~30-34) declares a top-level `var app = Requests(responseParser = jsonResponseParser)` with a user-agent default header. `insecureApp` (~37) is the same without SSL checks and is annotated `@UnsafeSSL`. Both are public, mutable top-level variables.
- Example use: `library/.../extractors/Up4Stream.kt` (~31) calls `app.get(url, cookies = mapOf("id" to movieId))`.
- Underlying OkHttp client: `app/.../network/RequestsHelper.kt` (~20) sets `baseClient = buildDefaultClient(context)`. That builder (~38-70) sets `followRedirects`, a 50 MiB HTTP cache at `cacheDir/http_cache`, a user-selected DNS option, and a Conscrypt security provider.
- Per-request cookies: `getHeaders(headers, cookie)` (`RequestsHelper.kt`, end of file) merges default headers, request headers and a `Cookie` header built from a map.
- Persistent cookie store: `CloudflareKiller.kt` (~37, ~81) uses Android WebView's global `CookieManager` (`removeAllCookies`, `getCookie`). A WebView resolver also exists in `library/.../network/WebViewResolver.kt`.
- The `nicehttp` library (pinned `0.4.18` in `gradle/libs.versions.toml` lines 44 and 136) supplies `Requests`, `cookies` and `RequestBodyTypes`. **NOT VERIFIED:** the cookie jar inside nicehttp. Its source is not in this repository.

Impact on HUSH:
- Extensions reference the global `app`/`insecureApp` names (`DATA-VERIFIED` in the matrix). The compat layer must supply compatible `Requests` objects, or the extension will fail to link. That is part of the D-A decision.
- Compat needs one OkHttp client with an explicit cookie store, and that store should be per repository, not global. Two stores exist in CloudStream (nicehttp and WebView); HUSH should pick one and document it.
- Because `app` is a mutable global, an extension can replace the client for every other extension. That is a security concern for §5 of this document, not a compatibility requirement.
- `getVideoInterceptor` (Q3) uses OkHttp, so the player path must also accept OkHttp interceptors.

### Summary table

| Q | Answer | Status |
|---|---|---|
| 1 | Two calls: `search(query, page)` returns summaries; `load(url)` returns details. Pagination is `Int` + `hasNext`. | VERIFIED |
| 2 | Episodes come inside `load` (`TvSeriesLoadResponse.episodes`). Each episode's `data` drives `loadLinks`. No episode-only call. | VERIFIED |
| 3 | `loadLinks` is one suspend call with `callback` (links) and `subtitleCallback`. Subtitles come from inside it. | VERIFIED |
| 4 | `getMainPage`, `search`, `quickSearch`, `load`, `extractorVerifierJob`, `loadLinks` all throw `NotImplementedError` by default. No subtitle capability flag. | VERIFIED |
| 5 | In-process, `Dispatchers.IO`. Default 120 s (floor 5 s, ceiling 8 min). `safeApiCall` swallows cancellation. | VERIFIED; blocking-call behaviour INFERENCE |
| 6 | minSdk 23 (CloudStream). HUSH 26 compatible by inference. | VERIFIED (23); INFERENCE (26) |
| 7 | Global `app` OkHttp client; per-request cookies; WebView `CookieManager` global; nicehttp jar not verified. | VERIFIED; nicehttp NOT VERIFIED |

### Unanswerable from source in this repo
- The nicehttp cookie jar and request timeout defaults (external library).
- Whether individual extensions fetch episodes lazily or handle seasons internally (only their dex would show it, and running it is out of scope).
- Real timeout behaviour on blocking network calls (needs a run, M6).
- Any value that depends on the extension running in a device or emulator (all of M6).

All rows above remain **NOT TESTED** at run time (`COMPATIBILITY_MATRIX.md`).
