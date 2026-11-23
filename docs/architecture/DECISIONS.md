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

## Open decisions (not yet recorded in DECISION_LOG)
- Minimum SDK (26 provisional). Waiting on Agent 1.
- Whether extensions need a specific HTTP client or cookie store.
- Test runner for Android unit tests (Robolectric or device-only).
