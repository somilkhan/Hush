package app.hushstream

import android.content.Context
import app.hushstream.core.data.EmptyMediaRepository
import app.hushstream.core.data.LocalLibraryRepository
import app.hushstream.core.data.LocalWatchHistoryRepository
import app.hushstream.core.data.addon.AddonMediaRepository
import app.hushstream.core.data.addon.PrefsAddonRegistry
import app.hushstream.core.data.addon.StremioClient
import app.hushstream.core.data.remote.RemoteLibraryRepository
import app.hushstream.core.data.remote.SupabaseAuthRepository
import app.hushstream.core.data.remote.SupabaseConfig
import app.hushstream.core.domain.account.AuthRepository
import app.hushstream.core.domain.addon.AddonRegistry
import app.hushstream.core.domain.repository.MediaRepository
import app.hushstream.core.domain.sync.LibraryRepository
import app.hushstream.core.domain.sync.WatchHistoryRepository
import app.hushstream.core.data.sync.SyncCoordinator
import app.hushstream.core.data.sync.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container (D-005: no DI framework).
 * Catalogue comes from user-added addons; demo data is shown only while none is installed.
 * Library is device-local for now; Supabase sync is a later adapter behind the same interface.
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    private val stremioClient = StremioClient()

    val addonRegistry: AddonRegistry = PrefsAddonRegistry(appContext, stremioClient)

    val mediaRepository: MediaRepository = AddonMediaRepository(
        registry = addonRegistry,
        client = stremioClient,
        fallback = EmptyMediaRepository(),
    )

    private val supabaseAuth = SupabaseAuthRepository(
        context = appContext,
        baseUrl = SupabaseConfig.url,
        publishableKey = SupabaseConfig.publishableKey,
    )

    val authRepository: AuthRepository = supabaseAuth

    val libraryRepository: LibraryRepository = LocalLibraryRepository(appContext)

    val historyRepository: WatchHistoryRepository = LocalWatchHistoryRepository(appContext)

    val guestStore: GuestStore = GuestStore(appContext)

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Library sync only. History sync stays off until the watch_history remote migration is approved. */
    private val syncCoordinator = SyncCoordinator(
        SyncEngine(
            sessionProvider = { supabaseAuth.authorizedSession() },
            localLibrary = libraryRepository,
            localHistory = historyRepository,
            remoteLibrary = RemoteLibraryRepository(SupabaseConfig.url, SupabaseConfig.publishableKey),
            remoteHistory = null,
        ),
    )

    /** Called once a session exists (app start with a session, or after sign-in). Guests make no network calls. */
    fun startSync() = syncCoordinator.onSignedIn(syncScope)
}
