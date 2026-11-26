package app.hushstream.core.data.addon

import android.util.Base64

/**
 * Addon-backed item id: base URL + Stremio type + meta id, packed into one URL-safe string.
 * Demo ids never decode to a key, so the repository can tell the two apart.
 */
internal data class ItemKey(val base: String, val type: String, val metaId: String) {

    fun encode(): String = Base64.encodeToString(
        "$base\u0000$type\u0000$metaId".toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )

    companion object {
        fun decode(id: String): ItemKey? = runCatching {
            val parts = String(
                Base64.decode(id, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charsets.UTF_8,
            ).split('\u0000')
            if (parts.size == 3 && parts[0].startsWith("https://")) ItemKey(parts[0], parts[1], parts[2]) else null
        }.getOrNull()
    }
}
