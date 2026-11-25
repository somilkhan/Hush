# PROJECT_STATUS (verified facts only)

Last updated: 2026-11-24 (post-merge)

- **Project name:** HUSH (working name, not legally cleared).
- **Phase:** Phase 0 (workspace and feasibility), with Agent 2 skeleton and architecture drafts done. Phase 1 reconnaissance for Agents 1 and 3 not confirmed in this file.
- **Repository:** `somilkhan/Hush` (public, GPL-3.0). Attached and pushed. Remote URL: https://github.com/somilkhan/Hush.git
- **Default branch:** `main` (HEAD `1253bb7`, bootstrap merged from `feature/project-bootstrap`).
- **Branch / HEAD:** `main` at `1253bb7`, in sync with origin.
- **Package namespace:** `app.hush.placeholder`. PROVISIONAL (DECISION_LOG D-002). Do not sign, publish or ship under it.

## Toolchain (verified 2026-11-24)
- JDK: OpenJDK 21.0.13 at `/usr/lib/jvm/java-21-openjdk-amd64`. Verified.
- Gradle: 8.14.3 at `/opt/gradle/bin/gradle`. Verified as installed.
- Gradle wrapper (`gradlew`, `gradle/wrapper/`): NOT present. Generation needs `services.gradle.org`, which returns HTTP 403 from the egress proxy.
- Android SDK: NOT present. `ANDROID_HOME` and `ANDROID_SDK_ROOT` unset; not found in common paths.
- Google Maven, Maven Central and Gradle plugin portal: blocked (HTTP 403).

## Build status: UNVERIFIED
- The Gradle skeleton (`settings.gradle.kts`, root and module build scripts, `gradle/libs.versions.toml`) was written but has NEVER been configured or built.
- Verification attempt: `gradle projects` with the system Gradle fails with "Plugin [id: 'com.android.application', version: '8.7.3'] was not found" (plugin repositories unreachable).
- Do not claim the app compiles until a machine with network access and an Android SDK runs `./gradlew :app:assembleDebug` successfully.

## Test status: none run.

## Implemented features
- None (UI, playback, persistence and compat are all unimplemented).
- Skeleton files only: `MainActivity` placeholder with no data, domain identifier value classes (`ProviderId`, `MediaRef`, `EpisodeRef`), empty `:core:data` marker file.

## Documentation (Agent 2)
- `docs/architecture/`: ARCHITECTURE, TECH_STACK, DOMAIN_MODEL, INTEGRATION_CONTRACTS (DRAFT, NOT AGREED), DATA_FLOW, DECISIONS.

## Blockers
2. Network egress to Google Maven, Gradle distribution and Maven Central is blocked for this session, so no build or dependency resolution is possible here.
3. Android SDK not installed and cannot be downloaded in this session.
4. Namespace and brand naming audit pending (D-002).
5. Provider port contracts need Agent 1 agreement before any implementation (INTEGRATION_CONTRACTS.md section 4).
6. CloudStream reuse: attribution and GPL-3 compliance rules now apply (DECISION_LOG D-007). Compat API-name review still needs legal check.

## Next actions
1. Obtain network access (or a machine with it) and an Android SDK; regenerate the Gradle wrapper; run `./gradlew :app:assembleDebug` and record the result here.
2. Agent 1 reviews INTEGRATION_CONTRACTS.md sections 2 and 5 and answers the questions; Agent 3 answers section 6.
3. Claude reviews and approves the architecture contracts (ARCHITECTURE.md, DECISIONS.md).
4. Settle minimum SDK with Agent 1 (provisional 26).

## M1 retry (Agent 2, 2026-11-24, about 02:42 IST)
Result: **M1 BLOCKED (environment).** Build status stays UNVERIFIED. Nothing compiled.

Commands and exact output:
- SDK search: `ls -d /opt /usr/lib/android-sdk ~/Android /opt/android* /usr/local/android*` returned only `/opt`. `ANDROID_HOME` and `ANDROID_SDK_ROOT` unset. `sdkmanager` not on PATH.
- `curl -sI https://dl.google.com/android/repository/repository2-3.xml` gave `HTTP/1.1 403 Forbidden (agent proxy: no rule or network setting allows this host)`.
- `curl -sI https://repo.maven.apache.org/maven2/` gave the same 403 with the same proxy message.
- `curl -sI https://plugins.gradle.org/m2/` gave the same 403 with the same proxy message.
- `curl -sI https://services.gradle.org/distributions/gradle-8.14.3-bin.zip` gave the same 403 with the same proxy message. No Gradle wrapper can be generated.
- `gradle :app:assembleDebug --no-daemon --console=plain` (system Gradle 8.14.3, no wrapper): `BUILD FAILED in 10s`. Failure: `Plugin [id: 'com.android.application', version: '8.7.3', apply: false] was not found in any of the following sources ... could not resolve plugin artifact 'com.android.application:com.android.application.gradle.plugin:8.7.3'. Searched in: Google, MavenRepo, Gradle Central Plugin Repository.`

Cause: the egress proxy refuses every Google, Maven and Gradle host (and services.gradle.org). The build stops at plugin resolution, before any source file is compiled. No SDK exists to install.

Static review (read only, no compiler): no compile-breaking issue found in `settings.gradle.kts`, the root and module build scripts, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `MainActivity.kt`, `DataLayer.kt` or `Identifiers.kt`. This is not evidence that the build passes. Code was not changed.

Unblock options (owner): give this session access to `dl.google.com`, `repo.maven.apache.org`, `plugins.gradle.org` and `services.gradle.org`, or run the build on a machine with them and an Android SDK (platform 35, build-tools).

## M2 contracts (Agent 2, docs only)
- `docs/architecture/INTEGRATION_CONTRACTS.md` v2: 0 of 15 contracts AGREED. Agent 1 and Agent 3 answers were not in the repository when checked.
- `docs/architecture/DECISIONS.md`: ADR-011 to ADR-015 added (minSdk, HTTP stack, DI, ABI module placement, port error types).
