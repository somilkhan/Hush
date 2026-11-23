# DEPENDENCY MAP — CloudStream

Owner: Agent 1. Reference revision: `a28e6468f54cfe420feebd205d3d21d08ed0cde8` (2026-11-22).
Status legend: **[V]** read in source at this revision. **[D]** observed in public data or bytes fetched this session. **[I]** inference. **[NV]** not verified.

## 1. Build toolchain [V]

From `gradle/libs.versions.toml`, `app/build.gradle.kts`, `library/build.gradle.kts`:

| Item | Value |
|---|---|
| Gradle wrapper | 8.14.3 on this host (`gradle -v`) [V for host, D] |
| Android Gradle Plugin | 9.1.1 |
| Kotlin | 2.4.0 |
| Compose Multiplatform / Compose compiler | 1.12.0 / Kotlin plugin |
| compileSdk / targetSdk / minSdk | 37 / 36 / 23 |
| JVM target / toolchain | 11 / 17 |
| App applicationId | `com.lagradost.cloudstream3` |
| Version | versionName 4.9.1, versionCode 68 |
| Modules | `:app`, `:shared`, `:library`, `:docs`, `:desktopApp` (`settings.gradle.kts`) |
| Repositories | google, mavenCentral, mavenLocal, `https://jitpack.io` (JitPack hosts `com.github.recloudstream:*` forks) |

Host state for this session [D]: Java and Gradle present; `ANDROID_HOME` unset; no `adb` or emulator. **No Android build has been run.**

## 2. Runtime dependencies of the CloudStream app

### 2.1 Library module `:library` (the extension SDK surface) [V]

`library/build.gradle.kts`, `commonMain`:

| Dependency | Version | Role |
|---|---|---|
| annotation | 1.10.0 | annotations |
| jackson-module-kotlin | 2.13.1 (strict) | JSON for extension manifests and data |
| jsoup | 1.22.1 | HTML parsing |
| ksoup | 0.2.6 | HTML parsing (KMP port) |
| kotlinx-atomicfu | 0.33.0 | atomics |
| kotlinx-coroutines-core | 1.11.0 | coroutines |
| kotlinx-datetime | 0.8.0 | dates |
| kotlinx-io-core | 0.9.1 | IO |
| kotlinx-serialization-json | 1.11.0 | JSON (`MainAPI.kt` `json` uses `ignoreUnknownKeys = true`) |
| ktor-http | 3.5.0 | HTTP types |
| nicehttp | 0.4.18 | HTTP client used as the global `app` (`library/.../MainActivity.kt` line 30) |
| rhino | 1.8.1 (strict) | JavaScript execution (`getRhinoContext`, `MainAPI.kt`) |
| cryptography-core / provider-optimal | 0.6.0 | crypto |

`jvmCommonMain`: kotlin-reflect, newpipeextractor v0.26.3 (JVM/Android only).

### 2.2 App module `:app` [V]

Declares about 44 `implementation`/`api` entries. Notable: media3 bundle (cast, common, container, datasource-cronet, datasource-okhttp, exoplayer, exoplayer-dash, exoplayer-hls, session, ui), nextlib bundle, coil bundle, Compose bundle, conscrypt-android 2.5.2 (to fix SSL on Android 9), biometric, zipline, work-runtime-ktx, `gson 2.11.0`, `fuzzywuzzy 1.4.0`, torrentserver, anime-db 1.0.2, juniversalchardet, shimmer, palette-ktx, tvprovider, qrcode-kotlin, `project(":library")`, `project(":shared")`.

Pinned for compatibility reasons (comments in the TOML) [V]: `jackson-module-kotlin 2.13.1` ("later versions don't support minSdk < 26"), `rhino 1.8.1` (minSdk), `conscrypt 2.5.2` ("2.5.3 crashes everything"), `coil 3.3.0` ("later require jvmTarget 11+").

## 3. What a `.cs3` extension expects at runtime [D]

Method: static reading of the `classes.dex` string table of the five extensions in the public `builds` index, after hash verification. No extension was loaded or executed. `dexdump` and `baksmali` are not installed, so this is a **class/method-name survey, not a full signature survey**.

Hosted by the app (must exist on the runtime class path for the extension to link):

| Group | Symbols referenced by at least one of 5 extensions |
|---|---|
| CloudStream API | `com/lagradost/cloudstream3/MainAPI` (+`MainAPIKt`), `plugins/BasePlugin`, `plugins/CloudstreamPlugin`, `utils/ExtractorApiKt`, `utils/ExtractorLink`, `utils/ExtractorLinkType`, `utils/Qualities`, `utils/AppUtils`, `utils/StringUtils`, `SearchResponse`, `SearchResponseList`, `MovieSearchResponse`, `LoadResponse`, `MovieLoadResponse`, `TvSeriesLoadResponse`, `LiveStreamLoadResponse`, `LiveSearchResponse`, `HomePageResponse`, `HomePageList`, `MainPageRequest`, `MainPageData`, `TvType`, `SubtitleFile`, `Actor`, `ActorData`, `ActorRole`, `Episode`, `ErrorLoadingException`, `mvvm/ArchComponentExtKt`, `MainActivityKt` (the `app` accessor, via `getApp`) |
| nicehttp | `com/lagradost/nicehttp/Requests`, `ResponseParser`, `NiceResponse` |
| Jackson | `ObjectMapper`, `JsonMapper`, `TypeReference`, `com/fasterxml/jackson/module/kotlin/ExtensionsKt` |
| jsoup | `org/jsoup/select/Elements` (and others) |
| OkHttp | `okhttp3/Interceptor` (for `getVideoInterceptor`) |
| Kotlin stdlib | `kotlin/jvm/functions/*`, `kotlin/collections/*`, `kotlin/jvm/internal/*` |

Provider overrides seen in the dex string table: `getMainPage`, `search`, `load`, `name`, `mainUrl`, `lang`, `hasMainPage`, `supportedTypes`, `mainPage`, `getUrl` (extractors), `registerMainAPI`, `registerExtractorAPI`, `requiresReferer`, `getApp`.

**Not seen:** `loadLinks` is not present as a method name in any of the five extensions. The playback path (`loadLinks`) is therefore **not covered by this sample** and needs a sample extension that implements it.

## 4. Dependency graph (simplified)

```
Extension .cs3 (classes.dex, untrusted)
  -> com.lagradost.cloudstream3 API (MainAPI, BasePlugin, ExtractorApi, responses)   [host must provide]
       -> nicehttp (Requests / app)                                                  [host must provide]
       -> Jackson, jsoup, OkHttp Interceptor, Kotlin stdlib                          [host must provide]
  -> host PathClassLoader parent = context.classLoader (see EXTENSION_ARCHITECTURE.md)
```

## 5. Implications for HUSH

1. A `.cs3` cannot run unless HUSH supplies the CloudStream API names, nicehttp names, Jackson, jsoup, OkHttp and the Kotlin stdlib at runtime. Versions of Jackson / jsoup / Kotlin that the extension was built against are **[NV]**; mismatches would surface as `NoSuchMethodError`, not at build time.
2. Licence consequences of each runtime dependency are in `LICENSING_AUDIT.md`. nicehttp is GPL-3.0.
3. Pinned versions in CloudStream (Jackson 2.13.1, Rhino 1.8.1, Coil 3.3.0, conscrypt 2.5.2) exist for minSdk and JVM target reasons. HUSH's minSdk is not yet decided; these pins constrain it.
4. Dependencies that exist only for CloudStream features (torrentserver, anime-db, fuzzywuzzy, NewPipe trailers, nextlib, tvprovider, biometric) are not needed for extension compatibility and should be evaluated individually by Agent 2.

## 6. Open items

- Obtain exact versions the extensions were compiled against (from the Gradle plugin build output, which is outside this repo) [NV].
- Confirm the Maven artifact coordinates and POMs once `repo1.maven.org` is reachable (currently 403 from the proxy).
- Signature-level scan with `dexdump` or `baksmali` once the Android SDK build-tools are available.
