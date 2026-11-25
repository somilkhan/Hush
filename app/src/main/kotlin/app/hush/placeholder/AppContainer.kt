package app.hush.placeholder

import android.content.Context
import app.hush.placeholder.core.data.EmptyMediaRepository
import app.hush.placeholder.core.data.LocalLibraryRepository
import app.hush.placeholder.core.data.LocalWatchHistoryRepository
import app.hush.placeholder.core.data.addon.AddonMediaRepository
import app.hush.placeholder.core.data.addon.PrefsAddonRegistry
import app.hush.placeholder.core.data.addon.StremioClient
import app.hush.placeholder.core.data.remote.SupabaseAuthRepository
import app.hush.placeholder.core.data.remote.SupabaseConfig
import app.hush.placeholder.core.domain.account.AuthRepository
import app.hush.placeholder.core.domain.addon.AddonRegistry
import app.hush.placeholder.core.domain.repository.MediaRepository
import app.hush.placeholder.core.domain.sync.LibraryRepository
import app.hush.placeholder.core.domain.sync.WatchHistoryRepository

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

    val authRepository: AuthRepository = SupabaseAuthRepository(
        context = appContext,
        baseUrl = SupabaseConfig.url,
        publishableKey = SupabaseConfig.publishableKey,
    )

    val libraryRepository: LibraryRepository = LocalLibraryRepository(appContext)

    val historyRepository: WatchHistoryRepository = LocalWatchHistoryRepository(appContext)

    val guestStore: GuestStore = GuestStore(appContext)
}
