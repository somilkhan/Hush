# HUSH — Integration contracts (application ports)

Status: M2 DRAFT v2 (2026-11-24, Agent 2). **Nothing is AGREED.**
Agent 1's M2 answers (`docs/compatibility/EXTENSION_ARCHITECTURE.md`, "M2 answers") were NOT present when this was written. Agent 3's answers are also missing. Every contract that depends on them stays PROPOSED. Re-check both before M2 sign-off.

Principle: these ports describe what the app needs, not how CloudStream works. Where an operation has no CloudStream equivalent, compat returns `AppError.Unsupported` and does not fake a result.

Location once agreed: `core/domain/.../port/` (interfaces only). Implementations live in `:core:compat` (HUSH-side adapters) and `:compat:cloudstream-abi` (ABI shim, see DECISIONS ADR-014).

Status legend:
- **AGREED**: both the owner and the consuming agent have signed it off in this file.
- **PROPOSED**: shape written; waiting on a named answer or review.
- **NEEDS-LEGAL**: blocked on the M3 legal review or an owner decision on licence or naming.

## 1. Conventions
- All operations are `suspend`, or return `Flow` where noted. They are main-safe.
- Expected failures are returned as `Outcome.Failure(AppError)`. Ports do not throw for them.
- Cancellation propagates. Compat must never swallow `CancellationException`. It maps to `AppError.Cancelled` only at the boundary.
- Every operation names its provider, or the caller fans out. Ports never pick a provider silently.
- Pagination uses an opaque `cursor: String?` from the previous page. The app never parses it. Compat may map it to a page number internally.
- `nextCursor == null` means the last page (or "not paged"). An empty string is never sent. Cursors are per provider and per query. The UI never merges cursors (Agent 3, COMPONENT_CATALOG M2 Q1).

## 2. Reconciliation with the compat spec
Two earlier drafts disagreed (v1 of this file and `EXTENSION_ARCHITECTURE.md` §6.1). This section resolves them. All items are PROPOSED.

| Topic | v1 (this file) | Compat spec (§6.1) | Resolution |
|---|---|---|---|
| Error type | `AppError` (DOMAIN_MODEL §6) | `ProviderError` (Timeout, Unavailable, NoData, Broken, Untrusted) | One error type crosses ports: `AppError`. Compat's five cases become `AppError` cases (see §3.1). |
| Result type | `Outcome.Success` / `Outcome.Failure` | `Outcome.Ok` / `Outcome.Err` | Keep `Success` / `Failure` (names only, no semantic change). |
| Search | Fan-out `Flow<SearchEvent>` in the port | `search(provider, query, page)` per provider | Compat implements a **per-provider** `suspend` search. The fan-out and streaming `Flow` move to a domain use case. CloudStream `search` is per provider (Agent 1 §6.3). |
| Discovery paging | `cursor: String?` | `page: Int` | Keep the opaque `cursor` in the domain port. Compat maps it to a page number. |
| Episodes | Separate `EpisodePort` | Episodes come inside `load` (`LoadResponse`) | `EpisodePort` removed. Episodes are part of `MediaDetails` (seasons). Pending Agent 1 confirmation. |
| Subtitles | Separate `SubtitlePort` | `loadLinks` takes a `subtitleCallback` | Subtitles arrive as `StreamEvent.Subtitle` on the stream flow. No separate call. Pending Agent 1 confirmation. |
| Stream object | `StreamSource` (DOMAIN_MODEL §3) | `StreamCandidate` | Use `StreamSource`. Referer goes into `headers` as `Referer`. No new field. |
| Process model, timeouts, network policy | Open | Open questions §8 | Owner / Claude decision (see §5). |

## 3. Shared types

### 3.1 Errors: `AppError` (DOMAIN_MODEL §6) plus additive cases
```
sealed interface AppError {
  data class Network(val providerId: ProviderId?, val retryable: Boolean) : AppError
  data class Parse(val providerId: ProviderId, val detail: String) : AppError
  data class Unsupported(val providerId: ProviderId, val operation: String) : AppError
  data class ProviderFailed(val providerId: ProviderId, val message: String) : AppError   // extension threw (compat "Broken")
  data object Cancelled : AppError
  data class Unknown(val cause: String?) : AppError
  // PROPOSED additions (additive, so existing `when` branches still compile when they add cases):
  data class Timeout(val providerId: ProviderId, val operation: String) : AppError
  data class NoData(val providerId: ProviderId) : AppError          // null or empty result from provider
  data class Unavailable(val providerId: ProviderId) : AppError     // provider status Down, host unreachable
  data class Untrusted(val providerId: ProviderId, val reason: String) : AppError // hash or signature failure
}
```
Note: `when` over a sealed type is exhaustive, so adding cases is additive only for consumers that use `else`. UI consumers must add branches when this lands. Recorded in ADR-015.

### 3.2 Outcome
```
sealed interface Outcome<out T> {
  data class Success<T>(val value: T) : Outcome<T>
  data class Failure(val error: AppError) : Outcome<Nothing>
}
```
Chosen over `kotlin.Result` so errors stay typed and `Cancelled` is never folded into a failure by accident.

### 3.3 Page
```
data class Page<T>(val items: List<T>, val nextCursor: String?)
```

## 4. Ports

### 4.1 ProviderCatalogPort (C1)
```
fun observeProviders(): Flow<List<ProviderInfo>>
suspend fun provider(id: ProviderId): ProviderInfo?
```
Emits whenever an installed or enabled provider changes. Status is honoured here (Down providers are listed as unavailable, not hidden silently).

### 4.2 DiscoveryPort (C2)
```
suspend fun sections(provider: ProviderId, cursor: String?): Outcome<Page<Section>>
```
Home and categories for one provider. The app combines providers itself.

Shape added for Agent 3 (SCREEN_INVENTORY M2 Q5), PROPOSED, to be added to DOMAIN_MODEL §2:
```
data class Section(
    val title: String,
    val kind: SectionKind,        // GENERIC by default
    val layout: SectionLayout,    // ROW by default; GRID for category tiles
    val items: List<MediaSummary>,
    val nextCursor: String?       // null = last page
)
enum class SectionKind { GENERIC, TRENDING, CATEGORY }
enum class SectionLayout { ROW, GRID }
```
Rule: compat sets `TRENDING` or `CATEGORY` only when the provider declares it. It never infers the kind from a title or a name. Otherwise `GENERIC`.
Recommendations are not a port. The rule that picks them is an owner decision (SCREEN_INVENTORY Q5). Until decided, no screen gets a recommendations port.

### 4.3 ProviderSearchPort (C3, compat-implemented, per provider)
```
suspend fun search(provider: ProviderId, query: String, cursor: String?): Outcome<Page<MediaSummary>>
```

### 4.4 SearchMediaUseCase (C4, domain, fan-out)
```
fun search(query: String, providers: Set<ProviderId>): Flow<SearchEvent>
// SearchEvent: Started(provider) | Results(provider, Page<MediaSummary>) | Failed(provider, AppError) | Finished
```
Calls `ProviderSearchPort` concurrently, one child coroutine per provider, so one slow provider does not block the screen. Failures are per provider. Cancelling the collector cancels all in-flight provider work.

Per Agent 3 (COMPONENT_CATALOG M2 Q2): `Started(provider)` opens one result slot per provider. A `Results` event carries one page. The first page fills the slot, later pages append, and nothing already shown moves. `Finished` is the only terminal event. A flow that ends without `Finished` (cancel) is shown as "Stopped" by the UI.

### 4.5 MediaDetailsPort (C5)
```
suspend fun details(ref: MediaRef): Outcome<MediaDetails>
```
`MediaDetails` carries seasons and episodes (DOMAIN_MODEL §2). For movies, `movieEpisode` is set. Pending Agent 1 confirmation (question 5.1).

### 4.6 StreamResolutionPort (C6)
```
fun streams(episode: EpisodeRef): Flow<StreamEvent>
// StreamEvent: Found(StreamSource) | Subtitle(SubtitleTrack) | Failed(AppError) | Finished
```
Flow because extractors find sources incrementally. The player can start on the first usable stream, and later events are fallbacks (DATA_FLOW §3). Subtitle events may arrive at any point and never block playback.

### 4.7 RepositoryPort (C7, compat-implemented)
```
suspend fun addRepository(address: RepositoryAddress): Outcome<RepositoryInfo>
suspend fun refresh(id: RepositoryId): Outcome<List<ExtensionListing>>
suspend fun install(listing: ExtensionListing): Outcome<InstalledExtension>
suspend fun remove(id: ExtensionId): Outcome<Unit>
fun observeInstalled(): Flow<List<InstalledExtension>>
```
Installation and execution are separate steps. Adding a repository or installing a package never executes extension code (ARCHITECTURE §9, ADR-008). `RepositoryAddress` accepts only HTTPS (proposed, §5 item C11).

## 5. Status table

| ID | Contract | Implemented by | Status | Waiting on |
|---|---|---|---|---|
| C1 | ProviderCatalogPort (§4.1) | Agent 1 | PROPOSED | Agent 1 confirms it can emit on change |
| C2 | DiscoveryPort (§4.2), with Section kind and layout | Agent 1 | PROPOSED | Agent 1 (how CloudStream home pages map to sections, and whether any declare a kind); Claude sign-off on the Section shape. Agent 3 Q1 answered (per-provider cursors, accepted) |
| C3 | ProviderSearchPort (§4.3) | Agent 1 | PROPOSED | Agent 1 Q5.1 (search vs details) |
| C4 | SearchMediaUseCase (§4.4) | Agent 2 | PROPOSED | Agent 3 answered (per-provider slots, accepted). Needs Agent 1 to confirm `search` returns pages |
| C5 | MediaDetailsPort with episodes (§4.5) | Agent 1 | PROPOSED | Agent 1 Q5.1 and episodes question (EpisodePort removal) |
| C6 | StreamResolutionPort with subtitles (§4.6) | Agent 1 | PROPOSED | Agent 1 Q2 and Q3 (incremental, subtitles). Agent 3 Q3 answered: automatic selection plus a source sheet, accepted. Timeouts and retry count are owner values, not set here |
| C7 | RepositoryPort (§4.7) | Agent 1 | NEEDS-LEGAL (implementation); port shape PROPOSED | M3 legal review; owner decision D-A |
| C8 | AppError additions (§3.1) | Agent 2 | PROPOSED | Claude sign-off; Agent 3 must add `when` branches |
| C9 | Per-operation timeouts | Agent 1 + Agent 2 | PROPOSED | Agent 1 Q5.5 (defaults); owner (no defaults chosen here) |
| C10 | Extension process model (in-process or isolated) | Claude / owner | PROPOSED, no decision | EXTENSION_ARCHITECTURE §8 Q5; affects loader placement |
| C11 | HTTPS-only repository and plugin URLs | Agent 1 + Claude | PROPOSED | Owner: may break existing HTTP repositories (EXTENSION_ARCHITECTURE §8 Q3) |
| C12 | ABI shim exposing `com.lagradost.*` names at runtime | Agent 1 | NEEDS-LEGAL | M3 legal review (EXTENSION_ARCHITECTURE §8 Q1, Q2) |
| C13 | Storage locations (`filesDir/extensions/`, `cacheDir`, key-value keys) | Agent 1 + Agent 2 | PROPOSED | Agent 1 storage format; Q5.6 (backup) |
| C14 | UI states for extensions (U1 to U5), and RowState | Agent 3 | PROPOSED | Agent 3 answered RowState (COMPONENT_CATALOG M2 Q1). U1 to U5 not answered |
| C15 | Reduce-transparency setting in DataStore (`ui.glass_mode`) | Agent 3 + Agent 2 | PROPOSED | Agent 3 Q4 answered (DataStore, read in `:app`, accepted). Claude sign-off |
| C16 | Local playback progress and history in Room (for Continue watching) | Agent 2 | PROPOSED | Built in M4 (`:core:data`). Agent 3 SCREEN_INVENTORY Q5: Continue watching is buildable on local data only |

Agreed contracts: **0 of 16.** Agent 3 answers are accepted by Agent 2 (see the table), but the change control in §8 needs Claude's sign-off before any row becomes AGREED.

## 6. Questions for Agent 1 (unanswered)
1. Does CloudStream return search results and details from separate calls? (`search` returns a list; `load` returns `LoadResponse` with episodes, per §6.3.) Confirms C3 and C5.
2. Can `loadLinks` emit incrementally? It is callback-based, which fits a `callbackFlow`. Confirms C6.
3. Are subtitles only delivered through `subtitleCallback` inside `loadLinks`? Confirms removal of `SubtitlePort`.
4. Which operations can an extension legitimately leave unimplemented? Sets the `Unsupported` cases.
5. Default timeouts per operation (C9), and where provider code runs (C10).
6. Does the app need to back up the repository and extension store? If yes, what must the format guarantee (C13)?
7. `DEPENDENCY_MAP §3` names the OkHttp `Interceptor` but gives no OkHttp version. Which OkHttp, Jackson, jsoup and Kotlin versions does the ABI expect at runtime? This fixes ADR-012 and the `libs.versions.toml` pins. (Also: the catalog has coroutines 1.9.0, while DEPENDENCY_MAP §2 lists 1.11.0. Which should HUSH use?)

## 7. Questions from Agent 3 (source: COMPONENT_CATALOG M2 and SCREEN_INVENTORY M2)
1. Paging: per-provider cursor or merged cursor? **Answered: per-provider. Accepted (§1, C2).**
2. Search: per-provider slots or a merged list? **Answered: per-provider slots, fixed order. Accepted (§4.4, C4).**
3. Player: stream picker or automatic selection? **Answered: automatic, with a source sheet during playback. Accepted (C6). Owner must set the first-stream timeout and the retry count.**
4. Reduce-transparency: DataStore? **Answered: DataStore, read in `:app`, key `ui.glass_mode`. Accepted (C15).**
5. Section kind (TRENDING, CATEGORY), categories, and who owns the recommendation rule? **Section kind and layout added (§4.2, C2). Categories use `SectionLayout.GRID`, not a new port. The recommendation rule is an owner decision (open).**
6. Does skeleton loading need state beyond `Flow` emissions? **Not answered directly. Agent 3's RowState is feature-layer state built from port results, so no port change is needed.**
7. Agent 3 asks whether `Page.nextCursor` is `null` or empty at the end. **Answered: `null` only (§1).**

## 8. Change control
- A port change needs Agent 1 and Agent 2 agreement, recorded in `docs/architecture/DECISIONS.md`.
- Renaming a port method after implementation is breaking for `:core:compat`. Prefer additive changes.
- No contract moves to AGREED without the named answer in §6 or §7 and a Claude sign-off in DECISION_LOG.
