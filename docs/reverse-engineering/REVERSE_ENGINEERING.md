# REVERSE ENGINEERING — CloudStream (reconnaissance)

Owner: Agent 1. Reviewer: Claude.
Reference: `https://github.com/recloudstream/cloudstream` at `a28e6468f54cfe420feebd205d3d21d08ed0cde8` (commit date 2026-11-22). Shallow clone in the session scratchpad at `/tmp/claude-0/-home-claude/fb3b8b07-ec76-52e6-b25e-290dc1213f89/scratchpad/cloudstream`. The clone was **not** placed in `/home/claude/hush`.

Status legend: **[V]** read in source at this revision. **[D]** observed in public data or bytes fetched this session. **[I]** inference. **[NV]** not verified. **[NT]** not tested (nothing executed).

## 1. Scope and method

- Static reading of the Kotlin source (521 `.kt` files across `library/`, `app/`, `shared/`). Symbol and call-site tracing with ripgrep.
- Public data: the official extensions index and five published `.cs3` extensions. Hashes were verified and each `classes.dex` string table was surveyed. No extension was loaded or run.
- No build, no emulator, no device. The Android SDK is not installed (`ANDROID_HOME` unset) and Maven Central is refused by the agent proxy (HTTP 403). Nothing in this document is a runtime result.

## 2. Repository structure [V]

| Path | Contents |
|---|---|
| `settings.gradle.kts` | Includes `:app`, `:shared`, `:library`, `:docs`, `:desktopApp` |
| `app/` | Android application. Main UI (Fragments and ViewModels), `plugins/` (loader and repository manager), `ui/`, `services/`, `receivers/`, `syncproviders/`, `subtitles/`, `network/` |
| `library/` | The extension SDK and most provider-facing logic. KMP source sets: `commonMain`, `jvmCommonMain`, `androidMain`, `jvmMain`, `webMain`. Holds `MainAPI.kt` (2,858 lines), `utils/ExtractorApi.kt`, `plugins/BasePlugin.kt`, `extractors/` (about 100 extractors), `metaproviders/` (TMDB, Trakt, MyDramaList, sync redirectors) |
| `shared/` | KMP module (package `com.lagradost.cloudstream4`), including `viewmodels/ExtensionsViewModel2.kt`. Migration target for Compose Multiplatform |
| `desktopApp/` | Desktop target |
| `docs/` | Dokka build only (`docs/build.gradle.kts`) |
| `COMPOSE.md` | Migration guide: MVI instead of MVVM, KMP-compatible libraries only |

Key observation [V]: the **extension SDK** lives in `library/` under package `com.lagradost.cloudstream3`. The **extension loader** lives in `app/` under `com.lagradost.cloudstream3.plugins`. Extensions are compiled against `library`; they are loaded by `app`.

## 3. Application entry points [V]

- `app/src/main/AndroidManifest.xml`: `MainActivity` and several receivers and services. Notable permissions: `INTERNET`, `MANAGE_EXTERNAL_STORAGE` (commented "Plugin API"), `REQUEST_INSTALL_PACKAGES`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. `android:usesCleartextTraffic="true"` is set on the application element.
- `MainActivity.kt` (in `app/`) calls plugin loading at lines around 334, 1378 and 1397.
- `services/SubscriptionWorkManager.kt` (lines 131-132) also loads extensions in the background via WorkManager.
- The global HTTP client `app` is defined in `library/src/commonMain/.../MainActivity.kt` line 30 as `var app = Requests(responseParser = jsonResponseParser)` (nicehttp). Extensions reach it through `MainActivityKt`.

## 4. Traced workflow: add repository -> discover -> load -> search -> load details -> extract streams

Every step below is a call path read from source. None was executed. Symbol names are given so they can be found again.

### 4.1 Add a repository [V]

1. UI: `app/.../ui/settings/extensions/` (`PluginsFragment`, `PluginsViewModel`, `AndroidRepositoryManager`). Repository model `RepositoryData` is defined in `ExtensionsViewModel.kt` line 22.
2. `RepositoryManager.parseRepoUrl(url)` (`app/.../plugins/RepositoryManager.kt`) normalises the input. Accepted forms: `http(s)://`, `cloudstreamrepo://`, `https://cs.repo/`, and bare short codes. Short codes are resolved through third-party redirectors `cutt.ly` and `py.md`. This is a privacy and integrity concern (see `EXTENSION_ARCHITECTURE.md` §5).
3. `RepositoryManager.addRepository(repository)` stores the entry under `REPOSITORIES_KEY` in CloudStream's key-value settings store. Duplicates are replaced by URL.

### 4.2 Discover extensions [V]

4. `RepositoryManager.getRepoPlugins(repositoryData)`:
   - `parseRepository(url)`: `app.get(convertRawGitUrl(url), cacheTime = 15 min).parsedSafe<Repository>()`. The `Repository` manifest has `name`, `description`, `manifestVersion`, `pluginLists`, `iconUrl`.
   - For each URL in `pluginLists`, `parsePlugins(url)` returns `Array<SitePlugin>`. A failed list is logged (`logError`) and yields an empty list, so one bad list does not fail the repo.
   - Each `SitePlugin` is wrapped as `PluginWrapper(repository, repositoryData, plugin)`.
5. `convertRawGitUrl` rewrites `raw.githubusercontent.com` to `cdn.jsdelivr.net/gh/...` when a settings key is enabled (`jsdelivr_proxy_key`). This is a user-facing network routing choice.
6. `manifestVersion` is parsed but its comment says "Take manifestVersion and such into account later": **not enforced** [V].

### 4.3 Install an extension [V]

7. `PluginManager.downloadPlugin(activity, pluginUrl, pluginHash, internalName, repositoryUrl, loadPlugin)` (`app/.../plugins/PluginManager.kt` ~line 757). The destination is `getPluginPath` -> `filesDir/Extensions/<sanitised repo url>/<fileName>.cs3` (lines ~747-754).
8. `downloadPluginToFile` (RepositoryManager.kt): downloads to a temp file in `cacheDir`. If `pluginHash` is non-null, it computes `sha256-<hex>` and throws `IllegalStateException` on mismatch. It then atomically moves the temp file into place, falling back to a non-atomic move.
9. If `loadPlugin` is true, `unloadPlugin(path)` runs first, then `loadPlugin(...)`.
10. Verification note: the SHA-256 check only applies when the index supplies `fileHash`. The index and the `.cs3` come from the same repository, so the hash is integrity against corruption, not authenticity (see `EXTENSION_ARCHITECTURE.md` §5).

### 4.4 Load an extension (class loading) [V]

11. `PluginManager.loadPlugin(context, file, data)` (`PluginManager.kt` lines 593-687):
    - Marks the file read-only (`file.setReadOnly()`). A code comment (around line 528) notes Android 14+ throws `SecurityException` otherwise.
    - `PathClassLoader(filePath, context.classLoader)`. **The extension's parent class loader is the whole app class loader.**
    - Reads `manifest.json` via `loader.getResourceAsStream("manifest.json")` and parses it into `BasePlugin.Manifest` (`name`, `version`, `pluginClassName`, `requiresResources`).
    - `loader.loadClass(manifest.pluginClassName)` then `getDeclaredConstructor().newInstance()` cast to `BasePlugin`.
    - If `requiresResources`, builds an `AssetManager` and calls `addAssetPath` (reflection on a hidden method).
    - Stores the instance in three maps: `plugins[filePath]`, `classLoaders[loader]`, `urlPlugins[url ?: filePath]`.
    - Calls `load(context)` (for `Plugin`) or `load()` (for `BasePlugin`).
    - Any `Throwable` is caught, logged, shown as a toast, and `false` is returned. A failed extension does not crash the app in this path.
12. `BasePlugin` (`library/.../plugins/BasePlugin.kt`) provides `registerMainAPI(element: MainAPI)` (sets `sourcePlugin`, adds to `APIHolder.allProviders`, calls `addPluginMapping`) and `registerExtractorAPI(element: ExtractorApi)` (adds to `extractorApis`).
13. `Plugin` (`app/.../plugins/Plugin.kt`) extends `BasePlugin` with `load(context: Context)`, `resources`, `openSettings`, and `registerVideoClickAction`.
14. `CloudstreamPlugin` (library) is an annotation. The loader does **not** check for it; only `manifest.pluginClassName` is used [V].

### 4.5 Provider registration and listing [V]

15. `APIHolder` (`library/.../MainAPI.kt` ~lines 110-170) holds `allProviders`, `apis`, `apiMap`, and lookup helpers `getApiFromNameNull`, `getApiFromUrlNull`. Filtering by user settings is in the UI layer and was not traced further.

### 4.6 Search [V]

16. `ui/search/SearchViewModel.kt` line ~235: for each active provider, calls `quickSearch(query)` or `search(query, 1)`. Results go through `ui/APIRepository.kt`.
17. `APIRepository.search` (`ui/APIRepository.kt` ~line 120): empty query returns empty. Otherwise `safeApiCall { withTimeout(getTimeout(api.searchTimeoutMs)) { api.search(query, page) ?: throw ErrorLoadingException() } }`.
18. The extension returns `SearchResponseList` (or `List<SearchResponse>` via `newSearchResponseList`). Paging is driven by `hasNext`.

### 4.7 Load media details [V]

19. `APIRepository.load(url)` (`ui/APIRepository.kt` ~lines 95-120): a rolling cache of `SavedLoadResponse` (`CACHE_SIZE`, `cacheIndex`), a timeout from `api.loadTimeoutMs`, then `api.load(fixedUrl)`. A null result throws `ErrorLoadingException`.
20. The extension returns a `LoadResponse`. Known subtypes in `library/.../MainAPI.kt`: `MovieLoadResponse` (~2461), `TvSeriesLoadResponse` (~2673), `AnimeLoadResponse` (~2318), `LiveStreamLoadResponse`, `TorrentLoadResponse`. Episodes are `Episode` (~2550) with `data: String` (an opaque per-episode URL or token), `name`, `season`, `episode`, `posterUrl`, `score`, `description`, `date`, `runTime`, `rating`.

### 4.8 Episode loading and stream extraction [V]

21. Episode `data` is passed back as the `data` argument of `loadLinks`.
22. `APIRepository.loadLinks(data, isCasting, subtitleCallback, callback)` (~line 200): rejects invalid data via `isInvalidData` (provider-side cleanliness check), then `withTimeout(getTimeout(api.loadLinksTimeoutMs)) { api.loadLinks(...) }`. Exceptions are logged and become `false`.
23. The extension's `loadLinks` emits zero or more `ExtractorLink` values through `callback` and zero or more `SubtitleFile` values through `subtitleCallback`. It returns `Boolean`.
24. Generic host-side extraction: `loadExtractor(url, referer, subtitleCallback, callback)` (`library/.../utils/ExtractorApi.kt` ~line 930). It unshortens the link, strips schemes, and iterates `extractorApis` **from last to first**, so a later registration overrides an earlier one. The first `ExtractorApi` whose `mainUrl` prefix matches runs `getUrl(...)`, which returns `List<ExtractorLink>`.
25. `ExtractorLink` (`ExtractorApi.kt` ~line 698) carries `source`, `name`, `url`, `referer`, `quality: Int`, `headers`, `extractorData`, `type: ExtractorLinkType`, `audioTracks`. `ExtractorLinkType` values include `VIDEO`, `M3U8`, `DASH`. Construct via `newExtractorLink` (the direct constructor is deprecated).
26. Player: `ui/player/RepoLinkGenerator.kt` line ~109 and `GeneratorPlayer.kt` (~line 2294, `viewModel.loadLinks()`; `loadExtractorJob` at line ~260 runs `extractorVerifierJob` in the background). Playback is media3 / ExoPlayer, with `UpdatedDefaultExtractorsFactory` and `UpdatedMatroskaExtractor` under `ui/player/`.

Subtitle path: `SubtitleFile` objects arrive via `subtitleCallback`. `SubtitleFile` has a private constructor and is built through factory functions. Fields: `lang`, `url`, `headers`, and `langTag`. Encoding detection uses `juniversalchardet` (see `DEPENDENCY_MAP.md`).

### 4.9 Error handling [V]

- `ErrorLoadingException` (`MainAPI.kt` line 96) is the provider-level "no data" signal.
- `Resource` (sealed class, `library/.../mvvm/ArchComponentExt.kt` line 48) plus `safeApiCall` (line 207) turn exceptions into UI states.
- `MainAPI` exposes per-operation timeouts (`loadLinksTimeoutMs`, `searchTimeoutMs`, `loadTimeoutMs`, `getMainPageTimeoutMs`, `quickSearchTimeoutMs`), so a slow provider cannot block the app.
- `loadPlugin` isolates load-time failures. Runtime failures inside an extension are **not** isolated from the app process (see `EXTENSION_ARCHITECTURE.md` §5).

### 4.10 Other systems (not traced in depth) [V for location only]

| Area | Location | Note |
|---|---|---|
| Subtitles (online) | `app/.../subtitles/`, `library/.../syncproviders/SubtitleAPI.kt` | Not traced |
| Sync / tracking | `library/.../syncproviders/`, `app/.../syncproviders/` (AniList, MAL, Simkl, Kitsu, Trakt) | Credentials via `BuildConfig` from env or `local.properties` |
| Downloads | `library/.../utils/ExtractorApi.kt` (`IDownloadableMinimum`), `app/.../services/` | Not traced |
| Metadata providers | `library/.../metaproviders/` (TMDB, Trakt) | Not traced |
| Persistence | Key-value store via `getKey` / `setKey` (`app/.../utils/` helpers) | Not traced; the exact store is **[NV]** |
| Background work | `services/SubscriptionWorkManager.kt` (WorkManager) | Refreshes extensions |

## 5. Compose migration status [V]

- `COMPOSE.md` describes the plan: MVI, KMP-compatible libraries only, UI refresh without losing features.
- `shared/` holds the Compose Multiplatform code (`com.lagradost.cloudstream4`).
- The legacy UI is still Fragment and ViewModel based (`app/.../ui/`). Migration is partial. Exact coverage is **[NV]**.

## 6. What this reconnaissance does not establish

- No CloudStream behaviour has been observed at run time.
- The exact method signatures used by extensions (needs `dexdump` or `baksmali`).
- The ABI of `loadLinks` (no sample extension implements it).
- Whether the plugin loader is derived from Aliucord (see `LICENSING_AUDIT.md` §2.1).

## 7. Sources

- Local clone at the scratchpad path above, commit `a28e6468`.
- `https://raw.githubusercontent.com/recloudstream/extensions/master/repo.json` and `README.md`
- `https://raw.githubusercontent.com/recloudstream/extensions/builds/plugins.json` and the five `.cs3` files it lists (hash-verified, read as archives only).
