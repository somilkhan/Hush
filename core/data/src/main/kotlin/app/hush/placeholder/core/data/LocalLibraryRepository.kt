package app.hush.placeholder.core.data

import android.content.Context
import app.hush.placeholder.core.domain.model.MediaKind
import app.hush.placeholder.core.domain.sync.LibraryEntry
import app.hush.placeholder.core.domain.sync.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-local library (saved titles). JSON in private preferences, mutex-guarded writes.
 * A malformed row is skipped instead of crashing every later read. Sync comes later behind the same interface.
 */
class LocalLibraryRepository(context: Context) : LibraryRepository {

    private val prefs = context.getSharedPreferences("hush_library", Context.MODE_PRIVATE)
    private val lock = Mutex()

    override suspend fun all(): List<LibraryEntry> = withContext(Dispatchers.IO) {
        lock.withLock { read() }
    }

    override suspend fun upsert(entry: LibraryEntry) = withContext(Dispatchers.IO) {
        lock.withLock { write(read().filterNot { it.mediaId == entry.mediaId } + entry) }
    }

    override suspend fun remove(mediaId: String) = withContext(Dispatchers.IO) {
        lock.withLock { write(read().filterNot { it.mediaId == mediaId }) }
    }

    private fun read(): List<LibraryEntry> {
        val text = prefs.getString("items", null) ?: return emptyList()
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
