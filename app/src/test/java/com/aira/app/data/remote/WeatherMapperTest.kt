package com.aira.app.data.remote

import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherMapperTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val sample = """
        {"latitude":22.6,"longitude":88.3,"utc_offset_seconds":19800,"timezone":"Asia/Kolkata",
         "current":{"time":"2026-09-29T10:45","interval":900,"temperature_2m":31.5,
           "apparent_temperature":37.7,"relative_humidity_2m":64,"precipitation":0.0,"rain":0.4,
           "weather_code":63,"wind_speed_10m":8.1,"uv_index":7.65,"is_day":1},
         "daily":{"time":["2026-09-29"],"sunrise":["2026-09-29T05:27"],"sunset":["2026-09-29T17:26"]}}
    """.trimIndent()

    @Test
    fun `maps all current fields`() {
        val weather = json.decodeFromString<ForecastResponse>(sample).toWeatherNow()
        assertEquals(31.5, weather.tempC, 0.0)
        assertEquals(37.7, weather.feelsLikeC, 0.0)
        assertEquals(64, weather.humidity)
        assertEquals(0.4, weather.rainMm, 0.0)
        assertEquals(7.65, weather.uvIndex, 0.0)
        assertEquals(8.1, weather.windKmh, 0.0)
        assertEquals(63, weather.weatherCode)
        assertTrue(weather.isDay)
    }

    @Test
    fun `sunrise is converted to epoch millis using the utc offset`() {
        val weather = json.decodeFromString<ForecastResponse>(sample).toWeatherNow()
        val expected = LocalDateTime.of(2026, 9, 29, 5, 27)
            .toEpochSecond(ZoneOffset.ofTotalSeconds(19800)) * 1000
        assertEquals(expected, weather.sunrise)
    }

    @Test
    fun `night is mapped to isDay false`() {
        val night = sample.replace("\"is_day\":1", "\"is_day\":0")
        assertFalse(json.decodeFromString<ForecastResponse>(night).toWeatherNow().isDay)
    }

    @Test
    fun `missing daily block gives null sunrise and sunset`() {
        val noDaily = """
            {"utc_offset_seconds":19800,
             "current":{"temperature_2m":31.5,"apparent_temperature":37.7,"relative_humidity_2m":64,
               "weather_code":63,"wind_speed_10m":8.1}}
        """.trimIndent()
        val weather = json.decodeFromString<ForecastResponse>(noDaily).toWeatherNow()
        assertNull(weather.sunrise)
        assertNull(weather.sunset)
    }
}
