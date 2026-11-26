package app.hushstream.core.domain.addon

/** A user-added Stremio-protocol addon. [manifestUrl] always ends with /manifest.json and uses https. */
data class InstalledAddon(
    val manifestUrl: String,
    val name: String,
    val description: String,
) {
    val base: String get() = manifestUrl.removeSuffix("/manifest.json")
}

interface AddonRegistry {
    suspend fun all(): List<InstalledAddon>
    suspend fun add(manifestUrl: String): Result<InstalledAddon>
    suspend fun remove(manifestUrl: String)
}
