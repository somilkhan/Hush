package app.hush.placeholder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.hush.placeholder.ui.HushApp

// Composition root. Wires the container into the UI.
class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HushApp(
                mediaRepository = container.mediaRepository,
                authRepository = container.authRepository,
                guestStore = container.guestStore,
            )
        }
    }
}
