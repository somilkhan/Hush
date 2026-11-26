package app.hushstream.core.data.sync

import app.hushstream.core.data.remote.AuthorizedSession
import app.hushstream.core.domain.sync.LibraryEntry
import app.hushstream.core.domain.sync.LibraryRepository
import app.hushstream.core.domain.sync.WatchEntry
import app.hushstream.core.domain.sync.WatchHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncOutcome {
    /** No session. Guests make no network calls. */
    data object SignedOut : SyncOutcome
    data class Done(val pushed: Int, val applied: Int) : SyncOutcome
    /** [reason] is an exception class name only. Messages and tokens are never surfaced. */
    data class Failed(val reason: String) : SyncOutcome
}

/**
 * Local-first sync. The local stores stay the source the UI reads.
 *
 * Per table: fetch the remote snapshot, merge with the local snapshot by the newer-updatedAt rule,
 * push the local winners, then write the remote winners into the local store. Fetching before
 * pushing is what keeps an old device from overwriting newer remote rows.
 *
 * Deletions are not synced yet: the remote has no tombstones, so a row removed locally returns on the next pull.
 */
class SyncEngine(
    private val sessionProvider: suspend () -> AuthorizedSession?,
    private val localLibrary: LibraryRepository,
    private val localHistory: WatchHistoryRepository,
    private val remoteLibrary: RemoteStore<LibraryEntry>,
    private val remoteHistory: RemoteStore<WatchEntry>,
) {
    private val mutex = Mutex()

    suspend fun syncNow(): SyncOutcome = mutex.withLock {
        try {
            val session = sessionProvider() ?: return@withLock SyncOutcome.SignedOut
            val library = syncLibrary(session)
            val history = syncHistory(session)
            SyncOutcome.Done(
                pushed = library.first + history.first,
                applied = library.second + history.second,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncOutcome.Failed(e.javaClass.simpleName)
        }
    }

    /** Returns (pushed, applied). */
    private suspend fun syncLibrary(session: AuthorizedSession): Pair<Int, Int> {
        val local = localLibrary.all()
        val remote = remoteLibrary.fetchAll(session)
        val plan = SyncMerge.plan(local, remote, key = { it.mediaId }, updatedAtEpochMs = { it.updatedAtEpochMs })
        if (plan.pushRemote.isNotEmpty()) remoteLibrary.upsertAll(session, plan.pushRemote)
        plan.applyLocal.forEach { localLibrary.upsert(it) }
        return plan.pushRemote.size to plan.applyLocal.size
    }

    /** Returns (pushed, applied). */
    private suspend fun syncHistory(session: AuthorizedSession): Pair<Int, Int> {
        val local = localHistory.all()
        val remote = remoteHistory.fetchAll(session)
        val plan = SyncMerge.plan(
            local,
            remote,
            key = { it.mediaId to it.episodeId },
            updatedAtEpochMs = { it.updatedAtEpochMs },
        )
        if (plan.pushRemote.isNotEmpty()) remoteHistory.upsertAll(session, plan.pushRemote)
        plan.applyLocal.forEach { localHistory.record(it) }
        return plan.pushRemote.size to plan.applyLocal.size
    }
}
