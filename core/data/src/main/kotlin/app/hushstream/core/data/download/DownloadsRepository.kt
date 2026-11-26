package app.hushstream.core.data.download

import android.content.Context
import app.hushstream.core.data.db.DownloadDao
import app.hushstream.core.data.db.DownloadRow
import app.hushstream.core.data.db.HushDatabase
import app.hushstream.core.domain.download.DownloadItem
import app.hushstream.core.domain.download.DownloadRepository
import app.hushstream.core.domain.download.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Room-backed [DownloadRepository]. Device-local; nothing here is synced. */
class DownloadsRepository(private val dao: DownloadDao) : DownloadRepository {

    override suspend fun all(): List<DownloadItem> = withContext(Dispatchers.IO) {
        dao.all().map { it.toItem() }
    }

    override suspend fun byId(id: String): DownloadItem? = withContext(Dispatchers.IO) {
        dao.byId(id)?.toItem()
    }

    override suspend fun upsert(item: DownloadItem) = withContext(Dispatchers.IO) {
        dao.upsert(item.toRow())
    }

    override suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        dao.remove(id)
    }

    companion object {
        fun create(context: Context): DownloadsRepository =
            DownloadsRepository(HushDatabase.get(context).downloadDao())
    }
}

internal fun DownloadItem.toRow() = DownloadRow(
    id = id,
    contentType = contentType,
    parentMetaId = parentMetaId,
    videoId = videoId,
    title = title,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    episodeTitle = episodeTitle,
    sourceUrl = sourceUrl,
    localFileUri = localFileUri,
    fileName = fileName,
    status = status.name,
    downloadedBytes = downloadedBytes,
    totalBytes = totalBytes,
    errorMessage = errorMessage,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

internal fun DownloadRow.toItem() = DownloadItem(
    id = id,
    contentType = contentType,
    parentMetaId = parentMetaId,
    videoId = videoId,
    title = title,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    episodeTitle = episodeTitle,
    sourceUrl = sourceUrl,
    localFileUri = localFileUri,
    fileName = fileName,
    // An unknown stored status is shown as failed, so the user can retry or remove it.
    status = DownloadStatus.entries.firstOrNull { it.name == status } ?: DownloadStatus.Failed,
    downloadedBytes = downloadedBytes,
    totalBytes = totalBytes,
    errorMessage = errorMessage,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)
