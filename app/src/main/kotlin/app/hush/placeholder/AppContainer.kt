package app.hush.placeholder

import android.content.Context
import app.hush.placeholder.core.data.DemoMediaRepository
import app.hush.placeholder.core.domain.repository.MediaRepository

/**
 * Manual dependency container (ADR, D-005: no DI framework).
 * The demo repository is the only data source in this vertical slice. It is clearly marked isDemo in the UI.
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val mediaRepository: MediaRepository = DemoMediaRepository()
}
