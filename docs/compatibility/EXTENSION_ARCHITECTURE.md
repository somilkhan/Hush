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
