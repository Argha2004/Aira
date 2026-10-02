package com.aira.app.domain.engine

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeriodReflectionsTest {
    private val start = LocalDate.of(2026, 9, 23)
    private fun chart(sun: List<Int>, rain: List<Int>, feels: List<Double?>) =
        ChartData(sun.indices.map { start.plusDays(it.toLong()) }, sun, rain, feels)

    private val week = chart(
        sun = listOf(45, 62, 35, 50, 70, 40, 0),
        rain = listOf(0, 1, 0, 3, 1, 0, 0),
        feels = listOf(28.0, 36.0, 29.0, 31.0, 32.0, 27.0, null),
    )

    @Test
    fun `sun totals, average, peak and change`() {
        val previous = chart(listOf(100, 100, 0, 0, 0, 0, 0), List(7) { 0 }, List(7) { null })
        val r = PeriodReflections.of(week, previous)
        assertEquals(302, r.sunMinutes)
        assertEquals(43, r.avgSunMinutes)
        assertEquals(start.plusDays(4), r.sunPeakDate)
        assertEquals(51, r.sunChangePercent)
    }

    @Test
    fun `rain and temperatures`() {
        val r = PeriodReflections.of(week, null)
        assertEquals(5, r.rainEncounters)
        assertEquals(3, r.rainDays)
        assertEquals(6, r.loggedDays)
        assertEquals(start.plusDays(1), r.warmestDate)
        assertEquals(36.0, r.warmestFeelsLike!!, 0.001)
        assertEquals(27.0, r.coolestFeelsLike!!, 0.001)
        assertNull(r.sunChangePercent)
    }

    @Test
    fun `outdoor time, its change, heat average and average feels-like`() {
        val current = ChartData(
            dates = week.dates, sunMinutes = week.sunMinutes, rainEncounters = week.rainEncounters, avgFeelsLike = week.avgFeelsLike,
            outdoorMinutes = listOf(60, 60, 60, 60, 60, 0, 0), heatMinutes = listOf(70, 0, 0, 0, 0, 0, 0),
        )
        val previous = current.copy(outdoorMinutes = listOf(100, 100, 0, 0, 0, 0, 0))
        val r = PeriodReflections.of(current, previous)
        assertEquals(300, r.outdoorMinutes)
        assertEquals(50, r.outdoorChangePercent)
        assertEquals(10, r.avgHeatMinutes)
        assertEquals(30.5, r.avgFeelsLike!!, 0.001)
    }

    @Test
    fun `averages use the calendar days of the period`() {
        val year = week.copy(dayCount = 302)
        assertEquals(1, PeriodReflections.of(year, null).avgSunMinutes)
    }

    @Test
    fun `an empty period has no peaks`() {
        val r = PeriodReflections.of(chart(List(7) { 0 }, List(7) { 0 }, List(7) { null }), null)
        assertNull(r.sunPeakDate)
        assertNull(r.warmestDate)
        assertEquals(0, r.loggedDays)
    }
}
