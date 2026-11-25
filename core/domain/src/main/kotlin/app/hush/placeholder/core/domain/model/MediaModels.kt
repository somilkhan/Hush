package app.hush.placeholder.core.domain.model

// Vertical-slice contract (fixed by the Agent 2 brief, consumed by Agent 3 UI).
// PROVISIONAL: DOMAIN_MODEL.md lists a richer shape (ANIME, OTHER, MediaRef, MediaSummary...).
// Reconciling those with this slice is a later milestone. Do not treat as the CloudStream mapping.

enum class MediaKind { MOVIE, SERIES }

/** A playable stream. [label] is user-facing, e.g. "Demo 720p". */
data class StreamSource(val url: String, val label: String)

/** One playable unit. For a MOVIE the list holds a single Episode with season 1, number 1. */
data class Episode(
    val id: String,
    val season: Int,
    val number: Int,
    val title: String,
    val streams: List<StreamSource>,
)

data class MediaItem(
    val id: String,
    val title: String,
    val synopsis: String,
    val year: Int,
    val kind: MediaKind,
    val episodes: List<Episode>,
    /** True for any item that comes from the built-in demo catalogue. Must be shown in the UI. */
    val isDemo: Boolean,
)
