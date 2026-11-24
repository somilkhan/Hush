package app.hush.placeholder.core.domain.repository

import app.hush.placeholder.core.domain.model.MediaItem

interface MediaRepository {
    suspend fun featured(): List<MediaItem>
    suspend fun search(query: String): List<MediaItem>
    suspend fun details(id: String): MediaItem?
}
