/*
 * Adapted from NuvioMobile-Enhanced (GPL-3.0), commit 7aca5a0,
 * path composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsStorageStats.kt.
 * Changes: only the data class is ported here (pure JVM). The expect object and the Android
 * StatFs lookup live in core:data (DeviceStorageStatsProvider). Package renamed to app.hushstream.
 */
package app.hushstream.core.domain.download

data class DeviceStorageStats(
    val totalBytes: Long,
    val freeBytes: Long,
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
}
