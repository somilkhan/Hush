/*
 * Adapted from NuvioMobile-Enhanced (GPL-3.0), commit 7aca5a0,
 * path composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsStorageStats.android.kt.
 * Changes: the Context is passed in (no global initialize()). The custom download-location
 * branch is omitted because that setting is not ported yet. Package renamed to app.hushstream.
 */
package app.hushstream.core.data.download

import android.content.Context
import android.os.StatFs
import app.hushstream.core.domain.download.DeviceStorageStats

class DeviceStorageStatsProvider(private val context: Context) {

    /** Total and free bytes of the app's private storage. Zeros if the lookup fails. */
    fun stats(): DeviceStorageStats = runCatching {
        val stat = StatFs(context.filesDir.path)
        DeviceStorageStats(
            totalBytes = (stat.blockCountLong * stat.blockSizeLong).coerceAtLeast(0L),
            freeBytes = (stat.availableBlocksLong * stat.blockSizeLong).coerceAtLeast(0L),
        )
    }.getOrElse { DeviceStorageStats(0L, 0L) }
}
