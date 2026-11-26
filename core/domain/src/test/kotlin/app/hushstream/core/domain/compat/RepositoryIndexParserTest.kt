package app.hushstream.core.domain.compat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryIndexParserTest {

    private val repoUrl = "https://example.org/index.json"

    private fun parseOk(text: String): RepositoryParseResult.Success {
        val result = RepositoryIndexParser.parse(text, repoUrl)
        assertTrue("expected Success but was $result", result is RepositoryParseResult.Success)
        return result as RepositoryParseResult.Success
    }

    @Test
    fun validIndexParsesAllFieldsAndDefaults() {
        val json = """
            {
              "name": "Sample repo",
              "extensions": [
                {
                  "internalName": "sample.movies",
                  "name": "Sample Movies",
                  "version": 3,
                  "sourceUrl": "https://example.org/sample-movies.pkg",
                  "sha256": "${"a".repeat(64)}",
                  "languages": ["en", "de"],
                  "types": ["movie", "series"],
                  "iconUrl": "https://example.org/icon.png",
                  "permissions": ["network"],
                  "description": "Movies",
                  "authors": ["someone"]
                },
                {
                  "internalName": "live_tv-1",
                  "name": "Live",
                  "version": 0,
                  "sourceUrl": "https://example.org/live.pkg"
                }
              ]
            }
        """.trimIndent()

        val success = parseOk(json)

        assertEquals("Sample repo", success.repository.name)
        assertEquals(repoUrl, success.repository.url)
        assertEquals(0, success.skippedCount)
        assertEquals(2, success.repository.extensions.size)

        val first = success.repository.extensions[0]
        assertEquals("sample.movies", first.internalName)
        assertEquals(3, first.version)
        assertEquals(listOf("en", "de"), first.languages)
        assertEquals(listOf("movie", "series"), first.types)
        assertEquals("https://example.org/icon.png", first.iconUrl)
        assertEquals(listOf("network"), first.permissions)
        assertEquals("a".repeat(64), first.sha256)

        val second = success.repository.extensions[1]
        assertNull(second.iconUrl)
        assertNull(second.sha256)
        assertEquals(emptyList<String>(), second.languages)
        assertEquals(emptyList<String>(), second.types)
        assertEquals(emptyList<String>(), second.permissions)
        assertEquals("", second.description)
        assertEquals(0, second.version)
    }

    @Test
    fun malformedEntriesAreSkippedAndCounted() {
        val json = """
            {
              "name": "Mixed",
              "extensions": [
                {"internalName": "good", "name": "Good", "version": 1, "sourceUrl": "https://example.org/a.pkg"},
                "not an object",
                {"internalName": "no-name", "version": 1, "sourceUrl": "https://example.org/b.pkg"},
                {"internalName": "http-source", "name": "H", "version": 1, "sourceUrl": "http://example.org/c.pkg"},
                {"internalName": "bad sha", "name": "S", "version": 1, "sourceUrl": "https://example.org/d.pkg", "sha256": "xyz"},
                {"internalName": "bad version", "name": "V", "version": "1", "sourceUrl": "https://example.org/e.pkg"},
                {"internalName": "neg version", "name": "V", "version": -1, "sourceUrl": "https://example.org/e.pkg"},
                {"internalName": "has/slash", "name": "P", "version": 1, "sourceUrl": "https://example.org/f.pkg"},
                {"internalName": "http-icon", "name": "I", "version": 1, "sourceUrl": "https://example.org/g.pkg", "iconUrl": "http://example.org/i.png"},
                {"internalName": "bad-langs", "name": "L", "version": 1, "sourceUrl": "https://example.org/h.pkg", "languages": "en"}
              ]
            }
        """.trimIndent()

        val success = parseOk(json)

        assertEquals(1, success.repository.extensions.size)
        assertEquals("good", success.repository.extensions[0].internalName)
        assertEquals(9, success.skippedCount)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), success.skipped.map { it.index })
        assertTrue(success.skipped.all { it.reason.isNotBlank() })
    }

    @Test
    fun emptyInputIsFailure() {
        for (text in listOf("", "   \n")) {
            val result = RepositoryIndexParser.parse(text, repoUrl)
            assertEquals(FailureReason.EMPTY_INPUT, (result as RepositoryParseResult.Failure).reason)
        }
    }

    @Test
    fun emptyExtensionListIsSuccessWithNoEntries() {
        val success = parseOk("""{"name": "Empty", "extensions": []}""")
        assertEquals(0, success.repository.extensions.size)
        assertEquals(0, success.skippedCount)
    }

    @Test
    fun nonHttpsRepositoryUrlIsRejected() {
        val json = """{"name": "R", "extensions": []}"""
        for (url in listOf("http://example.org/index.json", "ftp://example.org/i.json", "example.org/index.json")) {
            val result = RepositoryIndexParser.parse(json, url)
            assertEquals(FailureReason.INSECURE_URL, (result as RepositoryParseResult.Failure).reason)
        }
    }

    @Test
    fun invalidJsonIsFailure() {
        val result = RepositoryIndexParser.parse("{not json", repoUrl)
        assertEquals(FailureReason.INVALID_JSON, (result as RepositoryParseResult.Failure).reason)
    }

    @Test
    fun wrongShapeIsSchemaFailure() {
        val cases = listOf(
            "[]",
            """{"extensions": []}""",
            """{"name": "R"}""",
            """{"name": "R", "extensions": {}}""",
            """{"name": "  ", "extensions": []}""",
        )
        for (text in cases) {
            val result = RepositoryIndexParser.parse(text, repoUrl)
            assertEquals("for $text", FailureReason.SCHEMA, (result as RepositoryParseResult.Failure).reason)
        }
    }
}
