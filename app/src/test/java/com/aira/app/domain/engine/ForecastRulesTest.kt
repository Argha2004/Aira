package com.aira.app.domain.engine

import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.HourlyForecast
import com.aira.app.domain.model.Place
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val UTC = ZoneOffset.UTC
private fun millis(hour: Int, minute: Int = 0, day: Int = 29) =
    LocalDateTime.of(2026, 9, day, hour, minute).toInstant(UTC).toEpochMilli()

/** A forecast hour starting at [hour] o'clock on 29 September 2026 (UTC). */
private fun hourAt(hour: Int, rain: Int = 0, feels: Double = 30.0, day: Int = 29) =
    HourlyForecast(time = millis(hour, day = day), precipProbability = rain, precipitationMm = 0.0, feelsLikeC = feels)

class RainSoonTest {

    private val now = millis(10, 30)
    private val indoors = listOf(snap(0, atMillis = now))

    private fun alerts(vararg forecast: HourlyForecast) = AlertEngine.evaluate(indoors, forecast.toList(), now)

    @Test
    fun `rain likely in the next hour gives rain soon`() =
        assertTrue(AlertType.RAIN_SOON in alerts(hourAt(10, 30), hourAt(11, 70)))

    @Test
    fun `exactly 60 percent counts`() = assertTrue(AlertType.RAIN_SOON in alerts(hourAt(11, 60)))

    @Test
    fun `59 percent does not`() = assertFalse(AlertType.RAIN_SOON in alerts(hourAt(11, 59)))

    @Test
    fun `the hour we are in counts`() = assertTrue(AlertType.RAIN_SOON in alerts(hourAt(10, 80)))

    @Test
    fun `an hour that is already over is ignored`() = assertFalse(AlertType.RAIN_SOON in alerts(hourAt(9, 90)))

    @Test
    fun `rain more than 3 hours away is ignored`() = assertFalse(AlertType.RAIN_SOON in alerts(hourAt(14, 90)))

    @Test
    fun `an hour starting within the 3 hours counts`() = assertTrue(AlertType.RAIN_SOON in alerts(hourAt(13, 90)))

    @Test
    fun `no forecast gives no rain soon alert`() = assertFalse(AlertType.RAIN_SOON in alerts())

    @Test
    fun `the earliest rainy hour is the one reported`() {
        val first = AlertEngine.rainSoonHour(listOf(hourAt(12, 90), hourAt(11, 65)), now)
        assertEquals(millis(11), first?.time)
    }

    @Test
    fun `rain soon works together with the other alerts`() {
        val hot = listOf(snap(0, Place.OUTDOOR, lux = 5000f, feels = 40.0, atMillis = now))
        val result = AlertEngine.evaluate(hot, listOf(hourAt(11, 80)), now)
        assertTrue(result.containsAll(listOf(AlertType.RAIN_SOON, AlertType.HEAT)))
    }
}

class OutdoorPlanCheckerTest {

    private val now = millis(9, 0)

    private fun check(dueTime: Long, vararg forecast: HourlyForecast, at: Long = now) =
        OutdoorPlanChecker.check(dueTime, forecast.toList(), at, UTC)

    @Test
    fun `good weather at the planned hour gives no warning`() =
        assertNull(check(millis(14), hourAt(13), hourAt(14), hourAt(15)))

    @Test
    fun `rain likely at the planned hour gives a rain warning`() {
        val warning = check(millis(14), hourAt(14, rain = 80))!!
        assertEquals(PlanIssue.RAIN, warning.issue)
        assertEquals(millis(14), warning.badHour.time)
    }

    @Test
    fun `heat at the planned hour gives a heat warning`() =
        assertEquals(PlanIssue.HEAT, check(millis(14), hourAt(14, feels = 40.0))!!.issue)

    @Test
    fun `rain is reported before heat when both apply`() =
        assertEquals(PlanIssue.RAIN, check(millis(14), hourAt(14, rain = 90, feels = 42.0))!!.issue)

    @Test
    fun `exactly 60 percent warns and 59 does not`() {
        assertEquals(PlanIssue.RAIN, check(millis(14), hourAt(14, rain = 60))!!.issue)
        assertNull(check(millis(14), hourAt(14, rain = 59)))
    }

    @Test
    fun `exactly 38 degrees is fine and just above warns`() {
        assertNull(check(millis(14), hourAt(14, feels = 38.0)))
        assertEquals(PlanIssue.HEAT, check(millis(14), hourAt(14, feels = 38.1))!!.issue)
    }

    @Test
    fun `the nearest good hour is suggested`() {
        // Planned 14:00 (rain). Good hours: 11:00 (3 h earlier) and 16:00 (2 h later). 16:00 is nearer.
        val warning = check(millis(14), hourAt(11), hourAt(12, rain = 70), hourAt(13, rain = 70), hourAt(14, rain = 80), hourAt(16))!!
        assertEquals(millis(16), warning.betterHour?.time)
    }

    @Test
    fun `between two equally near good hours the earlier one wins`() {
        val warning = check(millis(14), hourAt(12), hourAt(13, rain = 90), hourAt(14, rain = 90), hourAt(15, rain = 90), hourAt(16))!!
        assertEquals(millis(12), warning.betterHour?.time)
    }

    @Test
    fun `a good hour right next to the planned one is preferred`() {
        val warning = check(millis(14), hourAt(11), hourAt(14, rain = 90), hourAt(15))!!
        assertEquals(millis(15), warning.betterHour?.time)
    }

    @Test
    fun `no better hour today gives a warning without a suggestion`() {
        val warning = check(millis(14), hourAt(13, rain = 90), hourAt(14, rain = 90), hourAt(15, rain = 90))!!
        assertNull(warning.betterHour)
    }

    @Test
    fun `hours that are already over are not suggested`() {
        // It is 12:30. 11:00 is over. Only 15:00 is still ahead.
        val warning = check(millis(14), hourAt(11), hourAt(14, rain = 90), hourAt(15), at = millis(12, 30))!!
        assertEquals(millis(15), warning.betterHour?.time)
    }

    @Test
    fun `the hour we are in can still be suggested`() {
        val warning = check(millis(14), hourAt(12), hourAt(14, rain = 90), at = millis(12, 30))!!
        assertEquals(millis(12), warning.betterHour?.time)
    }

    @Test
    fun `a good hour on another day is not suggested`() {
        val warning = check(millis(14), hourAt(14, rain = 90), hourAt(10, day = 30))!!
        assertNull(warning.betterHour)
    }

    @Test
    fun `a due time inside the hour matches that hour`() =
        assertEquals(PlanIssue.RAIN, check(millis(14, 45), hourAt(14, rain = 90))!!.issue)

    @Test
    fun `no forecast for that hour gives no warning`() = assertNull(check(millis(14), hourAt(10, rain = 90)))

    @Test
    fun `an empty forecast gives no warning`() = assertNull(OutdoorPlanChecker.check(millis(14), emptyList(), now, UTC))
}
