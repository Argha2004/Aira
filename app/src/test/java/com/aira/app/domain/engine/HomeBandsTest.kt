package com.aira.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeBandsTest {
    @Test
    fun `light levels at the boundaries`() {
        assertEquals(LightLevel.DIM, HomeBands.light(0f))
        assertEquals(LightLevel.DIM, HomeBands.light(300f))
        assertEquals(LightLevel.INDOOR, HomeBands.light(300.1f))
        assertEquals(LightLevel.INDOOR, HomeBands.light(999f))
        assertEquals(LightLevel.BRIGHT, HomeBands.light(1000f))
        assertEquals(LightLevel.BRIGHT, HomeBands.light(30_000f))
        assertEquals(LightLevel.STRONG_SUN, HomeBands.light(30_001f))
    }

    @Test
    fun `comfort at the boundaries`() {
        assertEquals(Comfort.COOL, HomeBands.comfort(17.9))
        assertEquals(Comfort.COMFORTABLE, HomeBands.comfort(18.0))
        assertEquals(Comfort.COMFORTABLE, HomeBands.comfort(28.0))
        assertEquals(Comfort.WARM, HomeBands.comfort(28.1))
        assertEquals(Comfort.WARM, HomeBands.comfort(33.0))
        assertEquals(Comfort.HOT, HomeBands.comfort(33.1))
    }

    @Test
    fun `uv levels at the boundaries`() {
        assertEquals(UvLevel.LOW, HomeBands.uv(2.9))
        assertEquals(UvLevel.MODERATE, HomeBands.uv(3.0))
        assertEquals(UvLevel.MODERATE, HomeBands.uv(5.9))
        assertEquals(UvLevel.HIGH, HomeBands.uv(6.0))
        assertEquals(UvLevel.HIGH, HomeBands.uv(7.9))
        assertEquals(UvLevel.VERY_HIGH, HomeBands.uv(8.0))
    }

    @Test
    fun `compass points`() {
        assertEquals("N", HomeBands.compass(0))
        assertEquals("N", HomeBands.compass(22))
        assertEquals("NE", HomeBands.compass(23))
        assertEquals("E", HomeBands.compass(90))
        assertEquals("S", HomeBands.compass(180))
        assertEquals("NW", HomeBands.compass(315))
        assertEquals("N", HomeBands.compass(359))
        assertEquals("W", HomeBands.compass(-90))
    }

    @Test
    fun `wind levels at the boundaries`() {
        assertEquals(WindLevel.CALM, HomeBands.wind(5.9))
        assertEquals(WindLevel.GENTLE, HomeBands.wind(6.0))
        assertEquals(WindLevel.GENTLE, HomeBands.wind(19.9))
        assertEquals(WindLevel.BREEZY, HomeBands.wind(20.0))
        assertEquals(WindLevel.BREEZY, HomeBands.wind(38.9))
        assertEquals(WindLevel.WINDY, HomeBands.wind(39.0))
    }

    @Test
    fun `humidity change against yesterday`() {
        assertEquals(-8, HomeBands.humidityChange(64, 72.0))
        assertEquals(5, HomeBands.humidityChange(75, 70.4))
        assertEquals(0, HomeBands.humidityChange(70, 70.0))
        org.junit.Assert.assertNull(HomeBands.humidityChange(70, null))
    }

    @Test
    fun `air quality bands at the boundaries`() {
        assertEquals(AirBand.GOOD, HomeBands.air(50))
        assertEquals(AirBand.MODERATE, HomeBands.air(51))
        assertEquals(AirBand.MODERATE, HomeBands.air(100))
        assertEquals(AirBand.SENSITIVE, HomeBands.air(101))
        assertEquals(AirBand.SENSITIVE, HomeBands.air(150))
        assertEquals(AirBand.UNHEALTHY, HomeBands.air(151))
    }
}
