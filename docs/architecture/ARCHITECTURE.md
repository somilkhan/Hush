# HUSH — Architecture

Status: PROPOSED (Agent 2). Build status: UNVERIFIED (see docs/PROJECT_STATUS.md).
Package namespace: PROVISIONAL `app.hushstream` (DECISION_LOG D-002).

## 1. Principles
- A new app. CloudStream is a reference for behaviour and reusable, licence-checked code, not a base.
- Extension compatibility is a hard boundary: the UI and domain never import CloudStream types. All legacy contact goes through an adapter owned by Agent 1.
- Keep it small. Modules are added when a boundary is real, not in anticipation.
- Pure Kotlin where possible. Android and Compose appear only in the modules that need them.

## 2. Modules

| Module | Kind | Depends on | Owner | Status |
|---|---|---|---|---|
| `:app` | Android application | all below | Agent 2 (host), Agent 3 (screens later) | skeleton |
| `:core:domain` | Kotlin/JVM | kotlinx-coroutines only | Agent 2 | skeleton (value types) |
| `:core:data` | Android library | `:core:domain` | Agent 2 | skeleton (empty) |
| `:core:compat` | Android library | `:core:domain` | Agent 1 | RESERVED, not created |
| `:core:player` | Android library | `:core:domain` (Media3) | Agent 2 | RESERVED, not created |
| `:core:designsystem` | Android library | none | Agent 3 | RESERVED, not created |
| `:feature:*` | Android library | `:core:domain`, `:core:designsystem` | Agent 3 (UI), Agent 2 (ViewModel contracts) | RESERVED |

Dependency rules (enforced by review until a lint or Gradle check exists):
1. `:core:domain` depends on nothing Android. No Compose, Room, Media3, OkHttp or CloudStream types.
2. `:feature:*` depends on `:core:domain` and `:core:designsystem`, never on `:core:compat` or `:core:data` directly. Dependencies arrive through interfaces wired in `:app`.
3. `:core:compat` implements the provider ports from `:core:domain`. It is the only module allowed to reference CloudStream code or its APIs.
4. `:app` is the composition root. It is the only module that sees every other module.

## 3. Layers and unidirectional data flow
- **Domain**: immutable models and ports (interfaces). Use cases are small classes that call ports and return domain results.
- **Data**: repositories implementing domain persistence ports (Room for history, favourites, downloads index; DataStore for preferences).
- **Compat**: adapters implementing provider ports on top of the extension runtime.
- **Presentation** (feature modules): one ViewModel per screen exposes `StateFlow<UiState>` (immutable). The UI sends events to the ViewModel. One-shot effects (navigation, snackbars) go through a channel. State flows one way: events in, state out.
- **Concurrency**: `suspend` and `Flow` throughout. ViewModels use `viewModelScope`. Compose collects with `collectAsStateWithLifecycle`. Long work runs on `Dispatchers.IO` inside the adapter that owns it, so every port is main-safe.

## 4. Composition and DI
- Manual dependency injection. A single `AppContainer` in `:app`, created in `Application`, holds the repositories, the provider registry and the player factory. No DI framework for now. Revisit only if the graph becomes hard to follow (ADR-003).
- Startup does no network or extension work synchronously. Extension loading is started lazily or on a background dispatcher after first frame.

## 5. Navigation
- Navigation Compose with type-safe routes, owned by `:app`. Feature modules expose their destinations as composable functions and do not own the NavHost.

## 6. Playback
- Media3 ExoPlayer, in `:core:player`, hosted by a `MediaSessionService` so playback survives backgrounding and exposes system controls.
- The player consumes `StreamSource` and `SubtitleTrack` domain values. It does not know which provider produced them.
- Progress is written to history by a throttled writer (about every 5 seconds and on pause or stop), not on every position tick.

## 7. Persistence and state
- Room: watch history, favourites, bookmarks, downloads index.
- DataStore (Preferences): user settings, including the "reduce transparency" fallback that Agent 3 needs.
- Provider and repository state (installed repositories, extension versions) belongs to the compat layer. Its storage format is decided by Agent 1, and the app sees it only through ports.
- In-memory caching of search and detail results is allowed. Nothing that must persist is held only in memory.

## 8. Errors
- Domain failures are a sealed `AppError` hierarchy (see DOMAIN_MODEL.md). Compat maps extension exceptions into these. No raw exception reaches a ViewModel.
- Provider calls are cancellable. Cancelling a screen cancels its in-flight provider work.
- Extension failures are isolated per provider. One broken extension must not fail a multi-provider search.

## 9. Security boundary
- Extension code is third-party code. Agent 1 must provide a security analysis before any loading strategy is accepted (CLAUDE.md rule 5). Until then, nothing executes extension code.

## 10. Testing strategy
- Domain: plain JUnit tests with `kotlinx-coroutines-test`.
- Data: Room tests on device or Robolectric (decision pending).
- Compat: contract tests against recorded fixtures, plus manual real-extension runs logged by Agent 1.
- UI: Compose UI tests owned by Agent 3.
- No test is counted as passing until it has actually run.

## 11. Open items
- Compat boundary and port shapes: INTEGRATION_CONTRACTS.md (pending Agent 1 agreement).
- Minimum SDK: 26 is provisional. Agent 1 must confirm the minimum that extensions and the CloudStream runtime need.
- Liquid Glass rendering approach and fallbacks: Agent 3 (see TECH_STACK.md section on rendering constraints).
