package app.hush.placeholder.core.domain.sync

import app.hush.placeholder.core.domain.model.MediaKind

/** A saved title. Updated time is epoch millis and decides which copy wins on sync. */
data class LibraryEntry(
    val mediaId: String,
    val kind: MediaKind,
    val title: String,
    val posterUrl: String?,
    val saved: Boolean,
    val updatedAtEpochMs: Long,
)

/** Playback position for one episode (or movie, with an empty episodeId). */
data class WatchEntry(
    val mediaId: String,
    val episodeId: String,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val updatedAtEpochMs: Long,
)

/** Local-first store. The UI reads only from here; sync runs underneath. */
interface LibraryRepository {
    suspend fun all(): List<LibraryEntry>
    suspend fun upsert(entry: LibraryEntry)
    suspend fun remove(mediaId: String)
}

interface WatchHistoryRepository {
    suspend fun all(): List<WatchEntry>
    suspend fun record(entry: WatchEntry)
}
