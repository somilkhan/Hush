package app.hush.placeholder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.hush.placeholder.design.HushTheme
import app.hush.placeholder.ui.HushApp

// Composition root. Wires the container into the UI.
class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // HushTheme provides LocalHushColors and the Material scheme. Without it the
            // default (light) tokens are used, which caused the light login background bug.
            HushTheme {
                HushApp(
                    mediaRepository = container.mediaRepository,
                    authRepository = container.authRepository,
                    libraryRepository = container.libraryRepository,
                    historyRepository = container.historyRepository,
                    addonRegistry = container.addonRegistry,
                    guestStore = container.guestStore,
                )
            }
        }
    }
}
