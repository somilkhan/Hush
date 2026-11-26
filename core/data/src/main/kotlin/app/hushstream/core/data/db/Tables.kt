package app.hushstream.core.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/** Saved title. `kind` stores MediaKind.name. */
@Entity(tableName = "library")
data class LibraryRow(
    @androidx.room.PrimaryKey val mediaId: String,
    val kind: String,
    val title: String,
    val posterUrl: String?,
    val saved: Boolean,
    val updatedAtEpochMs: Long,
)

/** Playback position per (mediaId, episodeId). Empty episodeId for movies. */
@Entity(tableName = "watch_history", primaryKeys = ["mediaId", "episodeId"])
data class HistoryRow(
    val mediaId: String,
    val episodeId: String,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val updatedAtEpochMs: Long,
    val title: String,
)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library")
    suspend fun all(): List<LibraryRow>

    @Upsert
    suspend fun upsert(row: LibraryRow)

    @Query("DELETE FROM library WHERE mediaId = :mediaId")
    suspend fun remove(mediaId: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY updatedAtEpochMs ASC")
    suspend fun all(): List<HistoryRow>

    @Upsert
    suspend fun upsert(row: HistoryRow)

    /** Keeps the newest `keep` rows. Uses rowid, which works on SQLite older than row-value IN (API 24). */
    @Query("DELETE FROM watch_history WHERE rowid NOT IN (SELECT rowid FROM watch_history ORDER BY updatedAtEpochMs DESC LIMIT :keep)")
    suspend fun trim(keep: Int)
}
