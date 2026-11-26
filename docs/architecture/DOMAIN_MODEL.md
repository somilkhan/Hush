# HUSH — Domain model

Status: PROVISIONAL. Names and shapes are the app's own view. They must be mapped from the real CloudStream contracts by the compat adapter (Agent 1). Do not treat these as the authoritative CloudStream types.
Code: `core/domain/src/main/kotlin/app/hushstream/core/domain/model/` (identifiers only so far).

## 1. Identity
- `ProviderId`: opaque, stable string for an installed provider. Survives app restarts.
- `MediaRef(providerId, key)`: the only cross-layer identity for a title. `key` is opaque to the app.
- `EpisodeRef(media, season?, episode)`: addresses an episode. `season` is null for media without seasons.

## 2. Catalogue
- `MediaKind`: `MOVIE`, `SERIES`, `ANIME`, `OTHER`. A provider's own type labels are mapped here by compat.
- `MediaSummary`: `ref`, `kind`, `title`, `year?`, `posterUrl?`, `backdropUrl?`.
- `MediaDetails`: `summary`, `overview?`, `genres`, `rating?`, `status?`, `seasons` (for series), `movieEpisode?` (for movies), `related: List<MediaSummary>`.
- `Season`: `number`, `title?`, `episodes: List<Episode>`.
- `Episode`: `ref`, `title?`, `airDate?`, `thumbnailUrl?`, `overview?`.
- `Section` and `Page<T>`: discovery rows and paged results, each carrying an opaque `nextCursor?`.

## 3. Playback
- `StreamSource`: `url`, `kind` (`HLS`, `DASH`, `PROGRESSIVE`, `OTHER`), `quality?`, `headers`, `providerId`, `label`.
- `SubtitleTrack`: `language?`, `url`, `format` (`SRT`, `VTT`, `OTHER`), `label`, `providerId`.
- `PlaybackProgress`: `episodeRef`, `positionMs`, `durationMs`, `updatedAtEpochMs`. Completed when position is at or above 90 percent of duration (threshold to confirm with Agent 3).

## 4. User data
- `HistoryEntry`: `media: MediaRef`, `episodeRef?`, `progress`, `lastWatchedEpochMs`.
- `Favourite`: `media: MediaRef`, `addedEpochMs`.
- `Bookmark`: `media: MediaRef`, `label?`, `addedEpochMs`. Distinct from favourites: bookmarks can point at a specific episode and time.
- `DownloadItem`: `episodeRef`, `state` (`QUEUED`, `RUNNING`, `DONE`, `FAILED`), `bytesDone`, `bytesTotal?`, `localPath?`. Downloads are deferred until playback is solid.

## 5. Providers and repositories (app view)
- `ProviderInfo`: `id`, `name`, `version`, `languages`, `supportedKinds`, `capabilities` (search, details, episodes, streams, subtitles).
- `RepositoryInfo`: `id`, `displayName`, `sourceUrl`, `status` (`OK`, `STALE`, `ERROR`), `lastRefreshedEpochMs?`.
- `ExtensionInfo`: `id`, `repositoryId`, `version`, `status`, `providers`. Installation state is owned by compat, observable through ports.

## 6. Errors
```
sealed interface AppError {
  data class Network(val providerId: ProviderId?, val retryable: Boolean) : AppError
  data class Parse(val providerId: ProviderId, val detail: String) : AppError
  data class Unsupported(val providerId: ProviderId, val operation: String) : AppError
  data class ProviderFailed(val providerId: ProviderId, val message: String) : AppError
  data object Cancelled : AppError   // Not shown to users. Mapped from CancellationException where needed.
  data class Unknown(val cause: String?) : AppError
}
```
Rule: compat maps every extension exception into `AppError`. Stack traces stay in logs, not in state.

## 7. Invariants
- All models are immutable `data class`es or value classes with no mutable collections exposed.
- Collections are `List`, never `MutableList`, at module boundaries.
- Ids are never parsed by the UI. Only the owning port interprets a `key`.
- Timestamps are epoch milliseconds (`Long`). Durations are milliseconds.
- Language codes are BCP-47 strings.

## 8. Open questions for Agent 1
- Which CloudStream concepts correspond to `MediaKind`, `MediaSummary` and `MediaDetails`? In particular: does CloudStream separate search results from full details the way this model does?
- Are episode lists always available in details, or only through a separate call?
- How are stream "kinds" and extractor link types represented, and does one provider call return multiple streams with per-stream headers?
