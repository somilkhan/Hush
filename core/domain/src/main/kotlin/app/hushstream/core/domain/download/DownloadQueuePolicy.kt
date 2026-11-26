/*
 * Adapted from NuvioMobile-Enhanced (GPL-3.0), commit 7aca5a0,
 * path composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadQueuePolicy.kt.
 * Changes: the network flag is passed in (no settings singleton); `current()` is removed
 * and will return in the settings slice (S2). Package renamed to app.hushstream.
 */
package app.hushstream.core.domain.download

/** Decides which downloads may run, given the user's concurrency limits and network policy. */
data class DownloadQueuePolicy(
    val maxDownloads: Int,
    val maxEpisodesPerShow: Int,
    val maxShows: Int,
    val isNetworkAllowed: Boolean,
) {
    /** Whether a download for [showKey] can start next to [downloading] without exceeding the limits. */
    fun canStart(showKey: String?, downloading: List<DownloadItem>): Boolean {
        if (!isNetworkAllowed) return false
        if (downloading.size >= maxDownloads) return false
        if (showKey == null) return true
        val activeShows = downloading.mapNotNullTo(mutableSetOf()) { it.showKey }
        if (showKey !in activeShows && activeShows.size >= maxShows) return false
        return downloading.count { it.showKey == showKey } < maxEpisodesPerShow
    }

    /** The oldest queued download that may start now, or null when none can. */
    fun nextToStart(items: List<DownloadItem>): DownloadItem? {
        val downloading = items.filter { it.status == DownloadStatus.Downloading }
        return items
            .filter { it.status == DownloadStatus.Queued }
            .sortedWith(queueOrder)
            .firstOrNull { canStart(it.showKey, downloading) }
    }

    companion object {
        private val queueOrder = compareBy<DownloadItem> { it.createdAtEpochMs }
            .thenBy { it.seasonNumber ?: 0 }
            .thenBy { it.episodeNumber ?: 0 }
            .thenBy { it.id }
    }
}
