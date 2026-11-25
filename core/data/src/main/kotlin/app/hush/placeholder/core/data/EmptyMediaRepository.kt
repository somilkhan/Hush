package app.hush.placeholder.core.data

import app.hush.placeholder.core.domain.model.MediaItem
import app.hush.placeholder.core.domain.model.StreamSource
import app.hush.placeholder.core.domain.repository.MediaRepository

/** Used when no addon is installed. Shows no titles at all, so nothing fake reaches the UI. */
class EmptyMediaRepository : MediaRepository {
    override suspend fun featured(): List<MediaItem> = emptyList()
    override suspend fun search(query: String): List<MediaItem> = emptyList()
    override suspend fun details(id: String): MediaItem? = null
    override suspend fun streams(item: MediaItem, episodeId: String): List<StreamSource> = emptyList()
}
