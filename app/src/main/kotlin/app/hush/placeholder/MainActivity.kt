package app.hush.placeholder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.hush.placeholder.ui.HushApp

// Composition root. The UI (HushApp, theme, navigation) is owned by Agent 3 in ui/.
// HushApp(mediaRepository: MediaRepository) is the agreed call. UNVERIFIED until ui/HushApp.kt exists.
class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HushApp(mediaRepository = container.mediaRepository)
        }
    }
}
