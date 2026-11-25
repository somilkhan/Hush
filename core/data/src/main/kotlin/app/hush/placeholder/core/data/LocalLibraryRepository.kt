package app.hush.placeholder.core.data

import android.content.Context
import app.hush.placeholder.core.domain.model.MediaKind
import app.hush.placeholder.core.domain.sync.LibraryEntry
import app.hush.placeholder.core.domain.sync.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-local library (saved titles). Stored as JSON in private preferences.
 * Temporary store: move to Room when watch history lands. Sync to Supabase is a later adapter.
 */
class LocalLibraryRepository(context: Context) : LibraryRepository {

    private val prefs = context.getSharedPreferences("hush_library", Context.MODE_PRIVATE)

    override suspend fun all(): List<LibraryEntry> = withContext(Dispatchers.IO) { read() }

    override suspend fun upsert(entry: LibraryEntry) = withContext(Dispatchers.IO) {
        val updated = read().filterNot { it.mediaId == entry.mediaId } + entry
        write(updated)
    }

    override suspend fun remove(mediaId: String) = withContext(Dispatchers.IO) {
        write(read().filterNot { it.mediaId == mediaId })
    }

    private fun read(): List<LibraryEntry> {
        val text = prefs.getString("items", null) ?: return emptyList()
        val array = JSONArray(text)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            LibraryEntry(
                mediaId = o.getString("mediaId"),
                kind = MediaKind.valueOf(o.getString("kind")),
                title = o.getString("title"),
                posterUrl = o.optString("posterUrl").ifBlank { null },
                saved = o.optBoolean("saved", true),
                updatedAtEpochMs = o.optLong("updatedAtEpochMs"),
            )
        }
    }

    private fun write(entries: List<LibraryEntry>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(
                JSONObject()
                    .put("mediaId", e.mediaId)
                    .put("kind", e.kind.name)
                    .put("title", e.title)
                    .put("posterUrl", e.posterUrl.orEmpty())
                    .put("saved", e.saved)
                    .put("updatedAtEpochMs", e.updatedAtEpochMs),
            )
        }
        prefs.edit().putString("items", array.toString()).apply()
    }
}
