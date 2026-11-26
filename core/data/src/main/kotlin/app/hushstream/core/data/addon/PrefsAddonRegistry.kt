package app.hushstream.core.data.addon

import android.content.Context
import app.hushstream.core.domain.addon.AddonRegistry
import app.hushstream.core.domain.addon.InstalledAddon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Device-local list of installed addons. Each add is validated by fetching its manifest first. */
class PrefsAddonRegistry(
    context: Context,
    private val client: StremioClient,
) : AddonRegistry {

    private val prefs = context.getSharedPreferences("hush_addons", Context.MODE_PRIVATE)

    override suspend fun all(): List<InstalledAddon> = withContext(Dispatchers.IO) { read() }

    override suspend fun add(manifestUrl: String): Result<InstalledAddon> = withContext(Dispatchers.IO) {
        runCatching {
            val url = normalise(manifestUrl)
            val manifest = client.manifest(url)
            val addon = InstalledAddon(
                manifestUrl = url,
                name = manifest.optString("name").ifBlank { "Unnamed addon" },
                description = manifest.optString("description"),
            )
            write(read().filterNot { it.manifestUrl == url } + addon)
            addon
        }
    }

    override suspend fun remove(manifestUrl: String) = withContext(Dispatchers.IO) {
        write(read().filterNot { it.manifestUrl == manifestUrl })
    }

    private fun read(): List<InstalledAddon> {
        val text = prefs.getString("list", null) ?: return emptyList()
        val array = JSONArray(text)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            InstalledAddon(
                manifestUrl = o.getString("url"),
                name = o.optString("name"),
                description = o.optString("description"),
            )
        }
    }

    private fun write(list: List<InstalledAddon>) {
        val array = JSONArray()
        list.forEach { a ->
            array.put(
                JSONObject()
                    .put("url", a.manifestUrl)
                    .put("name", a.name)
                    .put("description", a.description),
            )
        }
        prefs.edit().putString("list", array.toString()).apply()
    }

    companion object {
        /** Accepts a base link or a full manifest link; always returns https://.../manifest.json. */
        fun normalise(input: String): String {
            val trimmed = input.trim().trimEnd('/')
            require(trimmed.startsWith("https://")) { "Addon link must start with https://" }
            return if (trimmed.endsWith("/manifest.json")) trimmed else "$trimmed/manifest.json"
        }
    }
}
