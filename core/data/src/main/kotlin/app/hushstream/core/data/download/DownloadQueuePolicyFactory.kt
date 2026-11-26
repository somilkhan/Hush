/*
 * Adapted from NuvioMobile-Enhanced (GPL-3.0), commit 7aca5a0,
 * path composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadQueuePolicy.kt
 * (the companion current() function).
 * Changes: current() read a global settings singleton and checked the network itself. It is now a
 * factory that takes the settings and the network state as arguments, so core:domain stays pure.
 * Network allowance is (connected) and (unmetered or mobile data allowed in settings).
 * Package renamed to app.hushstream.
 */
package app.hushstream.core.data.download

import app.hushstream.core.domain.download.DownloadConcurrencyLimit
import app.hushstream.core.domain.download.DownloadQueuePolicy

class DownloadQueuePolicyFactory(private val settings: DownloadSettingsStore) {

    /**
     * Builds the policy from the current limits. [hasConnection] is false when offline;
     * [onUnmeteredNetwork] is true on Wi-Fi or other unmetered links.
     */
    fun create(hasConnection: Boolean, onUnmeteredNetwork: Boolean): DownloadQueuePolicy =
        DownloadQueuePolicy(
            maxDownloads = settings.concurrencyLimit(DownloadConcurrencyLimit.TotalDownloads),
            maxEpisodesPerShow = settings.concurrencyLimit(DownloadConcurrencyLimit.EpisodesPerShow),
            maxShows = settings.concurrencyLimit(DownloadConcurrencyLimit.ConcurrentShows),
            isNetworkAllowed = hasConnection &&
                (onUnmeteredNetwork || settings.allowMobileDataDownloads()),
        )
}
