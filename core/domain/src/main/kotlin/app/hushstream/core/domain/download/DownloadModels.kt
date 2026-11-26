/*
 * Adapted from NuvioMobile-Enhanced (GPL-3.0), commit 7aca5a0,
 * path composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsModels.kt
 * (subset: status, item, concurrency limits, status counts). Player, subtitle and
 * Compose resource fields are NOT ported here; they arrive in later slices (see docs/architecture/DOWNLOADS_PLAN.md).
 * Changes: package renamed to app.hushstream; serialization and Compose removed (pure JVM module).
 */
package app.hushstream.core.domain.download

enum class DownloadStatus { Queued, Downloading, Paused, Completed, Failed }

/** A user-adjustable cap on how many downloads may run at once. */
enum class DownloadConcurrencyLimit(val storageKey: String, val defaultValue: Int) {
    TotalDownloads("max_download_concurrency", 3),
    EpisodesPerShow("max_series_episode_concurrency", 1),
    ConcurrentShows("max_concurrent_shows", 1);

    fun clamp(value: Int): Int = value.coerceIn(1, MAX_VALUE)

    companion object {
        const val MAX_VALUE = 5
    }
}

data class DownloadItem(
    val id: String,
    val contentType: String,
    val parentMetaId: String,
    val videoId: String,
    val title: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val sourceUrl: String,
    val localFileUri: String? = null,
    val fileName: String,
    val status: DownloadStatus,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val errorMessage: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
) {
    val contentId: String
        get() = parentMetaId.trim().ifBlank { id }

    val isEpisode: Boolean
        get() = seasonNumber != null && episodeNumber != null

    val isActive: Boolean
        get() = status == DownloadStatus.Downloading || status == DownloadStatus.Queued

    /** Paused or failed: resuming continues a paused download and retries a failed one. */
    val isResumable: Boolean
        get() = status == DownloadStatus.Paused || status == DownloadStatus.Failed

    val isPlayable: Boolean
        get() = status == DownloadStatus.Completed && !localFileUri.isNullOrBlank()

    val progressFraction: Float
        get() {
            val total = totalBytes?.takeIf { it > 0L } ?: return 0f
            return (downloadedBytes.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
        }

    /** The show an episode download counts against for per-show limits; null for movies. */
    val showKey: String?
        get() = parentMetaId.takeIf { isEpisode && it.isNotBlank() }

    val logicalContentKey: String
        get() = if (isEpisode) {
            "${parentMetaId.trim()}|${seasonNumber ?: -1}|${episodeNumber ?: -1}"
        } else {
            "${parentMetaId.trim()}|movie"
        }
}

/** How many downloads are in each unfinished state. */
data class DownloadStatusCounts(
    val downloading: Int = 0,
    val queued: Int = 0,
    val paused: Int = 0,
    val failed: Int = 0,
) {
    val hasActive: Boolean get() = downloading > 0 || queued > 0
}

fun List<DownloadItem>.statusCounts(): DownloadStatusCounts {
    val byStatus = groupingBy { it.status }.eachCount()
    return DownloadStatusCounts(
        downloading = byStatus[DownloadStatus.Downloading] ?: 0,
        queued = byStatus[DownloadStatus.Queued] ?: 0,
        paused = byStatus[DownloadStatus.Paused] ?: 0,
        failed = byStatus[DownloadStatus.Failed] ?: 0,
    )
}
