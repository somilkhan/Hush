package app.hushstream.core.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The single trigger point for sync. The app calls [onSignedIn] once after sign-in (and may call it
 * on foreground later). Guests never reach the network: [SyncEngine.syncNow] returns SignedOut for them.
 * Not wired into app/ yet; the call site belongs to the app owner (see docs/SYNC_STATUS.md).
 */
class SyncCoordinator(private val engine: SyncEngine) {

    /** Starts one sync in [scope]. Overlapping calls are serialised by the engine's mutex. */
    fun onSignedIn(scope: CoroutineScope, onOutcome: (SyncOutcome) -> Unit = {}) {
        scope.launch { onOutcome(engine.syncNow()) }
    }
}
