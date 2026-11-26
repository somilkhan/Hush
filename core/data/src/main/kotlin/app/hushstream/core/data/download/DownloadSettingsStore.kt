/*
 * Adapted from NuvioMobile-Enhanced (GPL-3.0), commit 7aca5a0,
 * path composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsSettingsRepository.kt
 * (and DownloadsSettingsStorage.android.kt for the SharedPreferences keys).
 * Changes: StateFlow and the Nuvio singleton are dropped; reads and writes go straight to
 * DownloadPreferenceStore. Only the concurrency limits and mobile-data flag are kept
 * (download location and resolution settings arrive in later slices). Package renamed to app.hushstream.
 */
package app.hushstream.core.data.download

import app.hushstream.core.domain.download.DownloadConcurrencyLimit

/**
 * Device-level download preferences. Not per-profile or synced: whether this device may use
 * mobile data for downloads is a per-device fact.
 */
class DownloadSettingsStore(private val store: DownloadPreferenceStore) {

    /** The stored limit clamped to 1..MAX, or the default when nothing is stored. */
    fun concurrencyLimit(limit: DownloadConcurrencyLimit): Int =
        store.getInt(limit.storageKey)?.let(limit::clamp) ?: limit.defaultValue

    /** Clamps [count], stores it, and returns the stored value. */
    fun setConcurrencyLimit(limit: DownloadConcurrencyLimit, count: Int): Int {
        val clamped = limit.clamp(count)
        store.putInt(limit.storageKey, clamped)
        return clamped
    }

    /** Defaults to false: downloads wait for Wi-Fi or an unmetered connection. */
    fun allowMobileDataDownloads(): Boolean =
        store.getBoolean(ALLOW_MOBILE_DATA_KEY) ?: false

    fun setAllowMobileDataDownloads(enabled: Boolean) {
        store.putBoolean(ALLOW_MOBILE_DATA_KEY, enabled)
    }

    private companion object {
        const val ALLOW_MOBILE_DATA_KEY = "allow_mobile_data_downloads"
    }
}
