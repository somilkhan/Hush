package app.hushstream.core.data

import app.hushstream.core.domain.model.MediaKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// NOT RUN: written without a JDK or Gradle in this environment. CI (:core:data:test) is the first run.
class DemoMediaRepositoryTest {

    private val repository = DemoMediaRepository()

    @Test
    fun searchMatchesTitleCaseInsensitively() = runBlocking {
        val results = repository.search("LANTERN")
        assertEquals(listOf("demo-movie-lantern"), results.map { it.id })
    }

    @Test
    fun searchReturnsEmptyForNoMatch() = runBlocking {
        assertTrue(repository.search("no such title zzz").isEmpty())
    }

    @Test
    fun searchDoesNotMatchSynopsis() = runBlocking {
        // "lighthouse" appears only in a synopsis, so title-only filtering must exclude it.
        assertTrue(repository.search("lighthouse").isEmpty())
    }

    @Test
    fun detailsFindsKnownId() = runBlocking {
        val item = repository.details("demo-series-glasswater")
        assertEquals("Glasswater", item?.title)
    }

    @Test
    fun detailsReturnsNullForUnknownId() = runBlocking {
        assertNull(repository.details("missing"))
    }

    @Test
    fun catalogueHasFourDemoItemsWithExpectedShape() = runBlocking {
        val all = repository.featured()
        assertEquals(4, all.size)
        assertTrue(all.all { it.isDemo })
        assertEquals(2, all.count { it.kind == MediaKind.MOVIE })
        assertTrue(all.filter { it.kind == MediaKind.MOVIE }
            .all { it.episodes.size == 1 && it.episodes[0].season == 1 && it.episodes[0].number == 1 })
        assertTrue(all.all { item -> item.episodes.all { ep -> ep.streams.isNotEmpty() } })
    }
}
