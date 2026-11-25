package app.hush.placeholder.core.data.sync

/** What to write where after comparing a local snapshot with a remote snapshot. */
data class MergePlan<T>(
    /** Rows the remote side wins. Write these into the local store. */
    val applyLocal: List<T>,
    /** Rows the local side wins, or that exist only locally. Write these to the remote. */
    val pushRemote: List<T>,
)

/**
 * Conflict rule for sync: for each key, the row with the newer updatedAtEpochMs wins.
 * A tie goes to the remote copy, so devices converge on the same value.
 * Rows that exist on one side only are copied to the other side.
 */
object SyncMerge {

    fun <T, K> plan(
        local: List<T>,
        remote: List<T>,
        key: (T) -> K,
        updatedAtEpochMs: (T) -> Long,
    ): MergePlan<T> {
        val localByKey = local.associateBy(key)
        val remoteByKey = remote.associateBy(key)
        val applyLocal = mutableListOf<T>()
        val pushRemote = mutableListOf<T>()

        for ((k, remoteRow) in remoteByKey) {
            val localRow = localByKey[k]
            if (localRow == null) {
                applyLocal += remoteRow
            } else if (localRow == remoteRow) {
                continue
            } else if (updatedAtEpochMs(localRow) > updatedAtEpochMs(remoteRow)) {
                pushRemote += localRow
            } else {
                applyLocal += remoteRow
            }
        }
        for ((k, localRow) in localByKey) {
            if (k !in remoteByKey) pushRemote += localRow
        }
        return MergePlan(applyLocal = applyLocal, pushRemote = pushRemote)
    }
}
