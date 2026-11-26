package app.hushstream.core.domain.sync

import app.hushstream.core.domain.model.MediaKind

/** A saved title. Updated time is epoch millis and decides which copy wins on sync. */
data class LibraryEntry(
    val mediaId: String,
    val kind: MediaKind,
    val title: String,
    val posterUrl: String?,
    val saved: Boolean,
    val updatedAtEpochMs: Long,
    /** Set when the row was deleted. A tombstone wins a sync conflict by updatedAt like any other row. */
    val deletedAtEpochMs: Long? = null,
)

/** Playback position for one episode (or movie, with an empty episodeId). */
data class WatchEntry(
    val mediaId: String,
    val episodeId: String,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val updatedAtEpochMs: Long,
    /** Display title, so Continue watching works without loading the catalogue. */
    val title: String = "",
    /** Set when the row was deleted. Not yet applied locally: history has no remove API yet (see SYNC_STATUS). */
    val deletedAtEpochMs: Long? = null,
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
