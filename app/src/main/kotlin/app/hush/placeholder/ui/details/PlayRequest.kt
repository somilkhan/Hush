package app.hush.placeholder.ui.details

/** What the player needs to play one episode and report its progress back to history. */
data class PlayRequest(
    val url: String,
    val mediaId: String,
    val episodeId: String,
    val title: String,
)
