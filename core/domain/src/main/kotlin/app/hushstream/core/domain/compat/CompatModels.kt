/*
 * HUSH schema for extension repository indexes. This is NOT CloudStream's format.
 * It is defined from HUSH requirements only (D-020, D-013). No CloudStream code is copied.
 * UNVERIFIED against real CloudStream repositories; see docs/compatibility/METADATA_PARSER.md.
 */
package app.hushstream.core.domain.compat

/** A parsed extension repository index. [url] is the https address the index was fetched from. */
data class CompatRepository(
    val name: String,
    val url: String,
    val extensions: List<ExtensionMetadata>,
)

/**
 * Metadata for one extension, as listed in a repository index.
 * Only metadata is modelled. Nothing here executes extension code.
 */
data class ExtensionMetadata(
    /** Stable identifier: 1 to 64 characters from [A-Za-z0-9_.-]. */
    val internalName: String,
    val name: String,
    /** Monotonic version. Newer versions replace older ones. */
    val version: Int,
    /** https URL of the extension's source package. Required. */
    val sourceUrl: String,
    /** Optional SHA-256 of the source package, 64 hex characters. Lets a loader verify the download. */
    val sha256: String? = null,
    /** Language codes such as "en". Empty when not declared. */
    val languages: List<String> = emptyList(),
    /** Content types such as "movie", "series" or "live". Unknown values are kept as declared. */
    val types: List<String> = emptyList(),
    /** Optional https icon URL. */
    val iconUrl: String? = null,
    /** Permissions the extension declares, as plain strings. Not enforced in this slice. */
    val permissions: List<String> = emptyList(),
    val description: String = "",
    val authors: List<String> = emptyList(),
)

/** Why a whole index could not be parsed. Per-entry problems are not failures; see [SkippedEntry]. */
enum class FailureReason {
    EMPTY_INPUT,
    INVALID_JSON,
    SCHEMA,
    INSECURE_URL,
}

/** An extension entry that was dropped, with its position in the `extensions` array and the reason. */
data class SkippedEntry(val index: Int, val reason: String)

sealed interface RepositoryParseResult {
    data class Success(
        val repository: CompatRepository,
        val skipped: List<SkippedEntry>,
    ) : RepositoryParseResult {
        val skippedCount: Int get() = skipped.size
    }

    data class Failure(
        val reason: FailureReason,
        val message: String,
    ) : RepositoryParseResult
}
