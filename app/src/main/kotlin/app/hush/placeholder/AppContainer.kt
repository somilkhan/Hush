package app.hush.placeholder

import android.content.Context
import app.hush.placeholder.core.data.DemoMediaRepository
import app.hush.placeholder.core.data.LocalLibraryRepository
import app.hush.placeholder.core.data.remote.SupabaseAuthRepository
import app.hush.placeholder.core.data.remote.SupabaseConfig
import app.hush.placeholder.core.domain.account.AuthRepository
import app.hush.placeholder.core.domain.repository.MediaRepository
import app.hush.placeholder.core.domain.sync.LibraryRepository

/**
 * Manual dependency container (D-005: no DI framework).
 * Catalogue is still the demo repository. Auth uses Supabase when configured, otherwise reports "not configured".
 * Library is device-local for now; Supabase sync is a later adapter behind the same interface.
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val mediaRepository: MediaRepository = DemoMediaRepository()

    val authRepository: AuthRepository = SupabaseAuthRepository(
        context = appContext,
        baseUrl = SupabaseConfig.url,
        publishableKey = SupabaseConfig.publishableKey,
    )

    val libraryRepository: LibraryRepository = LocalLibraryRepository(appContext)

    val guestStore: GuestStore = GuestStore(appContext)
}
