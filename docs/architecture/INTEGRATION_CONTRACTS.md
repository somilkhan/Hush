# HUSH — Integration contracts (application ports)

Status: PROVISIONAL DRAFT. NOT AGREED. Must go to Agent 1 for review before any implementation of a dependent integration. Agent 1 may change these to match real CloudStream contracts; that is expected and preferred over inventing incompatible APIs.

Principle: these ports describe what the app needs, not how CloudStream works. Where an operation has no CloudStream equivalent, compat must say so, and the port returns `Unsupported` rather than faking a result.

Location once agreed: `core/domain/.../port/` (interfaces only). Implementations live in `:core:compat`.

## 1. Conventions
- All operations are `suspend` (or return `Flow` where noted). They are main-safe.
- Results are `Result`-like domain values: success value or `AppError` (DOMAIN_MODEL.md section 6). Ports do not throw for expected failures.
- Cancellation propagates. Compat must not swallow `CancellationException`.
- Every operation names its provider explicitly or is fanned out by the caller. Ports never pick a provider silently.
- Pagination uses an opaque `cursor` string supplied by the previous page. The app never interprets it.

## 2. Ports

### 2.1 ProviderCatalogPort
```
fun observeProviders(): Flow<List<ProviderInfo>>
suspend fun provider(id: ProviderId): ProviderInfo?
```
Emits whenever installed or enabled providers change.

### 2.2 DiscoveryPort
```
suspend fun sections(provider: ProviderId, cursor: String?): Outcome<Page<Section>>
```
Home and categories for one provider. The app combines providers itself.

### 2.3 SearchPort
```
fun search(query: String, providers: Set<ProviderId>): Flow<SearchEvent>
// SearchEvent: Started(provider) | Results(provider, List<MediaSummary>) | Failed(provider, AppError) | Finished
```
Streams per-provider results so one slow provider does not block the screen. Failures are per provider. Cancelling the collector cancels all in-flight provider work.

### 2.4 MediaDetailsPort
```
suspend fun details(ref: MediaRef): Outcome<MediaDetails>
```

### 2.5 EpisodePort
```
suspend fun episodes(ref: MediaRef, season: Int?): Outcome<List<Episode>>
```
Used only if details does not already include episodes. Agent 1 to confirm which is true for CloudStream providers.

### 2.6 StreamResolutionPort
```
fun streams(episode: EpisodeRef): Flow<StreamEvent>
// StreamEvent: Found(StreamSource) | Failed(AppError) | Finished
```
Flow because extractors may find sources incrementally. The player can start on the first usable stream. Stream headers (referer, user agent) travel inside `StreamSource`.

### 2.7 SubtitlePort
```
suspend fun subtitles(episode: EpisodeRef, stream: StreamSource?): Outcome<List<SubtitleTrack>>
```
Empty list means no subtitles. `Unsupported` means the provider has no subtitle capability at all. The UI must distinguish the two.

### 2.8 RepositoryPort (extension management, owned by compat)
```
suspend fun addRepository(sourceUrl: String): Outcome<RepositoryInfo>
suspend fun refresh(id: RepositoryId): Outcome<RepositoryInfo>
fun observeRepositories(): Flow<List<RepositoryInfo>>
suspend fun installExtension(repo: RepositoryId, extension: String): Outcome<ExtensionInfo>
suspend fun uninstallExtension(id: String): Outcome<Unit>
```
Installation and execution are separate steps. Adding a repository must not execute extension code (see ARCHITECTURE.md section 9).

## 3. Outcome type
```
sealed interface Outcome<out T> {
  data class Success<T>(val value: T) : Outcome<T>
  data class Failure(val error: AppError) : Outcome<Nothing>
}
```
Chosen over `kotlin.Result` so errors stay typed and `Cancelled` is never accidentally folded into a failure.

## 4. Ownership and status

| Port | Owner of implementation | Agreed with Agent 1 | Agreed with Agent 3 (UI needs) |
|---|---|---|---|
| ProviderCatalogPort | Agent 1 (compat) | NO | NO |
| DiscoveryPort | Agent 1 | NO | NO |
| SearchPort | Agent 1 | NO | NO |
| MediaDetailsPort | Agent 1 | NO | NO |
| EpisodePort | Agent 1 | NO | NO |
| StreamResolutionPort | Agent 1 | NO | NO |
| SubtitlePort | Agent 1 | NO | NO |
| RepositoryPort | Agent 1 | NO | NO |

No port is implemented yet. Nothing in `:app` or `:feature:*` may depend on any of them until its row is agreed.

## 5. Questions for Agent 1
1. Does the real CloudStream contract return search results and details from one call or two? This decides whether SearchPort and MediaDetailsPort need the shapes above.
2. Can a stream resolution emit results incrementally, or is it a single call returning a list? If single call, StreamResolutionPort becomes a `suspend` function returning a list.
3. Does subtitle extraction happen inside stream resolution or as a separate step? If inside, SubtitlePort may be redundant.
4. Which operations can a given extension legitimately not implement? This determines the `Unsupported` cases.
5. Where does provider execution run (process, thread, class loader)? Ports are main-safe, but the cost and isolation model affects cancellation and timeouts. Please specify default timeouts.
6. Repository and extension storage format: owned by compat, but does the app need to back up or restore it? If so, what must the format guarantee?

## 6. Questions for Agent 3
1. Which screens need paging, and do they need one cursor per provider or a merged cursor?
2. Should search show provider-by-provider progress, or a single merged list with a per-provider failure badge?
3. Does the player need stream choice before playback (a picker) or automatic selection with a fallback?
4. Where does the reduced-transparency preference live? This doc assumes DataStore, read by `:app` and passed into the design system.
5. Is there any state (for example, skeleton loading) that the ports must expose beyond `Flow` emissions?

## 7. Change control
- A port change requires Agent 1 and Agent 2 agreement, recorded in `docs/architecture/DECISIONS.md`.
- Renaming a port method after implementation is a breaking change for `:core:compat`. Prefer additive changes.
