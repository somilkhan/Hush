# Architecture parity audit: HUSH vs NuvioMobile-Enhanced

- Date: 2026-11-26, 11:07 +05:30
- HUSH audited at commit `887cdea` (branch `fix/root-theme-wrap`, per the owner's brief)
- Reference: NuvioMobile-Enhanced at commit `7aca5a0` (GPL-3.0), `/home/claude/upstream-nuvio`
- Method: read-only static review of build files, manifests, and the source files named below. No build, no test run, no device run.
- Build status: UNVERIFIED (no Android SDK in sandbox; CI is the only build evidence, and HUSH's own docs mark it UNVERIFIED).
- Runtime behaviour of any HUSH item: UNVERIFIED (not run on a device).
- Out of scope for initial release (Party, Discord, Trakt, Live TV, and the tracker/Simkl/AniList folders that go with them): classified SKIP, not investigated in depth.

## Legend

- TAKE: adopt the Nuvio approach as-is (none chosen in this audit; the Nuvio code is GPL-3.0 and would need attribution and origin notes).
- ADAPT: adopt the idea, re-implemented to HUSH's structure and visual identity.
- ALREADY IMPLEMENTED: HUSH has an equivalent.
- SKIP: not needed for initial release, or conflicts with HUSH's principles.
- NEEDS INVESTIGATION: a decision or a deeper read is required first.

Paths: `N:` = Nuvio under `composeApp/src/commonMain/kotlin/com/nuvio/app/` unless shown otherwise. `H:` = HUSH under `/home/claude/hush/`.

## 1. Build and structure

| # | Area | Nuvio (reference) | HUSH | Class | Reason |
|---|---|---|---|---|---|
| 1 | Module topology | `settings.gradle.kts`: one Kotlin Multiplatform `composeApp` plus `androidApp`, `baselineprofile` | `settings.gradle.kts`: `:app`, `:core:domain` (JVM), `:core:data`, `:core:player`, `:design` (Android) | SKIP | Nuvio is KMP for iOS, which is out of HUSH's Android-only scope. HUSH's split keeps `core:domain` free of Android deps (ADR-005). Keep HUSH's layout. |
| 2 | Version pins | `gradle/libs.versions.toml`: Kotlin 2.4.10, Media3 1.8.0, Ktor 3.4.1, Navigation3 1.2.0, supabase-kt 3.4.1 | `gradle/libs.versions.toml`: Kotlin 2.0.21, Media3 1.5.1, Compose BOM 2024.12.01, comments say versions are provisional and UNVERIFIED | ADAPT | Do not copy Nuvio's pins blindly. Pick versions HUSH's CI resolves, and record the choice in DECISION_LOG. |
| 3 | Feature packaging | `N:features/<feature>/` (about 40 folders, each with its own repo, storage and UI) | `H:app/.../ui/<screen>/` (home, details, library, search, login, settings, sources) | ADAPT | Adopt per-feature folders as the screen count grows. Not needed at current size. |

## 2. Dependency injection and composition

| # | Area | Nuvio | HUSH | Class | Reason |
|---|---|---|---|---|---|
| 4 | Composition root | No DI framework in `libs.versions.toml`. Wiring is through `object` singletons. | `H:app/.../AppContainer.kt`, manual container (ADR-003, D-005) | ALREADY IMPLEMENTED | HUSH has one explicit composition root, which is easier to follow than Nuvio's singleton graph. |
| 5 | Global singleton repositories | 69 `object *Repository` declarations, e.g. `N:features/watched/`, `N:features/collection/` | Constructor-injected repositories (`H:core/data/.../LocalLibraryRepository.kt`) | SKIP | Singletons make tests harder. Keep constructor injection. |
| 6 | ViewModel construction | Not inspected | `H:app/.../ui/common/ViewModelFactory.kt` (`viewModelFactoryOf`) | NEEDS INVESTIGATION | HUSH's lambda factory works for now. Nuvio's approach was not read, so no comparison is made. |

## 3. Navigation

| # | Area | Nuvio | HUSH | Class | Reason |
|---|---|---|---|---|---|
| 7 | Typed routes and back stack | `N:navigation/Routes.kt` (`@Serializable sealed interface AppRoute : NavKey`), `NavDisplay` with `rememberNavBackStack` in `N:MainAppContent.kt` | `H:app/.../ui/HushApp.kt`: state-based gate (login, main), tabs, then details, sources, player through `remember` state | ADAPT | HUSH's own ARCHITECTURE.md §5 says Navigation Compose with type-safe routes, but the code does not do this. Decide which is true. Typed routes are needed for back-stack restore and deep links. |
| 8 | Deep links | `N:core/deeplink/` | None | NEEDS INVESTIGATION | Depends on whether HUSH needs external links (for example, opening a title from a browser). Owner decision. |
| 9 | Screen transitions and poster artwork | `N:navigation/PosterNavigation*.kt` (shared-element motion) | None | SKIP | D-009 keeps structure only, not visuals. Motion is a design decision for Agent 3, not an architecture item. |
| 10 | Full-screen player chrome | `N:navigation/Routes.kt`: `AppRoute.hidesNavigationBar` | `HushApp.kt`: player replaces the tab shell when `request != null` | ALREADY IMPLEMENTED | HUSH gets the same effect from state, without a route flag. |

## 4. Data layer and persistence

| # | Area | Nuvio | HUSH | Class | Reason |
|---|---|---|---|---|---|
| 11 | Repository ports and local adapters | Per-feature repositories, e.g. `N:features/watchprogress/` | Ports in `H:core/domain/.../repository/MediaRepository.kt`, `.../sync/LibraryRepository.kt`, `.../sync/WatchHistoryRepository.kt`; local adapters in `H:core/data/.../Local*Repository.kt` | ALREADY IMPLEMENTED | Matches D-010 "sync-ready" requirement: each store has a port and a local adapter. |
| 12 | Local persistence | Per-feature storage via `expect object` (e.g. `N:features/watchprogress/WatchProgressStorage.kt`, `N:core/storage/AppFileCache.kt`) | JSON in `SharedPreferences`: `H:core/data/.../LocalLibraryRepository.kt`, `LocalWatchHistoryRepository.kt`, `PrefsAddonRegistry.kt` | ALREADY IMPLEMENTED | Works for an Android-only app. Keep until the Room decision (item 13) is made. |
| 13 | Room and DataStore | No Room or DataStore found in `commonMain` | ARCHITECTURE.md §7 and ADR-010 require Room (history, favourites, downloads index) and DataStore (settings). Code has neither (`H:core/data/.../DataLayer.kt` is a placeholder). | NEEDS INVESTIGATION | Doc and code disagree. Downloads (item 29) need a Room index. Owner decision: adopt Room now, or amend ADR-010. |
| 14 | Profile-scoped keys | `N:core/storage/ProfileScopedKey.kt`, `N:features/profiles/` | None | NEEDS INVESTIGATION | Only relevant if HUSH adds profiles. Not in the current docs. |
| 15 | Secure token storage | `N:features/profiles/ProfileBiometricAuth.android.kt`, `ProfilePinCacheStorage.android.kt` (profile PIN and biometric) | `H:core/data/.../remote/SecureTokenStore.kt`: AES-256-GCM with an Android Keystore key, migrates the old plain-text prefs file | ALREADY IMPLEMENTED | HUSH's design is sound on paper. Runtime behaviour UNVERIFIED (no device run). |
| 16 | Remote client | `N:core/network/SupabaseProvider.kt` (supabase-kt SDK, cached client) | `H:core/data/.../remote/PostgrestClient.kt` and `SupabaseAuthRepository.kt`: hand-written calls on OkHttp | NEEDS INVESTIGATION | ADR-012 makes OkHttp the single HTTP client, which argues for HUSH's approach. Token refresh and realtime are not present in HUSH, and the SDK would supply them. Decide before the sync work grows. |

## 5. Sync

| # | Area | Nuvio | HUSH | Class | Reason |
|---|---|---|---|---|---|
| 17 | Sync engine core | `N:core/sync/SyncManager.kt`, `ProfileSettingsSync.kt`, `ProviderCredentialSync.kt` | `H:core/data/.../sync/SyncEngine.kt`: fetch remote, merge by newer `updatedAt`, push local winners, apply remote winners; `Mutex` serialises runs; guests make no network calls | ALREADY IMPLEMENTED | Fetch-before-push protects old devices. Covers library and history. |
| 18 | Deletions (tombstones) | Not verified in Nuvio | `SyncEngine.kt` header: deletions are not synced; a local delete returns on the next pull | NEEDS INVESTIGATION | This is a data-loss bug from the user's view. Needs a tombstone design before sync goes to users. |
| 19 | Sync on app foreground | `N:core/sync/AppForegroundMonitor.kt` | None | ADAPT | Cheap way to refresh remote state. Add when sync is wired into the UI. |
| 20 | Sync scope: settings and addons | `ProfileSettingsSync.kt`, `ProviderCredentialSync.kt` | `PrefsAddonRegistry.kt` is local only. D-010 requires addons and plugins to sync. | ADAPT | Owner already asked for this scope in D-010. Not built yet. |

## 6. Playback

| # | Area | Nuvio | HUSH | Class | Reason |
|---|---|---|---|---|---|
| 21 | Player engine boundary | `N:features/player/PlayerEngine.kt` (`PlayerEngineController` interface with default no-op methods), `AutoPlaybackEngineResolver.kt`, `AndroidPlaybackEngine` enum in `N:features/player/PlayerModels.kt` | `H:core/player/.../HushVideoPlayer.kt`: one Compose player, Media3 ExoPlayer, takes a URL | ADAPT | Define a narrow controller port in `:core:player` when a second engine is added. HUSH's player has no engine abstraction yet, which is fine for one engine. |
| 22 | Extra engines (mpv, external player, Cast, AIOStreams) | `N:androidMain/features/player/` (mpv, `ExternalPlayerPlatform`, `PlayerCastBridge`) | None | SKIP | Not in HUSH's brief. Revisit if the owner adds them. |
| 23 | Background playback and media session | `N:androidMain/features/player/PlayerNowPlayingController.android.kt`, `BackgroundPlaybackRelease.android.kt` | No `MediaSessionService` in any manifest. ARCHITECTURE.md §6 requires one. | ADAPT | Gap between doc and code. Needed for lock-screen controls and playback that survives backgrounding. |
| 24 | Progress reporting | `N:features/watchprogress/` | `HushVideoPlayer.kt` calls `onProgress` every 5 s and on release; `HushApp.kt` writes history | ALREADY IMPLEMENTED | Matches ARCHITECTURE §6 throttle. Runtime UNVERIFIED. |
| 25 | Subtitles | `N:core/...` `AddonSubtitleLoader.kt`, `OpenSubtitlesHasher.kt`, `N:docs/subtitle-autosync.md` | None | NEEDS INVESTIGATION | Scope decision. Subtitle hashing and auto-sync are a large feature. |

## 7. Providers and extensions

| # | Area | Nuvio | HUSH | Class | Reason |
|---|---|---|---|---|---|
| 26 | Stremio-protocol catalog, meta and streams | `N:features/addons/`, `N:features/streams/` | `H:core/data/.../addon/StremioClient.kt`, `AddonMediaRepository.kt`, `PrefsAddonRegistry.kt`; falls back to `EmptyMediaRepository` | ALREADY IMPLEMENTED | Basic path exists. Nuvio's merge and ranking logic was not inspected. |
| 27 | Plugin and extension runtime | `N:features/plugins/` | None. ADR-008: extension code is not executed until security is analysed. | NEEDS INVESTIGATION | Highest-risk item. CLAUDE.md rule 5 requires a security analysis before running untrusted extension code. Gate this before any port. |
| 28 | Debrid, Plex, P2P, server integrations | `N:features/debrid/`, `N:features/servers/`, `N:features/p2p/` | None | SKIP | Not in HUSH's stated scope. |

## 8. Platform services

| # | Area | Nuvio | HUSH | Class | Reason |
|---|---|---|---|---|---|
| 29 | Downloads | `N:features/downloads/` (`DownloadsPlatformDownloader`, `DownloadNetworkGuard`) | None. ADR-019 PROPOSED: WorkManager and Room index. Licence question open. | NEEDS INVESTIGATION | Blocked on the owner's licence decision (ADR-019) and on Room (item 13). |
| 30 | Image loading | `N:core/poster/`; Coil 3 with Ktor in `libs.versions.toml` | None. Posters are tonal placeholders labelled DEMO. | NEEDS INVESTIGATION | Coil 3 can run on OkHttp, which would fit ADR-012. Check before adding a second HTTP stack. |
| 31 | In-app updater | `N:features/updater/` | None | NEEDS INVESTIGATION | Sideload APK distribution (D-008) may need one. Owner decision. |
| 32 | Startup work | `N:core/startup/StartupPreloader.kt` | `HushApp.kt`: one `LaunchedEffect` checks session or guest. No extension or network work on startup. | ADAPT | HUSH already avoids startup work. Keep this rule as the app grows (ARCHITECTURE §4). |
| 33 | Theme and design tokens | `N:features/settings/ThemeSettingsRepository.kt` | `H:design/` (`HushTheme.kt`, `HushTokens`, `HushColors.kt`) | ALREADY IMPLEMENTED | Visual identity intentionally differs (D-009). |

## 9. Testing and build config

| # | Area | Nuvio | HUSH | Class | Reason |
|---|---|---|---|---|---|
| 34 | Test layout | `commonTest`, `androidHostTest`, `iosTest` source sets | Unit tests only in `H:core/data/src/test/` (2 files). None in `:app` or `:design`. | ADAPT | Add tests for the domain and data layers as features land. |
| 35 | Build-time config and secrets | Not inspected | `H:core/data/build.gradle.kts` reads `local.properties` into BuildConfig; empty values disable sync | ALREADY IMPLEMENTED | Keeps keys out of git. Nuvio side not compared. |

## 10. Out of scope

| # | Area | Nuvio | HUSH | Class | Reason |
|---|---|---|---|---|---|
| 36 | Party, Discord, Trakt, Live TV, and tracker folders (Simkl, AniList, WeTrakr, tracking) | `N:features/party/`, `discord/`, `trakt/`, `livetv/`, `simkl/`, `anilist/`, `wetrakr/`, `tracking/` | None | SKIP | Out of scope for initial release. Not investigated in depth. |

## Summary

| Classification | Count | Items |
|---|---|---|
| ALREADY IMPLEMENTED | 10 | 4, 10, 11, 12, 15, 17, 24, 26, 33, 35 |
| ADAPT | 9 | 2, 3, 7, 19, 20, 21, 23, 32, 34 |
| SKIP | 6 | 1, 5, 9, 22, 28, 36 |
| NEEDS INVESTIGATION | 11 | 6, 8, 13, 14, 16, 18, 25, 27, 29, 30, 31 |
| TAKE | 0 | none |
| **Total** | **36** | |

Counts are per row. Row 36 is one row covering a group of folders.

## Top blockers

1. **Persistence decision (items 13, 29).** ARCHITECTURE §7 and ADR-010 call for Room and DataStore. The code uses `SharedPreferences`. Downloads cannot be built without a Room index, and ADR-019 is blocked on the owner's licence question. Owner decision needed.
2. **Navigation model drift (item 7).** ARCHITECTURE §5 says Navigation Compose. `HushApp.kt` is state-based with no back-stack restore and no deep links. Settle this before more screens are added.
3. **Playback gaps (items 21, 23).** No media session or background playback, despite ARCHITECTURE §6. One engine, no port. Nothing in `core:player` has run on a device (UNVERIFIED).

Also flagged, not in the top three: **item 27** (plugin runtime) must not start until the security analysis required by ADR-008 and CLAUDE.md rule 5 exists. **Item 18** (no tombstones) can lose user deletions once sync is live.

## Memory note

Nothing from the user's memory was used in this audit. The brief and the project docs in the repo were the only inputs.
