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

## D-002 — Package namespace and repo name: PROVISIONAL placeholder `app.hush.placeholder`
- **Date:** 2026-11-24 (provisional placeholder added 2026-11-24)
- **Decision:** Do not rename the repository (`somilkhan/Hush`) or pick the final package namespace until naming implications are audited. Scaffolding uses the clearly temporary namespace and applicationId `app.hush.placeholder` (`core:data` uses `app.hush.placeholder.core.data`). Do not sign, publish or ship under it.
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
