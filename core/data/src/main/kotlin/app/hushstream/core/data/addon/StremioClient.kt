package app.hushstream.core.data.addon

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Minimal client for the Stremio addon protocol (manifest, catalog, meta, stream).
 * Security: https only, bounded response size, JSON only. Addon responses are untrusted data.
 */
class StremioClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    fun manifest(manifestUrl: String): JSONObject = getJson(manifestUrl)

    fun catalog(base: String, type: String, id: String, search: String?): JSONObject {
        val path = if (search.isNullOrBlank()) {
            "catalog/$type/${encodeSegment(id)}.json"
        } else {
            "catalog/$type/${encodeSegment(id)}/search=${encodeSegment(search)}.json"
        }
        return getJson("$base/$path")
    }

    fun meta(base: String, type: String, id: String): JSONObject =
        getJson("$base/meta/$type/${encodeSegment(id)}.json")

    fun streams(base: String, type: String, id: String): JSONObject =
        getJson("$base/stream/$type/${encodeSegment(id)}.json")

    private fun getJson(url: String): JSONObject {
        require(url.startsWith("https://")) { "Addon URLs must use https" }
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Addon returned HTTP ${response.code}")
            val body = response.body ?: error("Addon returned an empty response")
            // Bounded read: never buffer more than MAX_BYTES, even when the length is unknown.
            return body.source().use { source ->
                if (source.request(MAX_BYTES + 1)) error("Addon response too large")
                JSONObject(source.readUtf8())
            }
        }
    }

    private fun encodeSegment(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20").replace("%3A", ":")

    private companion object {
        const val MAX_BYTES = 2_000_000L
    }
}
