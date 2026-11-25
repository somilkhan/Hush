# HUSH Context Snapshot

Status: Phase 0 (repository discovery) and part of Phase 1 (upstream audit). Updated 2026-11-25.

## Verified state
- Repo: `somilkhan/Hush`, branch `feature/app-vertical-slice`, HEAD `02418c1`. `main` is the older bootstrap.
- Modules: `:app`, `:core:domain`, `:core:data`, `:core:player`, `:design` (Android, Kotlin, Compose).
- Kotlin source files: 54. Unit tests: 2 (`DemoMediaRepositoryTest`, `SyncMergeTest`). Neither runs in CI.
- Build: CI `:app:assembleDebug` green on the HEAD commit. Device run: none. Unit tests: not run.

## Current HUSH architecture
- Composition root: `MainActivity` → `AppContainer` (manual DI).
- Navigation: state-based in `ui/HushApp.kt`. Tabs (Home, Search, Library, Settings), with details, sources and player as overlays on that state.
- Catalogue: Stremio-protocol addons (`core:data/addon`): manifest, catalog, meta, stream. https only, bounded reads. With no addon installed, the app shows an empty state (demo data removed).
- Auth: Supabase email/password through `SupabaseAuthRepository`. Tokens encrypted with Android Keystore (`SecureTokenStore`). Guest mode is a local flag.
- Local data: library, watch history and addons in SharedPreferences JSON (mutex-guarded). Room is not used yet.
- Sync: `core:data/sync` (engine, merge rule, remote adapters). Not wired into app start or sign-in. Migration `20261125120000` is not applied to the remote project.
- Player: Media3 ExoPlayer only. Custom overlay (tap to show, 3 s auto-hide, labelled buttons). No subtitle, audio-track, PiP or speed controls yet.

## Screen inventory (HUSH: 7 screens)
| HUSH screen | Nuvio counterpart (file) | Status |
|---|---|---|
| Login (`ui/login`) | AuthScreen | Email/password + guest. Visual check pending. |
| Home (`ui/home`) | HomeScreen | Hero, Continue watching, poster rail. No real artwork. |
| Search (`ui/search`) | SearchScreen | Addon search, grid, states. |
| Details (`ui/details`) | MetaDetailsScreen | Save, season picker, episode cards, Play. |
| Library (`ui/library`) | LibraryScreen | Saved titles grid only. |
| Settings (`ui/settings`) | SettingsScreen (+ 4 sub-pages) | Account, sources, about. Very small. |
| Sources (`ui/sources`) | AddonsScreen | Add and remove manifest links. |
| Player (`core:player`) | PlayerScreen (+ ~30 runtime files) | Basic overlay only. |

Nuvio has 62 screen-related files, including Downloads, Profiles, Collections, Watch history, Party, Live TV, Stream selection, Custom posters and What's new. HUSH has none of these yet.

## Upstream (Nuvio Enhanced)
- Clone: `/home/claude/luqmanfadlli/nuviomobile-enhanced`, commit `7aca5a0`, shallow.
- Licence: GPL-3.0 (LICENSE file verified, full text).
- Architecture: Kotlin Multiplatform, code under `composeApp/src/commonMain` and platform source sets. Playback includes mpv (libmpv) and ExoPlayer.
- Consequence: Nuvio code is not a drop-in for HUSH's Android-only modules. Reuse means either porting to commonMain structure (a large change) or copying Android-specific files with adaptation.

## Decisions needed from owner (not made yet)
1. **Architecture:** keep HUSH Android-only and port selected Nuvio logic (faster, smaller), or restructure HUSH to Kotlin Multiplatform like Nuvio (large, slower, closer to a full reuse). Recommendation: Android-only with ported logic, because HUSH targets Android only.
2. **Features out of scope:** Party, Discord, Trakt-style integrations, Live TV. Confirm.
3. **Screenshots:** the directive mentions supplied screenshots. They are not in this session's context. Please re-send them if the visual audit should match them.
4. **Sign-out data:** keep or clear local library and history on sign-out.
5. **Downloads:** ADR-019 is still a legal question for the owner.

## Duplicate audit findings
- Two sync-facing stores (local JSON and sync `RemoteStore`) for library and history: intentional, the local store stays the UI source.
- `DemoMediaRepository` kept for tests only; not wired into the app.
- No duplicate navigation, playback or extension manager found in HUSH.

## Known issues
- Deletions do not sync (local remove is re-pulled). Tombstone fix needed before wiring sync.
- Refresh token can race (auth refresh has no mutex). Redirect policy not restricted on auth and addon clients.
- Details Play does nothing when no stream resolves (no error message).
- DemoBadge still shows on items flagged `isDemo` (only demo data sets it now).
- Focus ring fixed at 2 dp, design doc asks 3 dp at 100% font scale.

## Test and build results
- CI assembleDebug: green (HEAD). Last checks on feature branch: runs 45529961538 and the docs-only run after it.
- Unit tests: not run anywhere (CI does not run `:core:data:test`).
- Device/emulator: none. Everything visual and playback is UNVERIFIED.

## Next actions
1. Owner answers decisions 1–5 above.
2. Phase 1 completion: map Nuvio features to HUSH (TAKE, ADAPT, ALREADY, SKIP, INVESTIGATE) in `docs/architecture/UPSTREAM_INTEGRATION_MATRIX.md`, with licence notes per file.
3. Fix the known issues list above in small verified batches.
