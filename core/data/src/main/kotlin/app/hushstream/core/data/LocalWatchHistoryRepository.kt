package app.hushstream.core.data

import android.content.Context
import androidx.room.withTransaction
import app.hushstream.core.data.db.HushDatabase
import app.hushstream.core.data.db.HistoryRow
import app.hushstream.core.domain.sync.WatchEntry
import app.hushstream.core.domain.sync.WatchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Device-local watch history in Room, one row per (mediaId, episodeId), capped to the newest 200.
 * Old SharedPreferences JSON is copied in once on first use, then its key is removed.
 */
class LocalWatchHistoryRepository(context: Context) : WatchHistoryRepository {

    private val db = HushDatabase.get(context)
    private val dao = db.historyDao()
    private val legacy = context.getSharedPreferences("hush_history", Context.MODE_PRIVATE)
    private val lock = Mutex()
    private var imported = false

    override suspend fun all(): List<WatchEntry> = withContext(Dispatchers.IO) {
        lock.withLock { ensureImported(); dao.all().map { it.toEntry() } }
    }

    override suspend fun record(entry: WatchEntry) = withContext(Dispatchers.IO) {
        lock.withLock {
            ensureImported()
            dao.upsert(entry.toRow())
            dao.trim(MAX_ENTRIES)
        }
    }

    private suspend fun ensureImported() {
        if (imported) return
        val text = legacy.getString("entries", null)
        if (text != null) {
            val rows = parseLegacyHistory(text).map { it.toRow() }
            db.withTransaction { rows.forEach { dao.upsert(it) } }
            dao.trim(MAX_ENTRIES)
            legacy.edit().remove("entries").apply()
        }
        imported = true
    }


    private fun WatchEntry.toRow() = HistoryRow(mediaId, episodeId, positionMs, durationMs, completed, updatedAtEpochMs, title)

    private fun HistoryRow.toEntry() = WatchEntry(
        mediaId = mediaId,
        episodeId = episodeId,
        positionMs = positionMs,
        durationMs = durationMs,
        completed = completed,
        updatedAtEpochMs = updatedAtEpochMs,
        title = title,
    )

    private companion object {
        const val MAX_ENTRIES = 200
    }
}
