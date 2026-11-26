package app.hushstream.core.data

import app.hushstream.core.domain.model.MediaItem
import app.hushstream.core.domain.model.StreamSource
import app.hushstream.core.domain.repository.MediaRepository

/** Used when no addon is installed. Shows no titles at all, so nothing fake reaches the UI. */
class EmptyMediaRepository : MediaRepository {
    override suspend fun featured(): List<MediaItem> = emptyList()
    override suspend fun search(query: String): List<MediaItem> = emptyList()
    override suspend fun details(id: String): MediaItem? = null
    override suspend fun streams(item: MediaItem, episodeId: String): List<StreamSource> = emptyList()
}
