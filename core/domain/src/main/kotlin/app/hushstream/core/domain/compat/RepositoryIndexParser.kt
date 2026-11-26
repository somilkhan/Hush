/*
 * HUSH schema parser. Pure JVM: depends only on kotlinx-serialization-json's tree API.
 * It never fetches URLs and never loads extension code.
 * UNVERIFIED against real CloudStream repositories.
 */
package app.hushstream.core.domain.compat

import java.net.URI
import java.net.URISyntaxException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Parses a HUSH repository index:
 * `{ "name": "...", "extensions": [ { "internalName": "...", "name": "...", "version": 1, "sourceUrl": "https://..." } ] }`
 *
 * Whole-index problems (empty input, bad JSON, wrong shape, non-https repository URL) return
 * [RepositoryParseResult.Failure]. A malformed extension entry is skipped and recorded in
 * [RepositoryParseResult.Success.skipped].
 */
object RepositoryIndexParser {

    private val internalNamePattern = Regex("^[A-Za-z0-9_.-]{1,64}$")
    private val sha256Pattern = Regex("^[0-9a-fA-F]{64}$")

    fun parse(text: String, repositoryUrl: String): RepositoryParseResult {
        if (text.isBlank()) {
            return RepositoryParseResult.Failure(FailureReason.EMPTY_INPUT, "Index is empty")
        }
        if (!isHttpsUrl(repositoryUrl)) {
            return RepositoryParseResult.Failure(FailureReason.INSECURE_URL, "Repository URL must be https")
        }

        val root = try {
            kotlinx.serialization.json.Json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            return RepositoryParseResult.Failure(FailureReason.INVALID_JSON, "Index is not valid JSON")
        }
        val rootObject = root as? JsonObject
            ?: return RepositoryParseResult.Failure(FailureReason.SCHEMA, "Index must be a JSON object")

        val name = (rootObject["name"] as? JsonPrimitive)
            ?.takeIf { it !is JsonNull && it.isString && it.content.isNotBlank() }?.content
            ?: return RepositoryParseResult.Failure(FailureReason.SCHEMA, "Index needs a non-blank \"name\" string")
        val entries = rootObject["extensions"] as? JsonArray
            ?: return RepositoryParseResult.Failure(FailureReason.SCHEMA, "Index needs an \"extensions\" array")

        val extensions = mutableListOf<ExtensionMetadata>()
        val skipped = mutableListOf<SkippedEntry>()
        entries.forEachIndexed { index, element ->
            try {
                extensions += parseEntry(element)
            } catch (e: MalformedEntryException) {
                skipped += SkippedEntry(index, e.message ?: "malformed entry")
            }
        }

        return RepositoryParseResult.Success(
            repository = CompatRepository(name = name, url = repositoryUrl, extensions = extensions),
            skipped = skipped,
        )
    }

    private fun parseEntry(element: JsonElement): ExtensionMetadata {
        val o = element as? JsonObject ?: throw MalformedEntryException("Entry is not an object")

        val internalName = requiredString(o, "internalName")
        if (!internalNamePattern.matches(internalName)) {
            throw MalformedEntryException("internalName must be 1-64 characters from [A-Za-z0-9_.-]")
        }
        val sourceUrl = requiredString(o, "sourceUrl")
        if (!isHttpsUrl(sourceUrl)) throw MalformedEntryException("sourceUrl must be https")

        val sha256 = optionalString(o, "sha256")?.also {
            if (!sha256Pattern.matches(it)) throw MalformedEntryException("sha256 must be 64 hex characters")
        }
        val iconUrl = optionalString(o, "iconUrl")?.also {
            if (!isHttpsUrl(it)) throw MalformedEntryException("iconUrl must be https")
        }

        return ExtensionMetadata(
            internalName = internalName,
            name = requiredString(o, "name"),
            version = requiredNonNegativeInt(o, "version"),
            sourceUrl = sourceUrl,
            sha256 = sha256,
            languages = optionalStringList(o, "languages"),
            types = optionalStringList(o, "types"),
            iconUrl = iconUrl,
            permissions = optionalStringList(o, "permissions"),
            description = optionalString(o, "description") ?: "",
            authors = optionalStringList(o, "authors"),
        )
    }

    private fun isHttpsUrl(url: String): Boolean {
        if (url.any { it.isWhitespace() }) return false
        val uri = try {
            URI(url)
        } catch (e: URISyntaxException) {
            return false
        }
        return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }

    private fun requiredString(o: JsonObject, key: String): String =
        optionalString(o, key)?.takeIf { it.isNotBlank() }
            ?: throw MalformedEntryException("Missing or blank \"$key\" string")

    private fun optionalString(o: JsonObject, key: String): String? {
        val value = o[key] ?: return null
        if (value is JsonNull) return null
        val primitive = value as? JsonPrimitive
        if (primitive == null || !primitive.isString) throw MalformedEntryException("\"$key\" must be a string")
        return primitive.content
    }

    private fun optionalStringList(o: JsonObject, key: String): List<String> {
        val value = o[key] ?: return emptyList()
        if (value is JsonNull) return emptyList()
        val array = value as? JsonArray ?: throw MalformedEntryException("\"$key\" must be an array")
        return array.map { item ->
            val primitive = item as? JsonPrimitive
            if (primitive == null || !primitive.isString || primitive.content.isBlank()) {
                throw MalformedEntryException("\"$key\" must contain only non-blank strings")
            }
            primitive.content
        }
    }

    private fun requiredNonNegativeInt(o: JsonObject, key: String): Int {
        val primitive = o[key] as? JsonPrimitive
        if (primitive == null || primitive.isString) throw MalformedEntryException("\"$key\" must be an integer")
        return primitive.content.toIntOrNull()?.takeIf { it >= 0 }
            ?: throw MalformedEntryException("\"$key\" must be a non-negative integer")
    }

    private class MalformedEntryException(message: String) : IllegalArgumentException(message)
}
