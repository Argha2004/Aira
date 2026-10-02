package com.aira.app.domain.engine

import com.aira.app.domain.model.DiaryEventType
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataTest {

    private val date = LocalDate.of(2026, 9, 29)
    private val snapshots = DemoData.day(date, ZoneOffset.UTC)

    @Test
    fun `one snapshot every 30 minutes from 06 30 to 22 30 inside the day`() {
        val range = DayRange.of(date, ZoneOffset.UTC)
        assertEquals(33, snapshots.size)
        assertTrue(snapshots.all { it.timestamp in range })
        assertEquals(snapshots.map { it.timestamp }.sorted(), snapshots.map { it.timestamp })
        assertTrue(snapshots.zipWithNext().all { (a, b) -> b.timestamp - a.timestamp == 30 * 60_000L })
    }

    @Test
    fun `the timeline shows every kind of event the diary can build`() {
        val types = DiaryBuilder.build(snapshots).map { it.type }.toSet()
        assertTrue(
            types.containsAll(
                listOf(
                    DiaryEventType.SUN, DiaryEventType.RAIN, DiaryEventType.HEAT,
                    DiaryEventType.INDOOR, DiaryEventType.PRESSURE_DROP,
                ),
            ),
        )
    }

    @Test
    fun `home rings have something to show`() {
        val stats = DailyStatsCalculator.calculate(snapshots)
        assertTrue(stats.sunMinutes > 0)
        assertTrue(stats.heatMinutes > 0)
        assertEquals(1, stats.rainEncounters)
        assertTrue(stats.outdoorMinutes > 0)
        assertTrue(stats.steps > 0)
    }

    @Test
    fun `step totals only ever go up`() {
        val totals = snapshots.map { it.totalSteps!! }
        assertEquals(totals.sorted(), totals)
    }

    @Test
    fun `no demo snapshot is waiting for weather`() = assertTrue(snapshots.none { it.weatherPending })
}
