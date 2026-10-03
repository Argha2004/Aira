package com.aira.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyEventsTest {
    private val hour = 60 * 60 * 1000L
    private val day = 24 * hour

    @Test
    fun sunProgress_isZeroAtSunriseHalfAtNoonAndOneAtSunset() {
        assertEquals(0f, SkyEvents.sunProgress(6 * hour, 6 * hour, 18 * hour)!!, 0.001f)
        assertEquals(0.5f, SkyEvents.sunProgress(12 * hour, 6 * hour, 18 * hour)!!, 0.001f)
        assertEquals(1f, SkyEvents.sunProgress(18 * hour, 6 * hour, 18 * hour)!!, 0.001f)
    }

    @Test
    fun sunProgress_staysInsideTheDay() {
        assertEquals(0f, SkyEvents.sunProgress(2 * hour, 6 * hour, 18 * hour)!!, 0.001f)
        assertEquals(1f, SkyEvents.sunProgress(22 * hour, 6 * hour, 18 * hour)!!, 0.001f)
    }

    @Test
    fun sunProgress_isNullWithoutTimes() {
        assertNull(SkyEvents.sunProgress(12 * hour, null, 18 * hour))
        assertNull(SkyEvents.sunProgress(12 * hour, 18 * hour, 6 * hour))
    }

    @Test
    fun moon_isNewAtTheKnownNewMoon() {
        val newMoon = 947_182_440_000L
        val moon = SkyEvents.moon(newMoon, null, null, newMoon)
        assertEquals(MoonPhase.NEW, moon.phase)
        assertTrue(moon.illumination < 0.01)
    }

    @Test
    fun moon_isFullHalfACycleLater() {
        val full = 947_182_440_000L + (SkyEvents.SYNODIC_MONTH_DAYS / 2 * day).toLong()
        val moon = SkyEvents.moon(full, null, null, full)
        assertEquals(MoonPhase.FULL, moon.phase)
        assertTrue(moon.illumination > 0.99)
    }

    @Test
    fun phaseOf_coversTheCycle() {
        assertEquals(MoonPhase.NEW, SkyEvents.phaseOf(0.0))
        assertEquals(MoonPhase.FIRST_QUARTER, SkyEvents.phaseOf(0.25))
        assertEquals(MoonPhase.FULL, SkyEvents.phaseOf(0.5))
        assertEquals(MoonPhase.LAST_QUARTER, SkyEvents.phaseOf(0.75))
        assertEquals(MoonPhase.NEW, SkyEvents.phaseOf(0.99))
    }

    @Test
    fun moonTimes_fallInsideToday() {
        val dayStart = 947_182_440_000L - 18 * hour
        val now = dayStart + 10 * day + 12 * hour
        val today = dayStart + 10 * day
        val moon = SkyEvents.moon(now, today + 6 * hour, today + 18 * hour, today)
        assertTrue(moon.rise!! in today until today + day)
        assertTrue(moon.set!! in today until today + day)
    }
}
