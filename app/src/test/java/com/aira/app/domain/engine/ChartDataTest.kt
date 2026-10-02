package com.aira.app.domain.engine

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartDataTest {

    private val end = LocalDate.of(2026, 9, 29)

    @Test
    fun `a week of chart data ends on the given day, oldest first`() {
        val data = ChartData.lastDays(emptyList(), end, 7)
        assertEquals(7, data.dates.size)
        assertEquals(end.minusDays(6), data.dates.first())
        assertEquals(end, data.dates.last())
    }

    @Test
    fun `days without data count as zero and no temperature`() {
        val data = ChartData.lastDays(emptyList(), end, 3)
        assertEquals(listOf(0, 0, 0), data.sunMinutes)
        assertEquals(listOf(0, 0, 0), data.rainEncounters)
        assertEquals(listOf<Double?>(null, null, null), data.avgFeelsLike)
        assertFalse(data.hasData)
    }

    @Test
    fun `values line up with their dates`() {
        val summaries = listOf(
            day(end, sun = 45, rain = 2, avgFeels = 36.5),
            day(end.minusDays(2), sun = 10, rain = 0, avgFeels = null),
        )
        val data = ChartData.lastDays(summaries, end, 3)
        assertEquals(listOf(10, 0, 45), data.sunMinutes)
        assertEquals(listOf(0, 0, 2), data.rainEncounters)
        assertEquals(listOf(null, null, 36.5), data.avgFeelsLike)
        assertTrue(data.hasData)
    }

    @Test
    fun `outdoor and heat minutes line up with their dates`() {
        val data = ChartData.lastDays(listOf(day(end, sun = 45, outdoor = 90, heat = 20)), end, 2)
        assertEquals(listOf(0, 90), data.outdoorMinutes)
        assertEquals(listOf(0, 20), data.heatMinutes)
        assertEquals(2, data.dayCount)
    }

    @Test
    fun `a year of chart data has one bar per month with sums and an average feels-like`() {
        val sept = java.time.YearMonth.of(2026, 9)
        val summaries = listOf(
            day(LocalDate.of(2026, 9, 1), sun = 30, rain = 1, avgFeels = 30.0, outdoor = 60),
            day(LocalDate.of(2026, 9, 2), sun = 20, rain = 2, avgFeels = 34.0, outdoor = 40),
            day(LocalDate.of(2026, 8, 5), sun = 10),
            day(LocalDate.of(2025, 9, 5), sun = 999), // the year before: not shown
        )
        val data = ChartData.lastMonths(summaries, sept, 12)
        assertEquals(12, data.dates.size)
        assertEquals(LocalDate.of(2025, 10, 1), data.dates.first())
        assertEquals(LocalDate.of(2026, 9, 1), data.dates.last())
        assertEquals(50, data.sunMinutes.last())
        assertEquals(3, data.rainEncounters.last())
        assertEquals(100, data.outdoorMinutes.last())
        assertEquals(32.0, data.avgFeelsLike.last()!!, 0.001)
        assertEquals(10, data.sunMinutes[10])
        assertNull(data.avgFeelsLike[10])
        assertEquals(0, data.sunMinutes.first())
        assertEquals(365, data.dayCount)
    }

    @Test
    fun `days outside the range are ignored`() {
        val data = ChartData.lastDays(listOf(day(end.minusDays(10), sun = 99)), end, 7)
        assertFalse(data.hasData)
    }
}

