package app.hush.placeholder.ui.library

import androidx.compose.runtime.Composable
import app.hush.placeholder.ui.common.StateMessage

/** Placeholder only. Library, history and favourites are not implemented in this slice. */
@Composable
fun LibraryScreen() {
    StateMessage(
        title = "Library is not built yet",
        body = "Saved titles and history arrive in a later milestone. Nothing is shown here.",
    )
}
