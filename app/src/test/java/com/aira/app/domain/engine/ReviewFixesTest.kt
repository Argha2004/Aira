package com.aira.app.domain.engine

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class LoggingReadinessTest {

    private fun status(enabled: Boolean = true, location: Boolean = true, background: Boolean = true, sdk: Int = 34) =
        LoggingReadiness.check(enabled, location, background, sdk)

    @Test fun `everything granted is ok`() = assertEquals(LoggingStatus.OK, status())

    @Test fun `logging switched off is reported as off, whatever the permissions`() =
        assertEquals(LoggingStatus.OFF, status(enabled = false, location = false, background = false))

    @Test fun `no location permission needs location`() =
        assertEquals(LoggingStatus.NEEDS_LOCATION, status(location = false, background = false))

    @Test fun `location without all the time on android 10 or newer needs background location`() =
        assertEquals(LoggingStatus.NEEDS_BACKGROUND_LOCATION, status(background = false, sdk = 29))

    @Test fun `background permission is not asked for before android 10`() =
        assertEquals(LoggingStatus.OK, status(background = false, sdk = 28))

    @Test fun `android 8 with location is ok`() = assertEquals(LoggingStatus.OK, status(background = false, sdk = 26))
}

class WeatherFillPlannerTest {

    private val utc = ZoneOffset.UTC
    private fun millis(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 9, day, hour, minute).toInstant(utc).toEpochMilli()

    private val now = millis(29, 12)
    private fun pending(at: Long) = snap(0, atMillis = at, pending = true)

    @Test
    fun `snapshots of today and yesterday can be filled`() {
        val list = listOf(pending(millis(29, 8)), pending(millis(28, 20)))
        assertEquals(list, WeatherFillPlanner.fillable(list, now, utc))
    }

    @Test
    fun `the very start of yesterday still counts`() {
        val list = listOf(pending(millis(28, 0)))
        assertEquals(1, WeatherFillPlanner.fillable(list, now, utc).size)
    }

    @Test
    fun `the last minute of the day before yesterday cannot be filled`() {
        val list = listOf(pending(millis(27, 23, 59)))
        assertEquals(0, WeatherFillPlanner.fillable(list, now, utc).size)
    }

    @Test
    fun `only the old ones are dropped`() {
        val recent = pending(millis(29, 8))
        val old = pending(millis(20, 8))
        assertEquals(listOf(recent), WeatherFillPlanner.fillable(listOf(old, recent), now, utc))
    }

    @Test
    fun `nothing pending gives nothing`() = assertEquals(0, WeatherFillPlanner.fillable(emptyList(), now, utc).size)

    @Test
    fun `yesterday is decided by the local zone`() {
        val kolkata = java.time.ZoneId.of("Asia/Kolkata")
        // 28 Sep 00:00 in Kolkata is 27 Sep 18:30 UTC.
        val justInside = pending(LocalDateTime.of(2026, 9, 27, 18, 30).toInstant(utc).toEpochMilli())
        val justOutside = pending(LocalDateTime.of(2026, 9, 27, 18, 29).toInstant(utc).toEpochMilli())
        assertEquals(listOf(justInside), WeatherFillPlanner.fillable(listOf(justOutside, justInside), now, kolkata))
    }
}
