package app.hush.placeholder.core.domain.model

// PROVISIONAL. Shapes are the app's own view (see docs/architecture/DOMAIN_MODEL.md).
// They are not CloudStream types and must be mapped by the compatibility adapter (Agent 1).

/** Stable identifier of a provider, e.g. an installed extension. Opaque to the UI. */
@JvmInline
value class ProviderId(val value: String) {
    init { require(value.isNotBlank()) { "ProviderId must not be blank" } }
}

/** Provider-scoped media reference. The pair is the only identity used across layers. */
data class MediaRef(val providerId: ProviderId, val key: String) {
    init { require(key.isNotBlank()) { "MediaRef key must not be blank" } }
}

/** Reference to one episode within a media item. */
data class EpisodeRef(val media: MediaRef, val season: Int?, val episode: Int)
