package com.aira.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PressureTrendTest {

    private val hour = 60 * 60 * 1000L

    @Test
    fun `no readings is no drop`() = assertFalse(PressureTrend.detectDrop(emptyList()))

    @Test
    fun `one reading is no drop`() = assertFalse(PressureTrend.detectDrop(listOf(0L to 1010f)))

    @Test
    fun `fall of more than 3 hPa in 2 hours is a drop`() =
        assertTrue(PressureTrend.detectDrop(listOf(0L to 1012f, hour to 1010f, 2 * hour to 1008.5f)))

    @Test
    fun `fall of exactly 3 hPa is not a drop`() =
        assertFalse(PressureTrend.detectDrop(listOf(0L to 1010f, 3 * hour to 1007f)))

    @Test
    fun `fall older than 3 hours is ignored`() =
        assertFalse(PressureTrend.detectDrop(listOf(0L to 1020f, 4 * hour to 1010f, 5 * hour to 1009f)))

    @Test
    fun `rising pressure is not a drop`() =
        assertFalse(PressureTrend.detectDrop(listOf(0L to 1005f, hour to 1010f)))

    @Test
    fun `readings in any order give the same answer`() =
        assertTrue(PressureTrend.detectDrop(listOf(2 * hour to 1006f, 0L to 1012f, hour to 1009f)))
}

class GeoMathTest {

    @Test
    fun `same point is zero km`() = assertEquals(0.0, GeoMath.distanceKm(22.57, 88.36, 22.57, 88.36), 1e-9)

    @Test
    fun `0_01 degrees of latitude is about 1_1 km`() =
        assertEquals(1.11, GeoMath.distanceKm(22.57, 88.36, 22.58, 88.36), 0.02)

    @Test
    fun `Kolkata to Delhi is about 1300 km`() =
        assertEquals(1305.0, GeoMath.distanceKm(22.57, 88.36, 28.61, 77.20), 30.0)

    @Test
    fun `distance is the same in both directions`() =
        assertEquals(
            GeoMath.distanceKm(22.57, 88.36, 28.61, 77.20),
            GeoMath.distanceKm(28.61, 77.20, 22.57, 88.36),
            1e-9,
        )
}
