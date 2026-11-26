package app.hushstream.core.data.download

import android.content.SharedPreferences

/**
 * Minimal key-value surface for download settings. Lets the settings logic run in JVM tests
 * without Android. Not a port: Nuvio's expect/actual object is replaced by this seam.
 */
interface DownloadPreferenceStore {
    fun getInt(key: String): Int?
    fun putInt(key: String, value: Int)
    fun getBoolean(key: String): Boolean?
    fun putBoolean(key: String, value: Boolean)
}

/** Device-local SharedPreferences, private to the app. */
class SharedPreferencesDownloadStore(private val prefs: SharedPreferences) : DownloadPreferenceStore {

    override fun getInt(key: String): Int? =
        prefs.takeIf { it.contains(key) }?.getInt(key, 0)

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun getBoolean(key: String): Boolean? =
        prefs.takeIf { it.contains(key) }?.getBoolean(key, false)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    companion object {
        const val PREFERENCES_NAME = "hush_downloads_settings"
    }
}
