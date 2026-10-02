package com.aira.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotTimingTest {

    @Test
    fun `each snapshot counts until the next one`() =
        assertEquals(listOf(5.0, 5.0, 5.0), SnapshotTiming.minutes(listOf(snap(0), snap(5), snap(10))))

    @Test
    fun `the last snapshot counts the same as the gap before it`() {
        assertEquals(listOf(30.0, 30.0), SnapshotTiming.minutes(listOf(snap(0), snap(30))))
        assertEquals(listOf(5.0, 5.0), SnapshotTiming.minutes(listOf(snap(0), snap(5))))
    }

    @Test
    fun `a long gap counts at most 60 minutes`() =
        assertEquals(listOf(60.0, 60.0), SnapshotTiming.minutes(listOf(snap(0), snap(180))))

    @Test
    fun `a single snapshot counts 30 minutes, none counts nothing`() {
        assertEquals(listOf(30.0), SnapshotTiming.minutes(listOf(snap(0))))
        assertTrue(SnapshotTiming.minutes(emptyList()).isEmpty())
    }

    @Test
    fun `a new snapshot is too soon within 4 minutes of the last one`() {
        val minute = 60_000L
        assertTrue(SnapshotTiming.tooSoon(10 * minute, 13 * minute))
        assertFalse(SnapshotTiming.tooSoon(10 * minute, 14 * minute))
        assertFalse(SnapshotTiming.tooSoon(null, 14 * minute))
        assertFalse(SnapshotTiming.tooSoon(20 * minute, 14 * minute)) // a clock change: take one anyway
    }
}
