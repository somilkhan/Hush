# QA, security and release audit (Phase 1)

- Date: 2026-11-26, 11:07 IST (+05:30)
- Commit audited: `887cdea` on branch `fix/root-theme-wrap` (working tree clean at audit time)
- Reference: NuvioMobile-Enhanced `7aca5a018` at `/home/claude/upstream-nuvio` (parity context only)
- Scope: read-only. No code edited, no commit, no push, no Gradle run (no Android SDK in this sandbox).
- Verification legend: **UNVERIFIED** = read from code or docs only; nothing executed.
- Skills: `systematic-debugging` and `axiom-testing` were requested but are not available in this session (`Unknown skill`, `ListSkills` returns no match). The defect and test-gap review below was done manually.

## Summary of classifications

| Class | Count |
|---|---|
| TAKE | 7 |
| ADAPT | 4 |
| ALREADY IMPLEMENTED | 20 |
| SKIP | 2 |
| NEEDS INVESTIGATION | 10 |
| **Total** | **43** |

## Top release blockers

1. **Placeholder namespace is in every module** (`app.hushstream`, see row 37). Must be renamed or formally accepted before any public release. Owner decision D-002.
2. **No signed release build.** CI builds only `assembleDebug`. `release` has no `signingConfig`, minify is off (ADR-020 requires a signed APK in CI). Upstream `mobile-release.yml` shows a keystore-from-secret pattern to adapt.
3. **Auth gate defect (new, D-1 below).** An expired access token plus a network failure at launch sends a signed-in user to the login screen. Tokens are kept, so this is a UX and sync-availability bug, not data loss. Runtime behaviour is UNVERIFIED.

Also: CI never runs tests (`SyncMergeTest` and `DemoMediaRepositoryTest` exist but are not executed by any workflow). No test or build result exists in this repo for `887cdea`. The CI green run cited in DECISION_LOG D-009 (run 45480169532) is cited, not re-checked here.

## New defect found

### D-1 (medium): offline launch with expired session shows login
- **Files:** `core/data/.../remote/SupabaseAuthRepository.kt` (`currentSession()`, lines around 34 to 44) and `app/.../ui/HushApp.kt` (gate, lines around 63 to 65).
- **What happens:** `currentSession()` returns `null` when refresh fails for any reason. The code comment says network errors keep tokens (F3 fix), but the return value is still `null`. `HushApp` sets `Gate.MAIN` only when `currentSession() != null || guestStore.isGuest`, otherwise `Gate.LOGIN`.
- **Failure scenario:** user signed in, access token expired more than 60 s ago, device offline at launch → refresh throws a network exception → `currentSession()` returns `null` → user lands on the login screen. Tokens remain on disk, so a later sign-in works.
- **Fix direction (for Agent 2, not done here):** distinguish "no session" from "session exists, refresh deferred" (for example, return the stored session with a stale flag, or return a sealed result). Keep sign-out only for `AuthRejectedException`.
- **Evidence:** code reading only. UNVERIFIED at runtime. Needs a device or instrumented test to confirm.

## Items

| # | Item | Class | Evidence (file or doc) | Notes |
|---|---|---|---|---|
| 1 | `SyncMergeTest` (6 cases: newer local, newer remote, tie to remote, identical, one-sided rows, empty) | ALREADY IMPLEMENTED | `core/data/src/test/.../sync/SyncMergeTest.kt` | Logic matches `SyncMerge.kt`. Not run. UNVERIFIED. |
| 2 | `DemoMediaRepositoryTest` (6 cases) | NEEDS INVESTIGATION | `core/data/src/test/.../DemoMediaRepositoryTest.kt` | Tests `DemoMediaRepository`, which no code in `app/` or `core/` wires. Keep or delete is an owner or Agent 2 call. |
| 3 | CI step to run unit tests (`gradle :core:data:test`) | TAKE | `.github/workflows/android.yml` (build step only) | ADR-022 allows CI build on every push. A test step is cheap. Currently no test runs in CI. |
| 4 | Unit tests for `SupabaseAuthRepository`: 400 on refresh clears tokens, network error keeps them, https guard, `signOut` clears local state | TAKE | `SupabaseAuthRepository.kt` | Needs MockWebServer or an injected client. No test exists. Covers F3 and F4 regressions. |
| 5 | Unit tests for `SyncEngine` with fake stores (signed-out returns `SignedOut`, failure returns class name only, fetch-before-push ordering) | TAKE | `core/data/.../sync/SyncEngine.kt` | No test exists. The engine uses an interface (`RemoteStore`), so fakes are easy. |
| 6 | Unit tests for addon guards: `PrefsAddonRegistry` rejects non-https, `StremioClient` rejects non-https | TAKE | `addon/PrefsAddonRegistry.kt:69`, `addon/StremioClient.kt:37` | No test exists. |
| 7 | Instrumented or Robolectric tests for `SecureTokenStore` round trip and legacy `hush_auth` clear | ADAPT | `SecureTokenStore.kt` | Keystore needs a device or Robolectric support. Upstream uses `androidHostTest` (for example `composeApp/src/androidHostTest/...`). Adapt the layout, not the code. |
| 8 | Compose and player tests (`HushVideoPlayer` lifecycle, release on dispose) | ADAPT | `core/player/.../HushVideoPlayer.kt` | Per ADR-022 the device run is the gate. Add only after device testing is set up. |
| 9 | Lint or static analysis config (Android lint, detekt) | NEEDS INVESTIGATION | none in repo | Not present. Owner to decide whether lint is in CI. |
| 10 | Compose UI test suite | SKIP | ADR-022 | Owner accepted manual device testing. |
| 11 | F1: tokens encrypted with Keystore AES-GCM | ALREADY IMPLEMENTED | `SecureTokenStore.kt` (AES/GCM/NoPadding, key in AndroidKeyStore, `hush_secure` file) | Code verified. Runtime UNVERIFIED. The audit's VERIFIED status refers to the old code; now fixed. |
| 12 | F2: backup disabled | ALREADY IMPLEMENTED | `app/src/main/AndroidManifest.xml` (`android:allowBackup="false"`) | No `dataExtractionRules` file. Acceptable while backup is off. |
| 13 | F3: keep tokens on network error, clear only on 400/401 | ALREADY IMPLEMENTED (code) | `SupabaseAuthRepository.kt` (`AuthRejectedException`) | Gate side effect is defect D-1 (row 17). Runtime NOT TESTED. |
| 14 | F4: https required for auth base URL | ALREADY IMPLEMENTED | `SupabaseAuthRepository.kt` (`configured`) | Guard covers `signIn`, `signUp`, `signOut`, refresh. |
| 15 | F5: player created in `DisposableEffect(url)` and released on dispose | ALREADY IMPLEMENTED (code) | `core/player/.../HushVideoPlayer.kt` | The audit described `remember(url)`. Current code is fixed. `PlayerView` is not detached on dispose; see row 44. |
| 16 | F6: server error text shown to user | SKIP | audit F6 | Info only. No change. |
| 17 | D-1: offline launch with expired session goes to login | TAKE | see the D-1 section above | Fix in `SupabaseAuthRepository` and `HushApp`. |
| 18 | No service-role or secret key in working tree | ALREADY IMPLEMENTED | grep for `service_role`, `sb_secret`, JWT-shaped strings: no hits | Repo search only. History search not repeated here. |
| 19 | Cleartext traffic disabled | ALREADY IMPLEMENTED | manifest has no `usesCleartextTraffic`, no `http://` literals in code | Upstream sets `usesCleartextTraffic="true"` and a network security config. Do not copy. |
| 20 | No logging of tokens or payloads | ALREADY IMPLEMENTED | grep for `Log.`, `println(`, `printStackTrace`: no hits | |
| 21 | Addon URLs https only, responses treated as untrusted | ALREADY IMPLEMENTED | `addon/AddonMediaRepository.kt:66`, `addon/StremioClient.kt:37` | Torrent and infoHash streams are dropped. |
| 22 | Addon response size bound (StremioClient header says "bounded response size") | NEEDS INVESTIGATION | `addon/StremioClient.kt` | Header claim not checked in this audit. Read the body-size logic before relying on it. |
| 23 | RLS on all four public tables, owner-only policies, no `anon` grants | ALREADY IMPLEMENTED | `docs/audit/2026-11-25-security-audit.md` (Supabase MCP evidence) and `supabase/migrations/20261125000000_init_sync_schema.sql` | Read from the audit. Not re-queried here. |
| 24 | FORCE RLS on table owner, and a live spoof test with an authenticated client | NEEDS INVESTIGATION | audit "Not checked" list | Owner-role bypass only. Live test never run. |
| 25 | Sign-out policy: whether device-local data is kept or cleared | NEEDS INVESTIGATION | `SettingsScreen.kt:73` (`auth.signOut()` only), `SupabaseAuthRepository.signOut()` | Owner decision pending (CLAUDE.md, status doc). Current code clears auth only. |
| 26 | CI: JDK 21, platform 35, `:app:assembleDebug` | ALREADY IMPLEMENTED | `.github/workflows/android.yml` | Build UNVERIFIED in this sandbox. |
| 27 | CI secrets: written to `local.properties` from env, not echoed, warning when empty | ALREADY IMPLEMENTED | `android.yml` "Write Supabase config" step | Publishable key ends up in the APK via BuildConfig. This is expected for a publishable key. |
| 28 | CI artifact: debug APK upload with `if-no-files-found: error` | ALREADY IMPLEMENTED | `android.yml` | |
| 29 | Actions pinned by tag, not commit SHA | ADAPT | `android.yml` (`actions/checkout@v4`, `setup-java@v5`, `setup-gradle@v4`, `upload-artifact@v4`) | Pin to SHAs with a version comment. Low effort, supply-chain benefit. |
| 30 | Gradle: no wrapper; CI installs Gradle 8.14.3 through `setup-gradle` | NEEDS INVESTIGATION | `PROJECT_STATUS.md` (wrapper generation was blocked); `android.yml` | Decide whether to commit `gradlew` once network allows. |
| 31 | Workflow token permission `contents: read` | ALREADY IMPLEMENTED | `android.yml` | |
| 32 | Signed release APK job for GitHub Releases (ADR-020) | ADAPT | ADR-020 in `docs/architecture/SOFTWARE_ARCHITECTURE.md`; upstream `mobile-release.yml` (keystore from base64 secret) | Not implemented here. Keystore must come from a GitHub secret, never the repo. Blocked by D-002 for the identity. |
| 33 | LICENSE is GPL-3.0 text | ALREADY IMPLEMENTED | `LICENSE` is byte-identical to `/usr/share/common-licenses/GPL-3` | |
| 34 | NOTICE with third-party attribution to NuvioMobile-Enhanced | ALREADY IMPLEMENTED | `NOTICE` | Matches D-009 "open" item (now present). |
| 35 | Copyright holder text ("HUSH contributors") | NEEDS INVESTIGATION | `NOTICE` line 2; CLAUDE.md "copyright holder" is an owner decision | Owner decision. |
| 36 | No CloudStream code and no `com.lagradost` names in the tree | ALREADY IMPLEMENTED | grep across `*.kt`, `*.kts`, `*.xml`, `*.toml`: no hits | Nothing to attribute yet. Attribution rules apply when code is ported. |
| 37 | Placeholder namespace `app.hushstream` (applicationId, namespace in 5 modules, package in about 100 files) | NEEDS INVESTIGATION | `app/build.gradle.kts`, `settings.gradle.kts`, `core/*/build.gradle.kts`, `design/build.gradle.kts` | Blocker for public release. Owner decision D-002 after trademark and store-name check. |
| 38 | No fake or demo data shown to users (rule 4) | ALREADY IMPLEMENTED | `AppContainer.kt` uses `EmptyMediaRepository` fallback; `DemoMediaRepository` is not referenced | `AppContainer` KDoc says "demo data is shown only while none is installed", which is inaccurate. Fix the comment (TAKE-level doc fix, folded into row 3 work). |
| 39 | ADR-019 offline downloads (legal question) | NEEDS INVESTIGATION | ADR-019 status PROPOSED; `core/data/.../DataLayer.kt` is a placeholder comment; no download code exists | Recorded only. Owner must record the legal answer in DECISION_LOG before any download feature ships. Affected future module: `:core:download` (not created). No advice given. |
| 40 | ADR-020 sideload release | see row 32 | | Merged into row 32 to avoid double counting. |
| 41 | ADR-022 manual device testing as the acceptance gate | ALREADY IMPLEMENTED (as policy) | `SOFTWARE_ARCHITECTURE.md` ADR-022 | No device run is recorded for `887cdea`. Device evidence UNVERIFIED. |
| 42 | GPL headers in source files | NEEDS INVESTIGATION | DECISION_LOG D-007 says headers are added "when real code exists"; no `Copyright` lines in `*.kt` | Owner or Agent 2 to decide the header text and whether own code needs one. |
| 43 | `PROJECT_STATUS.md` is stale (last updated 2026-11-24, says branch `main` at `1253bb7`) | TAKE | `docs/PROJECT_STATUS.md` | Update at next milestone (CLAUDE.md doc rules). |
| 44 | `PlayerView` not detached on dispose (`view.player` set to null is not done after release) | TAKE | `HushVideoPlayer.kt` `AndroidView` | Minor. Set `player = null` on the view in `onRelease` or on dispose. Not a crash path by itself; UNVERIFIED. |

## Not checked (UNVERIFIED)

- Gradle build, unit tests, lint: not run (no SDK, no network to Google Maven or services.gradle.org).
- Device behaviour: auth refresh, Keystore round trip, player lifecycle: not run.
- Supabase live queries (FORCE RLS, spoof test): not run in this audit. Rows 23 and 24 rely on the earlier audit file.
- Git history secret scan: not repeated (working tree only).
- `NOTICE` and `LICENSE` were read or compared; no legal review performed (owner waived M3 review, D-013).
