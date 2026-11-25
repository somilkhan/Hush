# HUSH — Architecture decisions (ADR summary)

Authoritative project log: `docs/DECISION_LOG.md`. This file records architecture-level decisions in ADR form. Where the two overlap, DECISION_LOG is the index and this file holds the reasoning.

## ADR-001 — New app, not a CloudStream fork
- Status: ACCEPTED (from the project handover, §0 and §9).
- Context: The product goal is a new identity and UI with compatibility for CloudStream extensions.
- Decision: Build a new Kotlin and Compose app. Reuse CloudStream code only after licence review, and only through `:core:compat`.
- Consequence: Every reused component needs an entry in Agent 1's licensing audit.

## ADR-002 — Module layout
- Status: PROPOSED.
- Decision: `:app`, `:core:domain`, `:core:data` created now. `:core:compat`, `:core:player`, `:core:designsystem`, `:feature:*` reserved, created by their owners when needed.
- Reason: Boundaries that are needed now (domain vs data vs app) are cheap to enforce. Others are created when there is real code to put in them.
- Trade-off: Gradle module count grows later, and the first module-level refactor will cost time.

## ADR-003 — Manual dependency injection
- Status: PROPOSED.
- Decision: A single `AppContainer` in `:app`. No Hilt or Koin.
- Reason: The graph is small. Manual wiring is explicit and easy to test.
- Revisit when: more than roughly 20 injected types or several feature modules need scoped graphs.

## ADR-004 — Placeholder namespace and applicationId
- Status: ACCEPTED as provisional (DECISION_LOG D-002).
- Decision: Use `app.hush.placeholder` for `namespace` and `applicationId`. Final value is deferred until the naming audit (trademark, store availability, domain ownership) is done.
- Rename cost, to be paid later:
  1. `namespace` and `applicationId` in `app/build.gradle.kts`, and `namespace` in each library module.
  2. Package directory and `package` line in every Kotlin file (`app/hush/placeholder` to the final path). Mechanical but touches every file.
  3. `AndroidManifest.xml` class references are relative, so they change only with the package.
  4. Any stored data keyed by package name (DataStore file names are not, Room database name is set in code, but shared preferences, file paths under app data, and any deep-link or intent actions would be affected). Currently none exist, so the cost is low now and rises with every feature.
  5. Signing keys and store listing: if a release is ever signed under the placeholder ID, the applicationId cannot be changed for the same store listing. The rule is: do not sign or publish under the placeholder.
- Verification: Not done. Naming audit is open.

## ADR-005 — Pure Kotlin domain module
- Status: PROPOSED.
- Decision: `:core:domain` has no Android, Compose or networking dependency.
- Reason: Keeps models and ports testable on the JVM and stops framework types leaking into business logic.

## ADR-006 — Ports, not CloudStream types, cross the UI boundary
- Status: PROPOSED, pending Agent 1 agreement.
- Decision: The app defines its own ports and domain models. Compat adapts CloudStream contracts to them.
- Reason: The handover forbids the UI depending on legacy internals, and an independent contract keeps the UI stable if compat internals change.
- Risk: The adapter may lose information the real contract provides. Mitigation: Agent 1 must document each lossy mapping.

## ADR-007 — Unidirectional data flow with immutable UI state
- Status: PROPOSED.
- Decision: One ViewModel per screen, `StateFlow<UiState>` out, events in, one-shot effects through a channel.
- Reason: Predictable state, easy to test, and it matches Compose's model.

## ADR-008 — Extension code is not executed until security is analysed
- Status: ACCEPTED as a rule (CLAUDE.md rule 5, handover §6).
- Decision: Adding a repository and installing an extension package never execute extension code. Execution strategy waits for Agent 1's security analysis.
- Reason: Extension code is third-party code with unrestricted reach unless isolated.

## ADR-009 — Media3 for playback, in its own module
- Status: PROPOSED.
- Decision: Playback is in `:core:player`, based on Media3 ExoPlayer and a `MediaSessionService`.
- Reason: Background playback and system media controls need a service, and the player must not know provider details.
- Alternatives considered: Raw `MediaPlayer` (rejected: weaker HLS, DASH and subtitle support).

## ADR-010 — Persistence split: Room for records, DataStore for settings
- Status: PROPOSED.
- Decision: Structured, queryable data in Room. Simple key-value preferences in DataStore.

## ADR-011 — Minimum SDK stays 26 (provisional)
- **Date:** 2026-11-24
- **Status:** PROPOSED. Owner decision (EXECUTION_PLAN §8 item 5).
- **Context:** `minSdk = 26` is set in `app/` and `core/data/`. No reason for 26 was recorded. `docs/reverse-engineering/DEPENDENCY_MAP.md` §2 pins CloudStream's Jackson at 2.13.1 with the comment "later versions don't support minSdk < 26", and Rhino 1.8.1 for minSdk reasons. So 26 is compatible with the extension pins, and a lower floor would be constrained by them. Real backdrop blur needs API 31 (TECH_STACK §3).
- **Options considered:** 26 (keep); 24 (wider reach, more code paths); 31 (drop blur fallback code, smaller audience).
- **Chosen:** Keep 26. No build file change.
- **Reason:** Changing it now costs nothing, but no evidence yet favours another value. Liquid Glass fallbacks cover API 26 to 30.
- **Trade-offs:** Blur fallback code is needed on API 26 to 30. Agent 1 may raise the floor if the extension runtime needs it.
- **Affected modules:** `app`, `core:data`.
- **Verification:** Not built (M1 BLOCKED). The value is only a declaration.

## ADR-012 — HTTP stack: OkHttp as the single app HTTP client
- **Date:** 2026-11-24
- **Status:** PROPOSED. The OkHttp choice is provisional. The `nicehttp` part is NEEDS-LEGAL.
- **Context:** Extensions link against OkHttp (`okhttp3/Interceptor`) and nicehttp (`com.lagradost.nicehttp`), per EXTENSION_ARCHITECTURE §3.1. TECH_STACK §7 planned OkHttp for the app.
- **Options considered:** (a) OkHttp everywhere, shared through `AppContainer`; (b) Retrofit or Ktor for the app, OkHttp only for extensions; (c) a second client per extension.
- **Chosen:** (a). One OkHttp client, created in `AppContainer`, is the only network path for app code. Compat wraps it for extension calls (C5 in EXTENSION_ARCHITECTURE §7.1).
- **Reason:** The ABI pins OkHttp anyway, so a second client adds a second set of timeouts, proxies and cookie stores with no benefit. Retrofit and Ktor add nothing the provider calls need.
- **Trade-offs:** The OkHttp version must match the ABI pin. `DEPENDENCY_MAP §3` names the OkHttp `Interceptor` but gives no version, so the pin is unknown (asked in INTEGRATION_CONTRACTS §6 Q7). Nothing is added to `libs.versions.toml` in this milestone.
- **Affected modules:** `app` (wiring), `core:compat` and `:compat:cloudstream-abi` (later).
- **Verification:** None. No dependency resolved (M1 BLOCKED).

## ADR-013 — Dependency injection: manual `AppContainer`, reaffirmed
- **Date:** 2026-11-24
- **Status:** PROPOSED (follows ADR-003).
- **Context:** The compat layer adds a provider registry, a loader and an HTTP client. The graph grows.
- **Options considered:** Hilt; Koin; manual `AppContainer` in `:app` (ADR-003).
- **Chosen:** Manual `AppContainer`. It is the only place that constructs `:core:compat` and `:compat:cloudstream-abi`. Feature modules get interfaces only.
- **Reason:** Graph is still small. Keeps compat types out of feature modules, which the boundary rules need.
- **Trade-offs:** Manual wiring is verbose. Revisit at roughly 20 injected types (ADR-003).
- **Affected modules:** `app`.
- **Verification:** None (no code yet).

## ADR-014 — Module boundaries: ABI shim in its own module
- **Date:** 2026-11-24
- **Status:** NEEDS-LEGAL. Placement depends on the M3 review (EXTENSION_ARCHITECTURE §8 Q1, Q2).
- **Context:** ARCHITECTURE §2 reserves `:core:compat` for all compat code. EXTENSION_ARCHITECTURE §6.2 asks for the ABI shim in its own module, so its licence status can be isolated and reviewed. The two documents conflict.
- **Options considered:** (a) everything in `:core:compat`; (b) ABI shim as `:compat:cloudstream-abi`, HUSH-side adapters in `:core:compat`; (c) a separate companion app.
- **Chosen (proposed):** (b). `:core:compat` depends on `:core:domain` only. `:compat:cloudstream-abi` depends on `:core:compat`. Only `:app` depends on the shim.
- **Reason:** Gradle visibility then enforces that the UI never sees ABI classes. The licence-sensitive code sits in one module that can be removed or replaced.
- **Trade-offs:** One more module. Gradle `settings.gradle.kts` needs an `include` only once a real module exists.
- **Affected modules:** `settings.gradle.kts` (later), `:core:compat`, `:compat:cloudstream-abi`, `:app`.
- **Verification:** None. Nothing created in M2 (docs only).

## ADR-015 — Port error and result types
- **Date:** 2026-11-24
- **Status:** PROPOSED. Needs Claude sign-off and Agent 1 agreement.
- **Context:** v1 used `AppError`. The compat spec used a separate `ProviderError`. Two error types would leak through ports.
- **Options considered:** (a) keep `AppError` and add cases; (b) a separate `ProviderError` crossing ports; (c) `kotlin.Result`.
- **Chosen:** (a). Additive `AppError` cases: `Timeout`, `NoData`, `Unavailable`, `Untrusted`. Streams are `Flow`s. Search fan-out is a domain use case. `Outcome.Success` / `Outcome.Failure`.
- **Reason:** One error type for the UI. Typed errors keep `Cancelled` separate from failures.
- **Trade-offs:** Adding sealed cases breaks exhaustive `when` in UI code. Agent 3 must handle the new branches.
- **Affected modules:** `:core:domain`, `:feature:*`, `:core:compat`.
- **Verification:** None (no code yet).

## ADR-016 — Section kind and layout hint; null cursor means the last page
- **Date:** 2026-11-24
- **Status:** PROPOSED. Needs Agent 1 confirmation (whether CloudStream declares any section kind) and Claude sign-off.
- **Context:** Agent 3 (SCREEN_INVENTORY M2 Q5) cannot label Trending or Categories rows without a kind. Agent 3 also asked whether the end of a list is an empty or a null cursor.
- **Options considered:** (a) infer kind from section titles in compat; (b) a new `DiscoveryPort` method for categories and trending; (c) `Section.kind` and `Section.layout` set by compat only when declared.
- **Chosen:** (c). `null` `nextCursor` means the last page. Empty strings are never sent.
- **Reason:** Title inference is guessing, which the no-fake-data rule forbids. A new port method is a breaking change for compat. A field on `Section` is additive.
- **Trade-offs:** Providers that declare no kind show as `GENERIC` rows. Trending and Categories stay hidden until a provider declares them.
- **Affected modules:** `:core:domain` (Section), `:core:compat`, `:feature:*` (Agent 3).
- **Verification:** None (no code yet). DOMAIN_MODEL §2 still needs this change.

## ADR-017 — Vertical-slice contract (demo catalogue, Media3 player)
- **Date:** 2026-11-24
- **Status:** PROPOSED. Scope is the playable vertical slice only. Not the compat contract.
- **Context:** The owner needs a playable build to try on a phone. The compat contracts (INTEGRATION_CONTRACTS) are not agreed, so no CloudStream data can be shown yet.
- **Decision:** A small slice contract in `:core:domain` (`MediaKind {MOVIE, SERIES}`, `StreamSource`, `Episode`, `MediaItem`, `MediaRepository`). `:core:data` provides `DemoMediaRepository`, a static catalogue with `isDemo = true` and a public Google test stream. `:core:player` provides `HushVideoPlayer` on Media3.
- **Deviation from DOMAIN_MODEL:** `MediaKind` has only MOVIE and SERIES (no ANIME or OTHER), and `MediaItem`/`Episode`/`StreamSource` use the flat shape of the brief, not `MediaRef`/`MediaSummary`/`MediaDetails`. The slice shape must be mapped to the DOMAIN_MODEL shape before compat lands. Recorded here so it is not mistaken for the agreed model.
- **Versions:** Media3 1.5.1 and Lifecycle 2.8.7 are UNVERIFIED (no Google Maven access in this environment).
- **Verification:** Read-through only. Unit tests in `:core:data` are NOT RUN. The build is NOT VERIFIED.

## Open decisions (not yet recorded in DECISION_LOG)
- Minimum SDK (26 provisional). Waiting on Agent 1.
- Whether extensions need a specific HTTP client or cookie store.
- Test runner for Android unit tests (Robolectric or device-only).
