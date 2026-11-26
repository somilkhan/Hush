package app.hushstream.core.data

import app.hushstream.core.domain.model.Episode
import app.hushstream.core.domain.model.MediaItem
import app.hushstream.core.domain.model.MediaKind
import app.hushstream.core.domain.model.StreamSource
import app.hushstream.core.domain.repository.MediaRepository

/**
 * Static, deterministic demo catalogue. No network calls.
 * Every item has [MediaItem.isDemo] = true. Titles and synopses are original.
 * Every stream points at a public Google test sample (Big Buck Bunny), not at the titles' content.
 */
class DemoMediaRepository : MediaRepository {

    private val catalogue: List<MediaItem> = listOf(
        movie(
            id = "demo-movie-lantern",
            title = "The Lantern Keeper",
            synopsis = "A lighthouse keeper finds a message sealed in a bottle and sets out to answer it.",
            year = 2019,
        ),
        movie(
            id = "demo-movie-harbour",
            title = "Quiet Harbour",
            synopsis = "A town that never locks its doors learns why one door has started to close.",
            year = 2021,
        ),
        series(
            id = "demo-series-glasswater",
            title = "Glasswater",
            synopsis = "Three cartographers map a river that changes course every night.",
            year = 2023,
            episodeTitles = listOf(
                1 to listOf("Source", "Confluence", "Low Tide"),
                2 to listOf("Survey", "Bridgehead"),
            ),
        ),
        series(
            id = "demo-series-northbound",
            title = "Northbound Signal",
            synopsis = "A radio operator on a remote station picks up a voice that repeats a single number.",
            year = 2024,
            episodeTitles = listOf(
                1 to listOf("Static", "Callsign"),
            ),
        ),
    )

    override suspend fun featured(): List<MediaItem> = catalogue

    override suspend fun search(query: String): List<MediaItem> {
        val needle = query.trim()
        if (needle.isEmpty()) return catalogue
        return catalogue.filter { it.title.contains(needle, ignoreCase = true) }
    }

    override suspend fun details(id: String): MediaItem? = catalogue.firstOrNull { it.id == id }

    private companion object {
        const val STREAM_URL =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        const val STREAM_LABEL = "Demo 720p"

        fun stream(): List<StreamSource> = listOf(StreamSource(url = STREAM_URL, label = STREAM_LABEL))

        fun movie(id: String, title: String, synopsis: String, year: Int) = MediaItem(
            id = id,
            title = title,
            synopsis = synopsis,
            year = year,
            kind = MediaKind.MOVIE,
            episodes = listOf(
                Episode(id = "$id-main", season = 1, number = 1, title = title, streams = stream()),
            ),
            isDemo = true,
        )

        fun series(
            id: String,
            title: String,
            synopsis: String,
            year: Int,
            episodeTitles: List<Pair<Int, List<String>>>,
        ) = MediaItem(
            id = id,
            title = title,
            synopsis = synopsis,
            year = year,
            kind = MediaKind.SERIES,
            episodes = episodeTitles.flatMap { (season, titles) ->
                titles.mapIndexed { index, episodeTitle ->
                    val number = index + 1
                    Episode(
                        id = "$id-s${season}e$number",
                        season = season,
                        number = number,
                        title = episodeTitle,
                        streams = stream(),
                    )
                }
            },
            isDemo = true,
        )
    }

    override suspend fun streams(item: MediaItem, episodeId: String): List<StreamSource> =
        item.episodes.firstOrNull { it.id == episodeId }?.streams ?: emptyList()
}
