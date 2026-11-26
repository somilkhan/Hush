package app.hushstream.core.domain.download

/** Device-local store of downloads. Implemented in core:data with Room. */
interface DownloadRepository {
    /** All downloads, oldest first. */
    suspend fun all(): List<DownloadItem>

    suspend fun byId(id: String): DownloadItem?

    /** Inserts or replaces the download with the same [DownloadItem.id]. */
    suspend fun upsert(item: DownloadItem)

    suspend fun remove(id: String)
}
