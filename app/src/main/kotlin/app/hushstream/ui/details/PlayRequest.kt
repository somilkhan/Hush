package app.hushstream.ui.details

/** What the player needs to play one episode and report its progress back to history. */
data class PlayRequest(
    val url: String,
    val mediaId: String,
    val episodeId: String,
    val title: String,
    /** Where to start playback, in ms. Used when resuming from Continue watching. */
    val startMs: Long = 0L,
)
