package app.hush.placeholder.core.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

// NOT RUN in this sandbox: no JDK or Gradle is verified here. CI must run :core:data:test to prove it.
class SyncMergeTest {

    private data class Row(val key: String, val value: String, val at: Long)

    private fun plan(local: List<Row>, remote: List<Row>) =
        SyncMerge.plan(local, remote, key = { it.key }, updatedAtEpochMs = { it.at })

    @Test
    fun newerLocalRowIsPushed() {
        val p = plan(listOf(Row("a", "local", 200)), listOf(Row("a", "remote", 100)))
        assertEquals(listOf(Row("a", "local", 200)), p.pushRemote)
        assertEquals(emptyList<Row>(), p.applyLocal)
    }

    @Test
    fun newerRemoteRowIsApplied() {
        val p = plan(listOf(Row("a", "local", 100)), listOf(Row("a", "remote", 200)))
        assertEquals(listOf(Row("a", "remote", 200)), p.applyLocal)
        assertEquals(emptyList<Row>(), p.pushRemote)
    }

    @Test
    fun tieGoesToRemote() {
        val p = plan(listOf(Row("a", "local", 100)), listOf(Row("a", "remote", 100)))
        assertEquals(listOf(Row("a", "remote", 100)), p.applyLocal)
        assertEquals(emptyList<Row>(), p.pushRemote)
    }

    @Test
    fun identicalRowsDoNothing() {
        val p = plan(listOf(Row("a", "same", 100)), listOf(Row("a", "same", 100)))
        assertEquals(emptyList<Row>(), p.applyLocal)
        assertEquals(emptyList<Row>(), p.pushRemote)
    }

    @Test
    fun localOnlyRowIsPushedAndRemoteOnlyRowIsApplied() {
        val p = plan(listOf(Row("l", "x", 1)), listOf(Row("r", "y", 1)))
        assertEquals(listOf(Row("r", "y", 1)), p.applyLocal)
        assertEquals(listOf(Row("l", "x", 1)), p.pushRemote)
    }

    @Test
    fun emptySidesProduceEmptyPlan() {
        val p = plan(emptyList(), emptyList())
        assertEquals(emptyList<Row>(), p.applyLocal)
        assertEquals(emptyList<Row>(), p.pushRemote)
    }
}
