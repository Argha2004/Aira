package com.aira.app.domain.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightPauseTest {

    @Test
    fun `22 00 is not paused`() = assertFalse(NightPause.isPaused(22))

    @Test
    fun `23 00 is paused`() = assertTrue(NightPause.isPaused(23))

    @Test
    fun `midnight is paused`() = assertTrue(NightPause.isPaused(0))

    @Test
    fun `05 00 is still paused`() = assertTrue(NightPause.isPaused(5))

    @Test
    fun `06 00 is not paused`() = assertFalse(NightPause.isPaused(6))

    @Test
    fun `midday is not paused`() = assertFalse(NightPause.isPaused(12))

    @Test
    fun `daytime by clock is 06 to 18`() {
        assertFalse(NightPause.isDaytimeByClock(5))
        assertTrue(NightPause.isDaytimeByClock(6))
        assertTrue(NightPause.isDaytimeByClock(17))
        assertFalse(NightPause.isDaytimeByClock(18))
    }
}
