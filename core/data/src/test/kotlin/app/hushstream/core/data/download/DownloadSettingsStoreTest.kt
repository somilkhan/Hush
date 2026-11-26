package app.hushstream.core.data.download

import app.hushstream.core.domain.download.DownloadConcurrencyLimit
import app.hushstream.core.domain.download.DownloadQueuePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadSettingsStoreTest {

    private class MemoryStore : DownloadPreferenceStore {
        val ints = mutableMapOf<String, Int>()
        val booleans = mutableMapOf<String, Boolean>()
        override fun getInt(key: String): Int? = ints[key]
        override fun putInt(key: String, value: Int) { ints[key] = value }
        override fun getBoolean(key: String): Boolean? = booleans[key]
        override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
    }

    @Test
    fun emptyStoreReturnsDefaults() {
        val settings = DownloadSettingsStore(MemoryStore())
        assertEquals(3, settings.concurrencyLimit(DownloadConcurrencyLimit.TotalDownloads))
        assertEquals(1, settings.concurrencyLimit(DownloadConcurrencyLimit.EpisodesPerShow))
        assertEquals(1, settings.concurrencyLimit(DownloadConcurrencyLimit.ConcurrentShows))
        assertFalse(settings.allowMobileDataDownloads())
    }

    @Test
    fun storedOutOfRangeValuesAreClampedOnRead() {
        val store = MemoryStore()
        store.ints[DownloadConcurrencyLimit.TotalDownloads.storageKey] = 0
        store.ints[DownloadConcurrencyLimit.EpisodesPerShow.storageKey] = 99
        val settings = DownloadSettingsStore(store)
        assertEquals(1, settings.concurrencyLimit(DownloadConcurrencyLimit.TotalDownloads))
        assertEquals(5, settings.concurrencyLimit(DownloadConcurrencyLimit.EpisodesPerShow))
    }

    @Test
    fun setClampsAndReturnsStoredValue() {
        val store = MemoryStore()
        val settings = DownloadSettingsStore(store)
        assertEquals(1, settings.setConcurrencyLimit(DownloadConcurrencyLimit.ConcurrentShows, -4))
        assertEquals(5, settings.setConcurrencyLimit(DownloadConcurrencyLimit.TotalDownloads, 12))
        assertEquals(1, store.ints[DownloadConcurrencyLimit.ConcurrentShows.storageKey])
        assertEquals(5, settings.concurrencyLimit(DownloadConcurrencyLimit.TotalDownloads))
    }

    @Test
    fun mobileDataFlagRoundTrips() {
        val settings = DownloadSettingsStore(MemoryStore())
        settings.setAllowMobileDataDownloads(true)
        assertTrue(settings.allowMobileDataDownloads())
        settings.setAllowMobileDataDownloads(false)
        assertFalse(settings.allowMobileDataDownloads())
    }

    @Test
    fun factoryBuildsPolicyFromSettings() {
        val settings = DownloadSettingsStore(MemoryStore())
        settings.setConcurrencyLimit(DownloadConcurrencyLimit.TotalDownloads, 4)
        val policy = DownloadQueuePolicyFactory(settings).create(hasConnection = true, onUnmeteredNetwork = true)
        assertEquals(DownloadQueuePolicy(4, 1, 1, isNetworkAllowed = true), policy)
    }

    @Test
    fun factoryBlocksMeteredNetworkUnlessAllowed() {
        val settings = DownloadSettingsStore(MemoryStore())
        val factory = DownloadQueuePolicyFactory(settings)

        assertFalse(factory.create(hasConnection = true, onUnmeteredNetwork = false).isNetworkAllowed)
        assertFalse(factory.create(hasConnection = false, onUnmeteredNetwork = true).isNetworkAllowed)

        settings.setAllowMobileDataDownloads(true)
        assertTrue(factory.create(hasConnection = true, onUnmeteredNetwork = false).isNetworkAllowed)
        assertFalse(factory.create(hasConnection = false, onUnmeteredNetwork = false).isNetworkAllowed)
    }
}
