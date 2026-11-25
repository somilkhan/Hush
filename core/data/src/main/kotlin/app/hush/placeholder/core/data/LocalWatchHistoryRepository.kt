package app.hush.placeholder.core.data

import android.content.Context
import app.hush.placeholder.core.domain.sync.WatchEntry
import app.hush.placeholder.core.domain.sync.WatchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-local watch history, one row per (mediaId, episodeId). Capped to the most recent entries.
 * Stored as JSON in private preferences; Room and Supabase sync come later behind the same interface.
 */
class LocalWatchHistoryRepository(context: Context) : WatchHistoryRepository {

    private val prefs = context.getSharedPreferences("hush_history", Context.MODE_PRIVATE)
    private val lock = Mutex()

    override suspend fun all(): List<WatchEntry> = withContext(Dispatchers.IO) {
        lock.withLock { read() }
    }

    override suspend fun record(entry: WatchEntry) = withContext(Dispatchers.IO) {
        lock.withLock {
            val kept = read().filterNot { it.mediaId == entry.mediaId && it.episodeId == entry.episodeId }
            write((kept + entry).takeLast(MAX_ENTRIES))
        }
    }

    private fun read(): List<WatchEntry> {
        val text = prefs.getString("entries", null) ?: return emptyList()
        val array = JSONArray(text)
        return (0 until array.length()).map { i ->
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
        }
    }

    private fun write(entries: List<WatchEntry>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(
                JSONObject()
                    .put("mediaId", e.mediaId)
                    .put("episodeId", e.episodeId)
                    .put("positionMs", e.positionMs)
                    .put("durationMs", e.durationMs)
                    .put("completed", e.completed)
                    .put("updatedAtEpochMs", e.updatedAtEpochMs)
                    .put("title", e.title),
            )
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }

    private companion object {
        const val MAX_ENTRIES = 200
    }
}
