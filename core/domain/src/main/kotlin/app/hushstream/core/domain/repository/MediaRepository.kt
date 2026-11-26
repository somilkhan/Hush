package app.hushstream.core.domain.repository

import app.hushstream.core.domain.model.MediaItem
import app.hushstream.core.domain.model.StreamSource

interface MediaRepository {
    suspend fun featured(): List<MediaItem>
    suspend fun search(query: String): List<MediaItem>
    suspend fun details(id: String): MediaItem?
    /** Playable streams for one episode (or the movie itself). Addon streams are resolved on demand. */
    suspend fun streams(item: MediaItem, episodeId: String): List<StreamSource>
}
