package com.aira.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeTest {

    @Test
    fun `known codes have descriptions`() {
        assertEquals("Clear sky", WeatherCode.describe(0))
        assertEquals("Partly cloudy", WeatherCode.describe(2))
        assertEquals("Fog", WeatherCode.describe(48))
        assertEquals("Rain", WeatherCode.describe(63))
        assertEquals("Thunderstorm", WeatherCode.describe(95))
    }

    @Test
    fun `unknown code falls back to Unknown`() {
        assertEquals("Unknown", WeatherCode.describe(1234))
        assertEquals("Unknown", WeatherCode.describe(-1))
    }
}
