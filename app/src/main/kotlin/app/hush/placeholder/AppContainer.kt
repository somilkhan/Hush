package app.hush.placeholder

import android.content.Context
import app.hush.placeholder.core.data.DemoMediaRepository
import app.hush.placeholder.core.data.remote.SupabaseAuthRepository
import app.hush.placeholder.core.data.remote.SupabaseConfig
import app.hush.placeholder.core.domain.account.AuthRepository
import app.hush.placeholder.core.domain.repository.MediaRepository

/**
 * Manual dependency container (D-005: no DI framework).
 * Catalogue is still the demo repository. Auth uses Supabase when configured, otherwise reports "not configured".
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val mediaRepository: MediaRepository = DemoMediaRepository()

    val authRepository: AuthRepository = SupabaseAuthRepository(
        context = appContext,
        baseUrl = SupabaseConfig.url,
        publishableKey = SupabaseConfig.publishableKey,
    )

    val guestStore: GuestStore = GuestStore(appContext)
}
