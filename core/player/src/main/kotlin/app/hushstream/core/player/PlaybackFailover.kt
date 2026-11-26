package app.hushstream.core.player

/**
 * Candidate stream URLs for one playback, in the order to try them.
 * Blank entries and duplicates are dropped. The result is never empty: if every entry is blank,
 * [primary] is returned so the player still reports a clear error instead of crashing.
 */
internal fun playbackCandidates(primary: String, fallbacks: List<String>): List<String> =
    (listOf(primary) + fallbacks)
        .filter { it.isNotBlank() }
        .distinct()
        .ifEmpty { listOf(primary) }

/**
 * Index of the candidate to try after a failure at [current], or null when none are left.
 * The result is always less than [size], so failover is bounded by the list length.
 */
internal fun nextFailoverIndex(current: Int, size: Int): Int? =
    (current + 1).takeIf { it in 0 until size }
