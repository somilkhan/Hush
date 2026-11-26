# DECISION_LOG

Format per MASTER_HANDOVER §4.

---

## D-001 — Working brand name: HUSH
- **Date:** 2026-11-24
- **Decision:** Use "HUSH" as the working project name across app identity, UI mockups, documentation and branding concepts.
- **Context:** User instruction in session. Project was previously described as a CloudStream-style clone; user wants a new independent identity.
- **Options considered:** Keep an internal codename only; adopt HUSH as the working name.
- **Chosen approach:** HUSH as working name.
- **Reason:** User decision.
- **Trade-offs:** Name is NOT legally cleared. Trademark/app-store availability must be checked before any public release.
- **Affected modules:** all docs, app name strings, icon/branding concepts.
- **Verification:** Trademark and store-name search NOT yet performed (open item).

## D-002 — Package namespace and repo name: PROVISIONAL placeholder `app.hushstream`
- **Date:** 2026-11-24 (provisional placeholder added 2026-11-24)
- **Decision:** Do not rename the repository (`somilkhan/Hush`) or pick the final package namespace until naming implications are audited. Scaffolding uses the clearly temporary namespace and applicationId `app.hushstream` (`core:data` uses `app.hushstream.core.data`). Do not sign, publish or ship under it.
- **Context:** User asked not to rename blindly. Gradle skeleton needs a namespace to exist at all.
- **Options considered:** (a) Use `com.somilkhan.hush`; (b) use a neutral reverse-domain owned by the user; (c) keep a placeholder namespace until legal/store check.
- **Chosen approach:** Option (c) for scaffolding only. Final namespace still deferred to the naming audit.
- **Reason:** Renaming package/applicationId later is expensive; a wrong early choice cannot be cheaply undone once published. Rename cost is itemised in `docs/architecture/DECISIONS.md` ADR-004.
- **Trade-offs:** Every Kotlin file and the manifest carry the placeholder package until renamed. Cost rises with each feature added.
- **Affected modules:** `settings.gradle.kts`, `app/build.gradle.kts`, `core/data/build.gradle.kts`, all Kotlin packages under `app/` and `core/`.
- **Verification:** Naming audit still pending. Gradle configuration not verified (no network; see PROJECT_STATUS).

## D-003 — Three-agent structure preserved
- **Date:** 2026-11-24
- **Decision:** Keep the three specialist agents exactly as defined in MASTER_HANDOVER §2.
- **Reason:** User instruction: continue original structure, do not restart.
- **Affected modules:** all.
- **Verification:** Agents spawned in this session (see PROJECT_STATUS).

## D-004 — Repository attachment blocked
- **Date:** 2026-11-24
- **Decision:** Work proceeds in a local workspace at `/home/claude/hush` (git initialised, no remote) until GitHub is re-authenticated and `somilkhan/Hush` is attached.
- **Context:** `add_repo` returned "link your GitHub account"; GitHub connector later reported sign-in needed.
- **Trade-offs:** Local commits must be pushed to the real repo later; no remote history exists locally.
- **Verification:** Pending user sign-in.

## D-005 — Initial module layout (Agent 2)
- **Date:** 2026-11-24
- **Decision:** Create `:app`, `:core:domain` (pure Kotlin/JVM) and `:core:data` (Android library) now. Reserve `:core:compat` (Agent 1), `:core:player`, `:core:designsystem` and `:feature:*` for their owners to create when they have real code. Manual DI via `AppContainer`, no DI framework.
- **Context:** Handover §5 asks for foundations before parallel build-out.
- **Options considered:** Single module; full multi-module upfront; the chosen split.
- **Chosen approach:** Minimal boundaries that matter now (domain purity, data separation, app as composition root).
- **Reason:** Enforces the rule that UI and domain never import compat internals.
- **Trade-offs:** Later module splits cost refactoring time.
- **Affected modules:** all Gradle modules; see `docs/architecture/ARCHITECTURE.md`.
- **Verification:** Structure written. Build NOT verified (no network, no Android SDK). Wrapper not generated.

## D-006 — Build status recorded as UNVERIFIED
- **Date:** 2026-11-24
- **Decision:** The Gradle skeleton is not claimed to build. Versions in `gradle/libs.versions.toml` are provisional.
- **Context:** Egress proxy blocks Google Maven, Maven Central and Gradle distribution; Android SDK absent.
- **Options considered:** Claim success from file presence (rejected); copy a wrapper jar from another clone (rejected: third-party code not checked for licence).
- **Chosen approach:** Mark UNVERIFIED in PROJECT_STATUS; build when network and SDK exist.
- **Reason:** CLAUDE.md rule 3.
- **Trade-offs:** No compile evidence yet.
- **Affected modules:** all Gradle modules.
- **Verification:** `gradle projects` attempted with system Gradle 8.14.3, failed at plugin resolution (HTTP 403).

## D-007 — Licence: GPL-3.0, open source
- **Date:** 2026-11-24
- **Decision:** HUSH is released under GPL-3.0 and developed as a public open-source repository (`somilkhan/hush`). Full text in `LICENSE` (copied from the system GPL-3 text).
- **Context:** The upstream repo had its `LICENSE` file deleted. Owner chose open source. CloudStream is also GPL-3.0, so reuse is now legally simpler, but still needs proper attribution and notices.
- **Options considered:** GPL-3.0 open source (chosen); closed/proprietary clean-room only.
- **Reason:** Owner's choice (public repo, open source).
- **Trade-offs:** Any copied or derived CloudStream code must keep copyright notices and be attributed. Extension API compatibility (`com.lagradost.cloudstream3.*` names) must still be reviewed for licence and attribution before implementation.
- **Affected modules:** `LICENSE`, all source files (headers to be added when real code exists), docs/reverse-engineering/LICENSING_AUDIT.md.
- **Verification:** `LICENSE` text copied from system GPL-3 file (Debian common-licenses). Attribution list not yet written.

## D-008 — Architecture drivers and forks (software-architecture interview, 2026-11-25)
- **Date:** 2026-11-25
- **Decision:** Sideload APK only; no backend and no accounts; offline downloads in scope now (ADR-019 PROPOSED, WorkManager + Room index); manual device testing is the acceptance gate; provider failure isolation per call (ADR-018).
- **Context:** No SRS exists, so the fallback interview ran. Owner answers recorded in `docs/architecture/SOFTWARE_ARCHITECTURE.md`.
- **Options considered:** Full list of forks and options is in that document (sections 5, ADR-018 to ADR-022).
- **Chosen approach:** As listed above.
- **Reason:** Owner's answers to four questions; privacy and local control are the top drivers.
- **Trade-offs:** Offline downloads widen the first slice and raise a legal question for the owner. Manual-only testing means regressions are caught by the owner, not by CI.
- **Affected modules:** `:core:download` (new, not yet created), `:core:data`, `:app`, `.github/workflows/android.yml` (release build later).
- **Verification:** Document review only. No code verifies these choices yet.

## D-009 — Screen structure modelled on owner's NuvioMobile-Enhanced fork
- **Date:** 2026-11-25
- **Decision:** Home, Search, Details and Library screen structure follows the layout of the owner's public fork `luqmanfadlli/NuvioMobile-Enhanced` (GPL-3.0). HUSH's own palette, tokens and naming are kept.
- **Context:** Owner asked for the same screen structure and confirmed the fork is their own ("mera").
- **Options considered:** Copy code (rejected); reimplement the structure from the public source (chosen); use a generic layout (rejected by owner).
- **Reason:** Owner's direction. No code, artwork or logos were copied. Nuvio logos and wordmark are not used.
- **Trade-offs:** Structure only. Visuals were not matched because the screenshots were not readable. Posters are tonal placeholders labelled DEMO until artwork exists.
- **Affected modules:** `app/.../ui/` (HushApp, Home, Search, Details, Library, PosterCard, HeroBanner).
- **Verification:** CI `:app:assembleDebug` green on run 45480169532. Not yet tested on device.
- **Open:** Whether to add an attribution line in a NOTICE file for the source of the structure. Owner has not decided.

## D-010 — Backend approved: Supabase, email and password, sync-ready design
- **Date:** 2026-11-25
- **Decision:** Owner approved a backend now, not later. Supabase with email and password login. Sync covers library, addons and plugins (the same scope Nuvio uses). Supabase project is owned by the owner; the Nuvio fallback URL is not used. Supersedes ADR-021 ("no backend, no accounts") for the account and sync scope; downloads stay device-local.
- **Context:** Owner said rewriting later would cost more, so the data layer is built sync-ready now (repository interfaces with local and remote adapters).
- **Options considered:** Backend in a separate milestone (recommended by Claude, not chosen); backend now (chosen).
- **Reason:** Owner's decision, with the reason that a later rewrite is more expensive.
- **Trade-offs:** Adds auth, a server schema, Row Level Security, and privacy duties. Client holds only the anon key; RLS must be enabled on every table before any release.
- **Affected modules:** `:core:data` (remote adapter), `:core:domain` (repository interfaces), `:app` (auth gate), new `supabase/` schema folder.
- **Verification:** Not yet. Waiting for owner to provide project URL and anon key (kept in `local.properties`, never committed).

## D-011 — Guest mode on the login screen (like NuvioMobile-Enhanced)
- **Date:** 2026-11-25
- **Decision:** Login screen offers "Continue as guest". Guest uses the app fully on device, with no account and no sync. A guest can sign in later, and local data is then offered for upload.
- **Context:** Owner asked for guest login, following the Nuvio pattern.
- **Options considered:** Login required (rejected); guest only, no sign-in (rejected); guest with later sign-in (chosen).
- **Reason:** Owner's direction. Keeps the app usable without an account.
- **Trade-offs:** Merging guest data into an account needs a clear rule (ask the user before upload; never overwrite server data silently).
- **Affected modules:** `:app` login screen, `:core:data` local store, `SupabaseAuthRepository`.
- **Verification:** Not yet. Build pending the login screen.

## D-012 — Upstream permission from CloudStream (owner-reported)
- **Date:** 2026-11-25
- **Decision:** Owner reports that permission was granted by CloudStream for reuse of its code and API names. Recorded as owner-reported only.
- **Context:** Owner says the permission is personal and cannot be shared. No written evidence is in the repo.
- **Trade-offs:** Not verifiable from the repo. Extension code belongs to its own authors and needs separate permission and a security analysis. M3 stays OPEN until a written record exists.
- **Affected modules:** `docs/compatibility/`, any future ported CloudStream code.
- **Verification:** None on file. Owner statement only.

## D-013 — M3 compat legal review waived by owner
- **Date:** 2026-11-25
- **Decision:** M3 (external legal review of CloudStream extension API names) is waived by the owner. The compat layer may be implemented.
- **Context:** Owner states CloudStream gave permission (D-012, owner-reported, no written record on file). Owner wrote the M3 gate in the execution plan and chose to waive it.
- **Options considered:** Keep M3 blocking until a written lawyer opinion (previous plan); owner waiver with recorded risk (chosen).
- **Chosen approach:** Waiver recorded here. `docs/compatibility/LEGAL_REVIEW.md` stays a placeholder and must not be claimed as a lawyer's opinion.
- **Reason:** Owner decision. Licences are GPL-3.0 on both sides.
- **Trade-offs:** No independent legal opinion on file. Owner carries the legal risk. Extension code from third parties still needs its own permission check and a security analysis (ADR and execution-plan rule 5).
- **Affected modules:** `docs/compatibility/`, `core/data` (future compat layer), `:app`.
- **Verification:** Owner statement only. No written evidence in the repo.

## D-014 — Public app name: HushStream
- **Date:** 2026-11-26
- **Decision:** The owner's final app name is **HushStream** (display name). The working name HUSH is retired for user-facing text.
- **Context:** Owner chose the name in chat. Repo, package and namespace (`app.hushstream`) are not yet changed.
- **Options considered:** Keep HUSH (working name); HushStream (chosen).
- **Chosen approach:** Use HushStream for app label, About screen and store-facing text once implemented. Package, applicationId and repo name stay open under D-002 until the owner decides.
- **Reason:** Owner decision.
- **Trade-offs:** The name is not trademark-cleared (CLAUDE.md says the same for HUSH). Owner should run a trademark check before any store or public release. No legal advice given here.
- **Affected modules:** `app` (string resources, label), `docs/` user-facing copy. No code changed in this entry.
- **Verification:** Owner statement only. Name not yet applied to code; no build run.

## D-015 — Owner decisions batch 1 (2026-11-26)
- **Date:** 2026-11-26
- **Decision:**
  1. Push the Phase 1 audit branch and the theme fix branch to origin.
  2. App name only: HushStream. Owner's personal name is not used in code identifiers.
  3. minSdk follows Nuvio: API 24 (replaces the HUSH 26 default). Other SDK levels follow Nuvio.
  4. Persistence: move to Room (best practice; supersedes SharedPreferences for library, history and downloads). Previously open under ARCHITECTURE §7 and ADR-010.
  5. Sign-out keeps device-local guest data, watch history and downloads; only account credentials are cleared.
  6. Downloads: follow what NuvioMobile-Enhanced does. Owner declined a separate lawyer review. ADR-019 is closed on owner instruction, with the legal risk carried by the owner.
  7. Name check: owner states HushStream has no conflict. Owner has checked.
- **Context:** Owner answers to the seven questions raised in chat on 2026-11-26.
- **Options considered:** Per item, as raised in chat.
- **Chosen approach:** As listed above.
- **Reason:** Owner decisions.
- **Trade-offs:** Item 6 means downloads go ahead without a written legal opinion. Owner carries that risk. Item 3 (API 24) raises Android 7 support cost. Item 4 needs a migration from existing SharedPreferences data.
- **Affected modules:** `app`, `core/data` (Room migration, sign-out policy), `docs/architecture/`, `docs/DECISION_LOG.md`.
- **Verification:** Owner statement only. No code change in this entry. Package rename (`app.hushstream`) is still to do in the next batch.

## D-016 — Sign-out data policy verified in code (implements D-015 item 5)
- **Date:** 2026-11-26
- **Decision:** Sign-out clears only the login tokens. Library, watch history, guest state and downloads stay on the device.
- **Context:** Code read of `SupabaseAuthRepository.signOut` (clears `SecureTokenStore` only) and `HushApp` `onSignedOut` (resets guest flag and gate only). No code path deletes library or history on sign-out.
- **Options considered:** Clear all local data on sign-out (rejected by D-015); tokens only (chosen).
- **Chosen approach:** No code change needed. Policy is already met.
- **Reason:** Owner decision D-015 item 5.
- **Trade-offs:** A later Room migration must keep this behaviour, and a test should guard it.
- **Affected modules:** `core/data` (remote/SupabaseAuthRepository, SecureTokenStore), `app` (HushApp).
- **Verification:** Static code reading on branch `feature/batch2-identity-minsdk`. No device run. A unit test is still missing (UNVERIFIED).

## D-017 — Room persistence for library and watch history (implements D-015 item 4)
- **Date:** 2026-11-26
- **Decision:** Library and watch history move from SharedPreferences JSON to Room (`hush.db`, tables `library` and `watch_history`). Repository interfaces are unchanged.
- **Context:** Architecture §7 and ADR-010 call for Room. Downloads need a real database.
- **Options considered:** Keep SharedPreferences; move to Room now (chosen).
- **Chosen approach:** Room 2.6.1 with KSP 2.0.21-1.0.28. One-time import from the old JSON keys, removed only after the import transaction succeeds. History keeps the newest 200 rows.
- **Reason:** Owner chose Room (D-015 item 4).
- **Trade-offs:** Adds two build plugins and dependencies. Migration is not yet covered by a test. Downloads still wait on the owner's ADR-019 decision (now D-015 item 6).
- **Affected modules:** `core/data` (`db/`, both local repositories), `gradle/libs.versions.toml`, root and `core/data` build files.
- **Verification:** CI on `94fe317` (`feature/batch2-identity-minsdk`): Android build success, run 45765818890. Runtime import UNVERIFIED (no device, no unit test).

## D-018 — Downloads: slice plan from NuvioMobile-Enhanced (plan only)
- **Date:** 2026-11-26
- **Decision:** Port Nuvio's downloads in ten slices (S1–S10), in the order in `docs/architecture/DOWNLOADS_PLAN.md`. No downloads code is built yet.
- **Context:** Owner decision D-015 item 6 (do what Nuvio does). Nuvio's downloads are about 8,200 lines across 71 files, too large for one batch.
- **Options considered:** One large port; sliced port in dependency order (chosen).
- **Chosen approach:** S1 (model and queue policy) first. Each ported file gets a GPL-3.0 origin header. Package renamed to `app.hushstream`.
- **Reason:** Keeps each batch reviewable and CI-verifiable.
- **Trade-offs:** Downloads will not work end to end until S3 and S5 are done. WorkManager vs JobService is open for S3. Owner carries the legal risk of downloads (D-015 item 6).
- **Affected modules:** `core/domain`, `core/data`, `app`, `design`, `docs/architecture`.
- **Verification:** Plan only. No code change in this entry. CI and device checks come with each slice.

## D-019 — Downloads S1 done: domain model and queue policy
- **Date:** 2026-11-26
- **Decision:** S1 of D-018 is implemented: `DownloadStatus`, `DownloadItem` (subset), `DownloadConcurrencyLimit`, `DownloadStatusCounts`, `DownloadQueuePolicy`, in `core/domain`.
- **Context:** Ported from NuvioMobile-Enhanced 7aca5a0 with origin headers. Pure JVM, no Compose or player types.
- **Options considered:** Port the full Nuvio model now (rejected: pulls in player, subtitle and Compose resources); port the pure subset (chosen).
- **Chosen approach:** Subset in `core/domain/download`. Network state is passed in rather than read from a singleton.
- **Reason:** Keeps S1 testable on CI with no Android dependencies.
- **Trade-offs:** `DownloadItem` lacks player, subtitle and artwork fields until S5. Settings-backed `DownloadQueuePolicy.current()` waits for S2.
- **Affected modules:** `core/domain` (download package, test), `.github/workflows/android.yml` (runs `:core:domain:test`).
- **Verification:** CI success on `c4b5ae5`, run 45767517749, including `:core:domain:test`. Runtime not applicable yet (no UI or transfer).

## D-020 — Product scope: HushStream = Nuvio features + CloudStream extension compatibility
- **Date:** 2026-11-26
- **Decision:** HushStream combines NuvioMobile-Enhanced's features (player, library, downloads, addons, sync, settings) with CloudStream extension compatibility, so existing CloudStream repositories and extensions work in the app. Android-only, Kotlin and Jetpack Compose.
- **Context:** Owner statement in chat, 2026-11-26. Supersedes the earlier "Nuvio features only" reading. Matches the handover rule that existing CloudStream repositories must keep working (§0/§6).
- **Options considered:** Nuvio features only; CloudStream only; combined (chosen).
- **Chosen approach:** Build in batches. Nuvio parity first (downloads S2–S10, playback gaps, UI gaps from the parity audit). Then the CloudStream compatibility layer, which needs its own isolation design and security analysis before any third-party code runs (ADR-008, plugin runtime).
- **Reason:** Owner decision.
- **Trade-offs:** The CloudStream layer is the largest and riskiest part. It runs third-party extension code, so it needs a security review before it ships. M3 legal review is waived (D-013), so the owner carries that risk. Downloads from torrent or HLS-only streams stay unsupported, as in Nuvio.
- **Affected modules:** all; first `core/data` (addon and extension loading), `core/player`, `app`.
- **Verification:** Owner statement only. No code change in this entry.
