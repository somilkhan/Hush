package app.hushstream.core.domain.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadQueuePolicyTest {

    private fun item(
        id: String,
        status: DownloadStatus = DownloadStatus.Queued,
        show: String? = null,
        season: Int? = null,
        episode: Int? = null,
        created: Long = 0L,
    ) = DownloadItem(
        id = id,
        contentType = if (show != null) "series" else "movie",
        parentMetaId = show ?: "movie-$id",
        videoId = id,
        title = id,
        seasonNumber = season,
        episodeNumber = episode,
        sourceUrl = "https://example.com/$id.mp4",
        fileName = "$id.mp4",
        status = status,
        createdAtEpochMs = created,
        updatedAtEpochMs = created,
    )

    private fun policy(
        total: Int = 3,
        perShow: Int = 1,
        shows: Int = 1,
        network: Boolean = true,
    ) = DownloadQueuePolicy(total, perShow, shows, network)

    @Test
    fun blocksEverythingWhenNetworkNotAllowed() {
        assertFalse(policy(network = false).canStart(null, emptyList()))
        assertNull(policy(network = false).nextToStart(listOf(item("a"))))
    }

    @Test
    fun respectsTotalDownloadLimit() {
        val running = listOf(item("r1", DownloadStatus.Downloading), item("r2", DownloadStatus.Downloading))
        assertFalse(policy(total = 2).canStart(null, running))
        assertTrue(policy(total = 3).canStart(null, running))
    }

    @Test
    fun perShowLimitBlocksSecondEpisodeOfSameShow() {
        val running = listOf(item("e1", DownloadStatus.Downloading, show = "show-a", season = 1, episode = 1))
        assertFalse(policy(perShow = 1).canStart("show-a", running))
        assertTrue(policy(perShow = 2).canStart("show-a", running))
    }

    @Test
    fun showLimitBlocksNewShowWhenAtMax() {
        val running = listOf(item("e1", DownloadStatus.Downloading, show = "show-a", season = 1, episode = 1))
        assertFalse(policy(shows = 1).canStart("show-b", running))
        assertTrue(policy(shows = 2).canStart("show-b", running))
    }

    @Test
    fun moviesHaveNoShowLimit() {
        assertNull(item("m").showKey)
        val running = listOf(item("e1", DownloadStatus.Downloading, show = "show-a", season = 1, episode = 1))
        assertTrue(policy(perShow = 1, shows = 1).canStart(null, running))
    }

    @Test
    fun nextToStartPicksOldestQueuedThatFits() {
        val items = listOf(
            item("late", created = 20L),
            item("early", created = 10L),
        )
        assertEquals("early", policy().nextToStart(items)?.id)
    }

    @Test
    fun nextToStartSkipsQueuedItemBlockedByShowLimit() {
        val items = listOf(
            item("busy", DownloadStatus.Downloading, show = "show-a", season = 1, episode = 1, created = 1L),
            item("same-show", show = "show-a", season = 1, episode = 2, created = 2L),
            item("other-show", show = "show-b", season = 1, episode = 1, created = 3L),
        )
        assertEquals("other-show", policy(perShow = 1, shows = 2).nextToStart(items)?.id)
    }

    @Test
    fun statusCountsGroupsByState() {
        val counts = listOf(
            item("a", DownloadStatus.Downloading),
            item("b", DownloadStatus.Queued),
            item("c", DownloadStatus.Failed),
            item("d", DownloadStatus.Completed),
        ).statusCounts()
        assertEquals(1, counts.downloading)
        assertEquals(1, counts.queued)
        assertEquals(1, counts.failed)
        assertTrue(counts.hasActive)
    }

    @Test
    fun concurrencyClampStaysInRange() {
        assertEquals(1, DownloadConcurrencyLimit.TotalDownloads.clamp(0))
        assertEquals(DownloadConcurrencyLimit.MAX_VALUE, DownloadConcurrencyLimit.TotalDownloads.clamp(99))
    }
}
