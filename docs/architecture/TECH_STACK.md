# HUSH — Tech stack

Status: PROPOSED. Versions are PROVISIONAL and UNVERIFIED. They were chosen without access to Google Maven or Gradle distribution servers (both blocked by the build session's egress policy), so none has been resolved.
Source of truth for versions: `gradle/libs.versions.toml`.

## 1. Toolchain (verified on the host, 2026-11-24)

| Item | Result | Verified how |
|---|---|---|
| JDK | OpenJDK 21.0.13 (Ubuntu) at `/usr/lib/jvm/java-21-openjdk-amd64` | `java -version`, `javac -version` |
| Gradle | 8.14.3 at `/opt/gradle` (binary `/opt/gradle/bin/gradle`) | `gradle --version` |
| Gradle wrapper | NOT present. Generation needs `services.gradle.org`, which returns HTTP 403 | `gradle wrapper` run failed |
| Android SDK | NOT present. `ANDROID_HOME` and `ANDROID_SDK_ROOT` unset; no SDK in common paths | filesystem check |
| Google Maven / Gradle plugin portal | Unreachable (HTTP 403 from egress proxy) | `curl` and Gradle plugin resolution |

Consequence: the project cannot be configured, let alone built, in this session. Any build claim requires an environment with network access to `dl.google.com`, `maven.google.com`, `repo1.maven.org` and `services.gradle.org`, plus an Android SDK (platform 35, build-tools).

Recommended local setup once network is available:
- JDK 17 toolchain for the Android modules (AGP 8.x), or keep JDK 21 as the runner with `jvmTarget` 17.
- Regenerate the wrapper with `gradle wrapper --gradle-version 8.14.3` from a project that already resolves, then commit `gradlew`, `gradlew.bat` and `gradle/wrapper/`.

## 2. Build and language
| Concern | Choice | Notes |
|---|---|---|
| Language | Kotlin 2.0.21 (provisional) | K2 compiler, Compose compiler plugin via `org.jetbrains.kotlin.plugin.compose` |
| Build | Android Gradle Plugin 8.7.3 (provisional) | Version catalog in `gradle/libs.versions.toml` |
| compileSdk / targetSdk | 35 | |
| minSdk | 26 (provisional) | Agent 1 must confirm the lowest API the extension runtime needs before this is final |
| Java target | 17 | Set explicitly in every module to avoid mismatched JVM targets |
| Module type | Pure Kotlin/JVM for `:core:domain`, Android library for others | |

## 3. UI
| Concern | Choice |
|---|---|
| UI toolkit | Jetpack Compose, BOM 2024.12.01 (provisional) |
| Design system | Owned by Agent 3 in `:core:designsystem` (Material 3 as base, custom tokens on top) |
| Navigation | Navigation Compose, type-safe routes |
| Images | Coil 3 (planned, not yet added) |
| Lifecycle | `lifecycle-runtime-compose` for `collectAsStateWithLifecycle` (planned) |

Rendering constraints for Liquid Glass (input to Agent 3):
- Real backdrop blur is available from Android 12 (API 31) through `RenderEffect`. Below API 31 use an opaque or semi-opaque surface.
- Blur over video is expensive. The player controls should not stack blur layers over moving video.
- Provide a reduced-transparency setting that replaces blur with opaque surfaces. Its value is stored in DataStore.

## 4. Concurrency
- Kotlin Coroutines 1.9.0 (provisional), `Flow` and `StateFlow` for state.
- No RxJava. No LiveData in new code.

## 5. Playback
- Media3 (ExoPlayer, UI-less session service, HLS and DASH modules). Version to be pinned when `:core:player` is created.
- Subtitles through Media3 `SubtitleConfiguration`. Formats to be confirmed against what the compat layer returns.

## 6. Persistence
| Concern | Choice |
|---|---|
| Structured data | Room (history, favourites, bookmarks, downloads index) |
| Preferences | DataStore Preferences |
| Files (downloads) | App-specific external storage, decided with the downloads feature |

## 7. Networking
- OkHttp for the app's own HTTP needs (planned), shared through `AppContainer`.
- Whether extensions require a specific HTTP client or cookie store is an open question for Agent 1. Do not add Retrofit or Ktor until that is answered.
- Network security configuration and cleartext policy are set in the manifest, not in code.

## 8. Testing
| Level | Tooling |
|---|---|
| JVM unit | JUnit 4 (provisional), kotlinx-coroutines-test |
| Android unit | JUnit 4 with Robolectric if needed (decision pending) |
| Instrumented / UI | Compose UI test, AndroidX test runner (added with the first screen) |

## 9. Explicitly not chosen (for now)
- Hilt or Koin: manual `AppContainer` first (ADR-003).
- Multi-module Gradle convention plugins (build-logic): unnecessary for six modules.
- Any CloudStream library: not added until Agent 1's licence audit is complete.
