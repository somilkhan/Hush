package app.hushstream.core.data

import app.hushstream.core.domain.model.MediaKind
import app.hushstream.core.domain.sync.LibraryEntry
import app.hushstream.core.domain.sync.WatchEntry
import org.json.JSONArray

/**
 * Parsers for the pre-Room SharedPreferences JSON. Used once, to copy old data into Room.
 * Pure JVM code (no Android types), so it can be unit tested. A malformed row is skipped.
 */
internal fun parseLegacyLibrary(text: String): List<LibraryEntry> {
    val array = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
    return (0 until array.length()).mapNotNull { i ->
        runCatching {
            val o = array.getJSONObject(i)
            LibraryEntry(
                mediaId = o.getString("mediaId"),
                kind = MediaKind.valueOf(o.getString("kind")),
                title = o.getString("title"),
                posterUrl = o.optString("posterUrl").ifBlank { null },
                saved = o.optBoolean("saved", true),
                updatedAtEpochMs = o.optLong("updatedAtEpochMs"),
            )
        }.getOrNull()
    }
}

internal fun parseLegacyHistory(text: String): List<WatchEntry> {
    val array = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
    return (0 until array.length()).mapNotNull { i ->
        runCatching {
            val o = array.getJSONObject(i)
            WatchEntry(
                mediaId = o.getString("mediaId"),
                episodeId = o.optString("episodeId"),
                positionMs = o.optLong("positionMs"),
                durationMs = o.optLong("durationMs"),
                completed = o.optBoolean("completed"),
                updatedAtEpochMs = o.optLong("updatedAtEpochMs"),
                title = o.optString("title"),
            )
        }.getOrNull()
    }
}
