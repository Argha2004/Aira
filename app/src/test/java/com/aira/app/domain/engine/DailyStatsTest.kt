package com.aira.app.domain.engine

import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyStatsTest {

    private fun sun(minute: Int) = snap(minute, Place.OUTDOOR, lux = 40_000f, feels = 30.0)
    private fun rain(minute: Int) = snap(minute, Place.OUTDOOR, lux = 3000f, rain = 1.0)

    @Test
    fun `no snapshots give empty stats`() = assertEquals(DailyStats.EMPTY, DailyStatsCalculator.calculate(emptyList()))

    @Test
    fun `sun minutes add up the interval of each sunny snapshot`() {
        val stats = DailyStatsCalculator.calculate(listOf(sun(0), sun(30), snap(60)))
        assertEquals(60, stats.sunMinutes)
    }

    @Test
    fun `heat minutes count outdoor snapshots hotter than 33`() {
        val hot = snap(0, Place.OUTDOOR, lux = 5000f, feels = 36.0)
        val cool = snap(30, Place.OUTDOOR, lux = 5000f, feels = 30.0)
        assertEquals(30, DailyStatsCalculator.calculate(listOf(hot, cool, snap(60))).heatMinutes)
    }

    @Test
    fun `neighbouring rainy snapshots are one encounter`() {
        assertEquals(1, DailyStatsCalculator.calculate(listOf(rain(0), rain(30), rain(60))).rainEncounters)
    }

    @Test
    fun `two separate showers are two encounters`() {
        val stats = DailyStatsCalculator.calculate(listOf(rain(0), snap(30), rain(60)))
        assertEquals(2, stats.rainEncounters)
    }

    @Test
    fun `rain while indoors is not an encounter`() {
        assertEquals(0, DailyStatsCalculator.calculate(listOf(snap(0, rain = 5.0))).rainEncounters)
    }

    @Test
    fun `outdoor minutes and steps are summed, missing steps count as zero`() {
        val stats = DailyStatsCalculator.calculate(
            listOf(
                snap(0, Place.OUTDOOR, Movement.WALKING, steps = 500, feels = 25.0),
                snap(30, Place.OUTDOOR, steps = null, feels = 25.0),
                snap(60, steps = 100),
            ),
        )
        assertEquals(60, stats.outdoorMinutes)
        assertEquals(600, stats.steps)
    }

    @Test
    fun `uv dose is uv index times outdoor minutes and gives the band`() {
        // UV 7 for 60 outdoor minutes = 420: high.
        val high = DailyStatsCalculator.calculate(
            listOf(snap(0, Place.OUTDOOR, uv = 7.0, feels = 25.0), snap(30, Place.OUTDOOR, uv = 7.0, feels = 25.0)),
        )
        assertEquals(UvBand.HIGH, high.uvDoseBand)
        // UV 2 for 30 minutes = 60: low.
        val low = DailyStatsCalculator.calculate(listOf(snap(0, Place.OUTDOOR, uv = 2.0, feels = 25.0)))
        assertEquals(UvBand.LOW, low.uvDoseBand)
    }

    @Test
    fun `snapshots waiting for weather add no heat, rain or uv`() {
        val pending = snap(0, Place.OUTDOOR, feels = 40.0, rain = 5.0, uv = 9.0, pending = true)
        val stats = DailyStatsCalculator.calculate(listOf(pending))
        assertEquals(0, stats.heatMinutes)
        assertEquals(0, stats.rainEncounters)
        assertEquals(UvBand.LOW, stats.uvDoseBand)
        assertEquals(30, stats.outdoorMinutes) // where you were is still known
    }
}
