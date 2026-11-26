# Android platform and core-app parity audit (Phase 1)

- Date: 2026-11-26
- Time: 05:38 UTC
- HUSH audited: `fix/root-theme-wrap` @ `887cdea78a7413ba9a4ce40f6aa0b767ad7df50c` (`/home/claude/hush`)
- Reference: NuvioMobile-Enhanced @ `7aca5a01896cfdf244f39660dd4d2c7e685b7dbb` (`/home/claude/upstream-nuvio`)
- Scope: read-only. No code was edited, committed or pushed.
- Build evidence: none. The sandbox has no Android SDK. All build and runtime statements below are UNVERIFIED unless marked otherwise.

Classifications: TAKE, ADAPT, ALREADY IMPLEMENTED, SKIP, NEEDS INVESTIGATION. Two more labels are used for items the owner must decide: NEEDS OWNER DECISION.

Path shorthand: `H:` = `/home/claude/hush/`, `N:` = `/home/claude/upstream-nuvio/`.

## 1. Manifest and permissions

| # | Item | HUSH | Nuvio | Class | Reason |
|---|---|---|---|---|---|
| 1 | Permissions | `H:app/src/main/AndroidManifest.xml`: INTERNET only | `N:androidApp/src/main/AndroidManifest.xml`: INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, POST_NOTIFICATIONS, FOREGROUND_SERVICE(+MEDIA_PLAYBACK), USE_BIOMETRIC | ADAPT | Add ACCESS_NETWORK_STATE only when the connectivity guard (item 18) lands. Do not copy the rest; each needs a feature. |
| 2 | allowBackup | `false` | `false` | ALREADY IMPLEMENTED | Same policy. Keeps device-local tokens and history out of backups. No backup rules needed. |
| 3 | Launch theme | `android:theme="@android:style/Theme.Material.Light.NoActionBar"`, Compose `HushTheme` applied after first frame | Dedicated `Theme.Nuvio.Splash` per launcher alias | ADAPT | A light platform theme shows before Compose draws a dark UI. Add a splash theme with the canvas colour (androidx core-splashscreen or a window background). Visual check needed on device (UNVERIFIED). |
| 4 | Activity config and PiP | No `configChanges`, no `supportsPictureInPicture` | `configChanges`, `supportsPictureInPicture=true`, `onUserLeaveHint` hook | NEEDS INVESTIGATION | Rotation recreates the activity. Playback state survives through `rememberSaveable` in `H:app/.../ui/HushApp.kt`, but the ExoPlayer is rebuilt. Verify rotation during playback on device. PiP is a playback-role decision. |
| 5 | Application class and container scope | `MainActivity` owns `AppContainer` via `by lazy` (`H:app/src/main/kotlin/app/hushstream/MainActivity.kt`, `AppContainer.kt`) | `NuvioApplication` class declared in manifest | ADAPT | Move the container to an Application class so repositories, OkHttp client and any future worker or service share one instance across activity recreation. Depends on the namespace decision (section 6). |
| 6 | Lifecycle hooks in player | `H:core/player/.../HushVideoPlayer.kt` observes lifecycle (pause on background) and releases ExoPlayer in `DisposableEffect` | `onUserLeaveHint`, PiP manager, `onDestroy` cleanup | ALREADY IMPLEMENTED | Basic pause and release exists. PiP and foreground service are items 4 and 22. |

## 2. Application lifecycle and process state

| # | Item | HUSH | Nuvio | Class | Reason |
|---|---|---|---|---|---|
| 7 | Restore after process death | `rememberSaveable` for tab, detail id, play request, start position; login email is saved, password is not (`H:app/.../ui/login/LoginScreen.kt`) | Not audited in depth | ALREADY IMPLEMENTED | Good default. Password is correctly kept out of saved state. |
| 8 | Session restore at launch | `HushApp` decides Login or Main from `authRepository.currentSession()` or guest flag, in a `LaunchedEffect` (`H:app/.../ui/HushApp.kt`) | Startup gating in `AppGate.kt` | ALREADY IMPLEMENTED | Same concept. HUSH shows a blank canvas while checking, which is acceptable. |
| 9 | Coroutine scopes | `viewModelScope` in ViewModels; `withContext(Dispatchers.IO)` in repositories | Kotlin coroutines via KMP modules | ALREADY IMPLEMENTED | Standard pattern. |

## 3. Storage

| # | Item | HUSH | Nuvio | Class | Reason |
|---|---|---|---|---|---|
| 10 | Device-local JSON stores | SharedPreferences JSON, one file per store: `hush_library`, `hush_history` (capped at 200), `hush_addons`, `hush_app` (guest flag). Mutex-guarded in `H:core/data/.../LocalLibraryRepository.kt`, `LocalWatchHistoryRepository.kt` | SharedPreferences per feature, JSON payload keyed by profile id (`N:composeApp/src/androidMain/.../WatchProgressStorage.android.kt`, and similar in `features/*/Storage.android.kt`) | ALREADY IMPLEMENTED | Same approach. Nuvio does not use Room, DataStore or SQLDelight (checked: no matches in the version catalog or source). |
| 11 | Room database | Not used. `H:core/data/.../DataLayer.kt` says Room and DataStore are "planned" and nothing is wired | Not used | SKIP | Not warranted at current size. Revisit when a downloads index, paged history or a query-heavy feature appears. Known limits of the current JSON store: whole-list rewrite on every write, and the mutex only covers one process. Record the threshold in `DECISIONS.md` if Room is adopted later. |
| 12 | Secure token storage | `H:core/data/.../remote/SecureTokenStore.kt`: AES-256-GCM key in Android Keystore, ciphertext in `hush_secure`, legacy plain `hush_auth` cleared on init | Not checked | ALREADY IMPLEMENTED | HUSH side is sound by inspection. Decrypt failure returns null and the user is silently treated as signed out, which is acceptable but should be logged as a reason code. Nuvio comparison UNVERIFIED. |
| 13 | Settings persistence | None. The Settings screen only shows the signed-in email (`H:app/.../ui/settings/SettingsScreen.kt`). The `HushTheme.kt` comment refers to a DataStore key `ui.glass_mode` that does not exist in code | Per-feature typed storage plus host tests (`N:composeApp/src/androidHostTest/.../*StorageTest.kt`) | ADAPT | Add a small typed preferences layer when the first real setting lands. Fix the stale DataStore comment in the same change. |
| 14 | Storage tests | `H:core/data` has tests for demo repo and sync merge only. No test for the local stores | Host tests for storage classes | ADAPT | Add round-trip and malformed-row tests for `LocalLibraryRepository` and `LocalWatchHistoryRepository`. Needs Robolectric or an Android host test setup (UNVERIFIED that the catalog has it). |
| 15 | Sign-out data policy | `HushApp.kt` on sign-out resets guest flag and returns to Login. Local library and history are not cleared or scoped per account | Local account data cleaner exists (`N:composeApp/.../LocalAccountDataCleanerTest.kt`) | NEEDS OWNER DECISION | Already listed as pending in PROJECT_STATUS (preserve device-local guest data, history and downloads). Do not implement until decided. |

## 4. Background work, notifications, services

| # | Item | HUSH | Nuvio | Class | Reason |
|---|---|---|---|---|---|
| 16 | Notifications (episode release, downloads) | None. No POST_NOTIFICATIONS, no channel | `N:composeApp/.../features/notifications/EpisodeReleaseNotificationPlatform.android.kt` (channel, runtime permission, alarms) | SKIP | No feature in HUSH needs them yet. |
| 17 | Exact alarms | None | `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, boot receiver (`N:composeApp/src/androidMain/AndroidManifest.xml`) | SKIP | Adds Play policy and battery cost. If a reminder feature is wanted, prefer WorkManager with inexact timing. |
| 18 | WorkManager | Not in the version catalog or any module | `work-runtime-ktx` 2.10.3 for downloads and reminders | SKIP | Not needed until sync-on-reconnect or downloads. When needed, use WorkManager with a network constraint. |
| 19 | Foreground media service and media session | None | `PlayerNowPlayingService` (`foregroundServiceType=mediaPlayback`), media3-session | NEEDS INVESTIGATION | Lock-screen and background-audio controls. Owned by the Playback role. Android 14+ requires a declared foreground service type if this is added. |
| 20 | Downloads and APK installer | None. Download and ADR-019 licence question pending | `DownloadsTransferWorker`, `REQUEST_INSTALL_PACKAGES` in the full flavor (self-update) | SKIP | Self-installing APKs are a security surface HUSH should not copy. Downloads wait for the ADR-019 decision. |
| 21 | Crash and error reporting | None | Sentry, auto-init disabled (`io.sentry.auto-init=false`) | SKIP | Crash reporting is a privacy and owner decision. If adopted, keep auto-init off and send no account data. |

## 5. Navigation, deep links, back handling, connectivity

| # | Item | HUSH | Nuvio | Class | Reason |
|---|---|---|---|---|---|
| 22 | Deep links and auth callbacks | None. Supabase auth is email and password, so no redirect is needed | `nuvio://auth/*`, `stremio://`, verified `https://nuvio.tv/party/` (`N:androidApp/src/main/AndroidManifest.xml`, `LinkActivity.kt`, `MainActivity.kt` `onNewIntent`) | SKIP | Not needed for the current auth flow. Revisit for addon install links. |
| 23 | Third-party URL schemes | None | Claims `stremio://` | SKIP | Claiming another app's scheme can conflict with that app. Do not copy. |
| 24 | Back handling | `BackHandler` in `H:app/.../ui/HushApp.kt` (three handlers in priority order: player, detail, sources) and in `H:core/player/.../HushVideoPlayer.kt` | `PlatformBackHandler` abstraction (`N:composeApp/src/commonMain/.../PlatformBackHandler.kt`) | ALREADY IMPLEMENTED | HUSH's ordering is correct for its state model. Ordering depends on the `enabled` flags being mutually exclusive; keep that invariant in review. |
| 25 | Predictive back opt-out | Not set (default behaviour) | `android:enableOnBackInvokedCallback="false"` | SKIP | Nuvio sets this as a workaround. HUSH should test predictive back instead of disabling it (UNVERIFIED). |
| 26 | Cleartext traffic | Not set. Default blocks cleartext on targetSdk 35. No network security config | `cleartextTrafficPermitted="true"` in base config (`N:composeApp/src/androidMain/res/xml/network_security_config.xml`) | SKIP | Nuvio weakens transport security. HUSH keeps the secure default. HUSH addon and Supabase calls are HTTPS-only (`StremioClient`, `PrefsAddonRegistry.normalise` requires https). |
| 27 | Connectivity and unmetered detection | No ConnectivityManager use. Offline errors surface per call only | `DownloadNetworkGuard` uses `registerDefaultNetworkCallback` and `NET_CAPABILITY_NOT_METERED` (`N:composeApp/src/androidMain/.../DownloadNetworkGuard.android.kt`) | ADAPT | Needed for an offline banner and for gating sync or downloads on unmetered networks. Requires ACCESS_NETWORK_STATE (item 1). Pattern is reusable; Nuvio code is GPL-3.0 and would need attribution if copied. |
| 28 | Network timeouts and retry | `StremioClient` uses connect 10 s and read 15 s (`H:core/data/.../addon/StremioClient.kt`). No retry or backoff found. Supabase client timeouts not verified | Ktor and OkHttp clients (not reviewed in depth) | NEEDS INVESTIGATION | Confirm Supabase client timeouts and whether any retry policy is wanted. |
| 29 | Sync trigger | `SyncEngine` (`H:core/data/.../sync/SyncEngine.kt`) and `RemoteLibraryRepository`, `RemoteWatchHistoryRepository` exist. A grep of `app/` and `core/` finds no caller of `SyncEngine(`, `syncNow` or the remote repositories | Not applicable | NEEDS INVESTIGATION | Looks unwired. Sync never runs from the UI, at sign-in, on foreground or on reconnect. Confirm on the branch, then decide the trigger points. |

## 6. Build, SDK levels and CI

| # | Item | HUSH | Nuvio | Class | Reason |
|---|---|---|---|---|---|
| 30 | Version catalog | `H:gradle/libs.versions.toml` present, used by all modules | `N:gradle/libs.versions.toml` | ALREADY IMPLEMENTED | Same mechanism. Versions are marked UNVERIFIED in the file. |
| 31 | Dependency versions | AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Media3 1.5.1, Lifecycle 2.8.7, OkHttp 4.12.0, coroutines 1.9.0. Marked "UNVERIFIED: not checked against Maven" | AGP 9.4.0, Kotlin 2.4.10, Media3 1.8.0, Ktor 3.4.1, Work 2.10.3 | NEEDS INVESTIGATION | Only CI can confirm resolution. Ask CI to run a dependency resolve and record the result. |
| 32 | Gradle wrapper | Missing. No `gradlew`, no `gradle/wrapper/` in `H:`. CI installs Gradle 8.14.3 through `setup-gradle` | Present (`N:gradlew`, `gradle/wrapper`) | ADAPT | Add the wrapper so local and CI builds use the same Gradle. |
| 33 | CI build steps | `H:.github/workflows/android.yml` runs `assembleDebug` only, uploads the debug APK. No unit tests, no lint, no release build | `N:.github/workflows/pr-full-debug-build.yml`, `pr-template-check.yml`, `mobile-release.yml` | ADAPT | Add `testDebugUnitTest` and `lint` to CI. `core:data` tests currently never run in CI. |
| 34 | CI secrets handling | Writes `local.properties` from secrets; warns when empty. Keys are read via `BuildConfig` in `core:data` | Not reviewed in depth | ALREADY IMPLEMENTED | Reasonable. The publishable key is client-safe; keep it out of logs. |
| 35 | Release build | `isMinifyEnabled = false`, no signing config (`H:app/build.gradle.kts`) | Minify switch via property, release signing | NEEDS OWNER DECISION | Shrinking, signing and keystore ownership need an owner decision before any release build is claimed. |
| 36 | minSdk | 26 (`H:app/build.gradle.kts`, `core/data/build.gradle.kts`) | 24 | NEEDS OWNER DECISION | Do not decide here. Affects Keystore AES-GCM use (available from 23), media3 minimum and device reach. |
| 37 | targetSdk and compileSdk | targetSdk 35, compileSdk 35 | targetSdk 36, compileSdk 37 | NEEDS OWNER DECISION | Play target-level policy and the timing of predictive-back and edge-to-edge changes. |
| 38 | namespace and applicationId | `app.hushstream` (placeholder, D-002) | `com.nuvio.android` namespace, `com.nuvio.media` applicationId | NEEDS OWNER DECISION | Blocks items 5 and 3 (launcher alias and splash naming). Rename cost in ADR-004. |
| 39 | Launcher icon | No `android:icon` or `roundIcon` in `H:app/src/main/AndroidManifest.xml`. The app shows the platform default icon | Icon resources and launcher alias variants (`N:composeApp/src/androidMain/AndroidManifest.xml`) | ADAPT | Set an icon before any build is shown to anyone. Alias variants are optional and need an owner design decision. |
| 40 | Media3 modules | `H:core/player/build.gradle.kts` depends on `media3-exoplayer` and `media3-ui` only. HLS and DASH need separate modules | `media3-exoplayer-hls`, `-dash`, `-smoothstreaming`, `-rtsp`, datasource-okhttp | ADAPT | Playback role to confirm which stream types HUSH must play. Add the modules needed (UNVERIFIED: not built). |
## 7. Items needing owner decision (not decided here)

- Namespace and applicationId (item 38). Blocks item 5 and item 39 naming.
- minSdk (item 36).
- targetSdk and compileSdk (item 37).
- Release minify and signing (item 35).
- Sign-out data policy for guest, history and downloads (item 15).
- Crash reporting, if any (item 21).
- Downloads licence question ADR-019 (item 20).

## 8. Counts (items 1 to 40, one class each)

- TAKE: 0
- ADAPT: 10 (items 1, 3, 5, 13, 14, 27, 32, 33, 39, 40)
- ALREADY IMPLEMENTED: 10 (items 2, 6, 7, 8, 9, 10, 12, 24, 30, 34)
- SKIP: 10 (items 11, 16, 17, 18, 20, 21, 22, 23, 25, 26)
- NEEDS INVESTIGATION: 5 (items 4, 19, 28, 29, 31)
- NEEDS OWNER DECISION: 5 (items 15, 35, 36, 37, 38)

Total: 40 rows (items 1 to 40). Item 40 is the Media3 modules row.

## 9. Top gaps

1. Sync is not wired (item 29). Remote sync classes have no caller, so Supabase sync cannot run from the UI. Confirm and decide trigger points.
2. CI proves only that the debug APK assembles, and that is UNVERIFIED because no build has been run. Unit tests and lint never run, the Gradle wrapper is missing, and dependency versions are unchecked (items 31, 32, 33).
3. First impression: the launch theme is light before Compose draws (item 3), the app has no launcher icon (item 39), and the container is tied to the activity rather than the application (item 5).

## 10. Verification status

- Static reading only. No Gradle, lint, unit test or device run was performed.
- Every ADAPT and NEEDS INVESTIGATION item is UNVERIFIED until a build or device run confirms it.
- Nuvio items marked "not reviewed in depth" were not checked and are not classified.
