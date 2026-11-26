package app.hushstream.core.data

import app.hushstream.core.domain.model.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyPrefsParserTest {

    @Test
    fun libraryRowsParseAndMalformedRowIsSkipped() {
        val json = """
            [
              {"mediaId":"m1","kind":"MOVIE","title":"One","posterUrl":"","saved":true,"updatedAtEpochMs":5},
              {"mediaId":"bad","kind":"NOT_A_KIND","title":"X"},
              {"mediaId":"m2","kind":"MOVIE","title":"Two","posterUrl":"https://e/p.jpg","saved":false,"updatedAtEpochMs":7}
            ]
        """.trimIndent()
        val rows = parseLegacyLibrary(json)
        assertEquals(listOf("m1", "m2"), rows.map { it.mediaId })
        assertNull(rows[0].posterUrl)
        assertEquals("https://e/p.jpg", rows[1].posterUrl)
        assertEquals(false, rows[1].saved)
        assertEquals(MediaKind.MOVIE, rows[0].kind)
    }

    @Test
    fun historyRowsKeepEpisodeAndDefaults() {
        val json = """[{"mediaId":"s1","episodeId":"e3","positionMs":1200,"durationMs":3600,"updatedAtEpochMs":9,"title":"Show"}]"""
        val rows = parseLegacyHistory(json)
        assertEquals(1, rows.size)
        assertEquals("e3", rows[0].episodeId)
        assertEquals(1200L, rows[0].positionMs)
        assertEquals(false, rows[0].completed)
    }

    @Test
    fun garbageInputGivesEmptyList() {
        assertEquals(emptyList<Any>(), parseLegacyLibrary("not json"))
        assertEquals(emptyList<Any>(), parseLegacyHistory(""))
    }
}
