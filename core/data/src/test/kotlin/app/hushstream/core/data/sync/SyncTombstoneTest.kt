package app.hushstream.core.data.sync

import app.hushstream.core.domain.model.MediaKind
import app.hushstream.core.domain.sync.LibraryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTombstoneTest {

    private fun row(id: String, updated: Long, deleted: Long? = null) = LibraryEntry(
        mediaId = id,
        kind = MediaKind.MOVIE,
        title = id,
        posterUrl = null,
        saved = deleted == null,
        updatedAtEpochMs = updated,
        deletedAtEpochMs = deleted,
    )

    private fun plan(local: List<LibraryEntry>, remote: List<LibraryEntry>) =
        SyncMerge.plan(local, remote, key = { it.mediaId }, updatedAtEpochMs = { it.updatedAtEpochMs })

    @Test
    fun newerRemoteTombstoneWinsAndIsAppliedForRemoval() {
        val p = plan(local = listOf(row("m1", updated = 5)), remote = listOf(row("m1", updated = 9, deleted = 9)))
        assertEquals(1, p.applyLocal.size)
        assertTrue(p.applyLocal.single().deletedAtEpochMs != null)
        assertTrue(p.pushRemote.isEmpty())
    }

    @Test
    fun newerLocalTombstonePushedToRemote() {
        val p = plan(local = listOf(row("m1", updated = 9, deleted = 9)), remote = listOf(row("m1", updated = 5)))
        assertEquals(1, p.pushRemote.size)
        assertTrue(p.pushRemote.single().deletedAtEpochMs != null)
        assertTrue(p.applyLocal.isEmpty())
    }

    @Test
    fun olderRemoteTombstoneDoesNotDeleteNewerLocalRow() {
        val p = plan(local = listOf(row("m1", updated = 9)), remote = listOf(row("m1", updated = 5, deleted = 5)))
        assertTrue(p.applyLocal.isEmpty())
        assertEquals(1, p.pushRemote.size)
        assertEquals(null, p.pushRemote.single().deletedAtEpochMs)
    }
}
