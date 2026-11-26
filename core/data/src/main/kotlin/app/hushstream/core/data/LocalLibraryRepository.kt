package app.hushstream.core.data

import android.content.Context
import androidx.room.withTransaction
import app.hushstream.core.data.db.HushDatabase
import app.hushstream.core.data.db.LibraryRow
import app.hushstream.core.domain.model.MediaKind
import app.hushstream.core.domain.sync.LibraryEntry
import app.hushstream.core.domain.sync.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Device-local library (saved titles), stored in Room.
 * On first use, rows from the old SharedPreferences JSON are copied in once. The old key is removed only after that copy succeeds.
 */
class LocalLibraryRepository(context: Context) : LibraryRepository {

    private val db = HushDatabase.get(context)
    private val dao = db.libraryDao()
    private val legacy = context.getSharedPreferences("hush_library", Context.MODE_PRIVATE)
    private val lock = Mutex()
    private var imported = false

    override suspend fun all(): List<LibraryEntry> = withContext(Dispatchers.IO) {
        lock.withLock { ensureImported(); dao.all().map { it.toEntry() } }
    }

    override suspend fun upsert(entry: LibraryEntry) = withContext(Dispatchers.IO) {
        lock.withLock { ensureImported(); dao.upsert(entry.toRow()) }
    }

    override suspend fun remove(mediaId: String) = withContext(Dispatchers.IO) {
        lock.withLock { ensureImported(); dao.remove(mediaId) }
    }

    private suspend fun ensureImported() {
        if (imported) return
        val text = legacy.getString("items", null)
        if (text != null) {
            val rows = parseLegacyLibrary(text).map { it.toRow() }
            db.withTransaction { rows.forEach { dao.upsert(it) } }
            legacy.edit().remove("items").apply()
        }
        imported = true
    }


    private fun LibraryEntry.toRow() = LibraryRow(mediaId, kind.name, title, posterUrl, saved, updatedAtEpochMs)

    private fun LibraryRow.toEntry() = LibraryEntry(
        mediaId = mediaId,
        kind = MediaKind.valueOf(kind),
        title = title,
        posterUrl = posterUrl,
        saved = saved,
        updatedAtEpochMs = updatedAtEpochMs,
    )
}
