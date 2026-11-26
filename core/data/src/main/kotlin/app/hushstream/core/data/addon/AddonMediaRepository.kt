package app.hushstream.core.data.addon

import app.hushstream.core.domain.addon.AddonRegistry
import app.hushstream.core.domain.addon.InstalledAddon
import app.hushstream.core.domain.model.Episode
import app.hushstream.core.domain.model.MediaItem
import app.hushstream.core.domain.model.MediaKind
import app.hushstream.core.domain.model.StreamSource
import app.hushstream.core.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Catalogue from user-added Stremio-protocol addons.
 * With no addon installed it delegates to [fallback] (demo data, flagged isDemo).
 * Each addon is queried in isolation, so one failing addon never empties the others (ADR-018).
 */
class AddonMediaRepository(
    private val registry: AddonRegistry,
    private val client: StremioClient,
    private val fallback: MediaRepository,
) : MediaRepository {

    override suspend fun featured(): List<MediaItem> {
        val addons = registry.all()
        if (addons.isEmpty()) return fallback.featured()
        return withContext(Dispatchers.IO) {
            addons.flatMap { safeCatalog(it, search = null) }
                .distinctBy { it.id }
                .take(MAX_FEATURED)
        }
    }

    override suspend fun search(query: String): List<MediaItem> {
        val addons = registry.all()
        if (addons.isEmpty()) return fallback.search(query)
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            addons.flatMap { safeCatalog(it, search = term) }.distinctBy { it.id }
        }
    }

    override suspend fun details(id: String): MediaItem? {
        val key = ItemKey.decode(id) ?: return fallback.details(id)
        return withContext(Dispatchers.IO) {
            runCatching {
                val meta = client.meta(key.base, key.type, key.metaId).optJSONObject("meta")
                    ?: return@runCatching null
                toDetails(key, meta)
            }.getOrNull()
        }
    }

    override suspend fun streams(item: MediaItem, episodeId: String): List<StreamSource> {
        val key = ItemKey.decode(item.id) ?: return fallback.streams(item, episodeId)
        val type = if (item.kind == MediaKind.SERIES) "series" else "movie"
        return withContext(Dispatchers.IO) {
            runCatching {
                val streams = client.streams(key.base, type, episodeId).optJSONArray("streams")
                    ?: return@runCatching emptyList<StreamSource>()
                (0 until streams.length()).mapNotNull { i ->
                    val s = streams.optJSONObject(i) ?: return@mapNotNull null
                    // Only direct https URLs. Torrent/infoHash streams are not supported yet.
                    val url = s.optString("url").takeIf { it.startsWith("https://") } ?: return@mapNotNull null
                    StreamSource(
                        url = url,
                        label = s.optString("title").ifBlank { s.optString("name") }.ifBlank { "Stream" },
                    )
                }
            }.getOrDefault(emptyList())
        }
    }

    private fun safeCatalog(addon: InstalledAddon, search: String?): List<MediaItem> =
        runCatching { catalogItems(addon, search) }.getOrDefault(emptyList())

    private fun catalogItems(addon: InstalledAddon, search: String?): List<MediaItem> {
        val manifest = client.manifest(addon.manifestUrl)
        val catalogs = manifest.optJSONArray("catalogs") ?: return emptyList()
        val usable = (0 until catalogs.length())
            .mapNotNull { catalogs.optJSONObject(it) }
            .filter { it.optString("type") in SUPPORTED_TYPES && it.optString("id").isNotBlank() }
            .filter { search == null || supportsSearch(it) }
            .take(MAX_CATALOGS_PER_ADDON)
        return usable.flatMap { catalog ->
            val type = catalog.optString("type")
            val metas = client.catalog(addon.base, type, catalog.optString("id"), search)
                .optJSONArray("metas") ?: return@flatMap emptyList<MediaItem>()
            (0 until metas.length()).mapNotNull { i ->
                metas.optJSONObject(i)?.let { toSummary(addon.base, type, it) }
            }
        }
    }

    private fun supportsSearch(catalog: JSONObject): Boolean {
        val extra = catalog.optJSONArray("extra")
        if (extra != null) {
            for (i in 0 until extra.length()) {
                if (extra.optJSONObject(i)?.optString("name") == "search") return true
            }
        }
        val supported = catalog.optJSONArray("extraSupported")
        if (supported != null) {
            for (i in 0 until supported.length()) {
                if (supported.optString(i) == "search") return true
            }
        }
        return false
    }

    private fun toSummary(base: String, type: String, meta: JSONObject): MediaItem? {
        val metaId = meta.optString("id").takeIf { it.isNotBlank() } ?: return null
        return MediaItem(
            id = ItemKey(base, type, metaId).encode(),
            title = meta.optString("name", metaId),
            synopsis = meta.optString("description"),
            year = yearOf(meta.optString("releaseInfo")),
            kind = kindOf(type),
            episodes = emptyList(),
            isDemo = false,
        )
    }

    private fun toDetails(key: ItemKey, meta: JSONObject): MediaItem {
        val kind = kindOf(key.type)
        val videos = meta.optJSONArray("videos")
        val episodes = if (kind == MediaKind.SERIES && videos != null) {
            (0 until videos.length()).mapNotNull { i ->
                val v = videos.optJSONObject(i) ?: return@mapNotNull null
                val videoId = v.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val number = v.optInt("episode", i + 1)
                Episode(
                    id = videoId,
                    season = v.optInt("season", 0),
                    number = number,
                    title = v.optString("title").ifBlank { "Episode $number" },
                    streams = emptyList(),
                )
            }.sortedWith(compareBy<Episode>({ it.season }, { it.number }))
        } else {
            listOf(
                Episode(
                    id = key.metaId,
                    season = 1,
                    number = 1,
                    title = meta.optString("name", key.metaId),
                    streams = emptyList(),
                ),
            )
        }
        return MediaItem(
            id = key.encode(),
            title = meta.optString("name", key.metaId),
            synopsis = meta.optString("description"),
            year = yearOf(meta.optString("releaseInfo")),
            kind = kind,
            episodes = episodes,
            isDemo = false,
        )
    }

    private fun kindOf(type: String): MediaKind = if (type == "series") MediaKind.SERIES else MediaKind.MOVIE

    private fun yearOf(releaseInfo: String): Int =
        Regex("""\d{4}""").find(releaseInfo)?.value?.toIntOrNull() ?: 0

    private companion object {
        const val MAX_FEATURED = 40
        const val MAX_CATALOGS_PER_ADDON = 4
        val SUPPORTED_TYPES = setOf("movie", "series")
    }
}
