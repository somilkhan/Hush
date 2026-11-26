package app.hushstream.core.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * One download. Mirrors `DownloadItem` from core:domain. `status` stores DownloadStatus.name.
 * Table schema must match [HushDatabase.MIGRATION_1_2] exactly, or Room rejects the database on open.
 */
@Entity(tableName = "downloads")
data class DownloadRow(
    @PrimaryKey val id: String,
    val contentType: String,
    val parentMetaId: String,
    val videoId: String,
    val title: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val episodeTitle: String?,
    val sourceUrl: String,
    val localFileUri: String?,
    val fileName: String,
    val status: String,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAtEpochMs ASC")
    suspend fun all(): List<DownloadRow>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): DownloadRow?

    @Upsert
    suspend fun upsert(row: DownloadRow)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun remove(id: String)
}
