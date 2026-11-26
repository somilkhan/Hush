package app.hushstream.core.domain.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStatusCountsTest {

    private fun item(id: String, status: DownloadStatus) = DownloadItem(
        id = id,
        contentType = "movie",
        parentMetaId = "movie-$id",
        videoId = id,
        title = id,
        sourceUrl = "https://example.com/$id.mp4",
        fileName = "$id.mp4",
        status = status,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
    )

    @Test
    fun emptyListCountsZeroAndHasNoActive() {
        val counts = emptyList<DownloadItem>().statusCounts()
        assertEquals(DownloadStatusCounts(0, 0, 0, 0), counts)
        assertFalse(counts.hasActive)
    }

    @Test
    fun countsEachStatusSeparately() {
        val counts = listOf(
            item("a", DownloadStatus.Downloading),
            item("b", DownloadStatus.Downloading),
            item("c", DownloadStatus.Queued),
            item("d", DownloadStatus.Paused),
            item("e", DownloadStatus.Failed),
            item("f", DownloadStatus.Failed),
            item("g", DownloadStatus.Completed),
        ).statusCounts()

        assertEquals(DownloadStatusCounts(downloading = 2, queued = 1, paused = 1, failed = 2), counts)
    }

    @Test
    fun completedOnlyHasNoActive() {
        val counts = listOf(item("a", DownloadStatus.Completed)).statusCounts()
        assertFalse(counts.hasActive)
    }

    @Test
    fun queuedOrDownloadingIsActive() {
        assertTrue(listOf(item("a", DownloadStatus.Queued)).statusCounts().hasActive)
        assertTrue(listOf(item("a", DownloadStatus.Downloading)).statusCounts().hasActive)
        assertFalse(listOf(item("a", DownloadStatus.Paused)).statusCounts().hasActive)
    }
}
