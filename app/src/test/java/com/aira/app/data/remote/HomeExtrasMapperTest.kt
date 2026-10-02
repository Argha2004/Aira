package com.aira.app.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeExtrasMapperTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val forecast = """
        {"utc_offset_seconds":19800,"elevation":9.0,
         "current":{"temperature_2m":28.0,"apparent_temperature":31.0,"relative_humidity_2m":72,
                    "weather_code":2,"wind_speed_10m":12.0,"pressure_msl":1013.6},
         "daily":{"sunrise":["2026-09-29T05:27"],"sunset":["2026-09-29T17:37"],
                  "temperature_2m_max":[29.4,30.0],"temperature_2m_min":[21.1,22.0]}}
    """.trimIndent()

    @Test
    fun `today's high, low and the pressure are read`() {
        val weather = json.decodeFromString<ForecastResponse>(forecast).toWeatherNow()
        assertEquals(29.4, weather.highC!!, 0.001)
        assertEquals(21.1, weather.lowC!!, 0.001)
        assertEquals(1013.6, weather.pressureHpa!!, 0.001)
        assertEquals(9.0, weather.elevationM!!, 0.001)
    }

    @Test
    fun `missing high, low and pressure stay null`() {
        val bare = """
            {"current":{"temperature_2m":28.0,"apparent_temperature":31.0,"relative_humidity_2m":72,
                        "weather_code":2,"wind_speed_10m":12.0}}
        """.trimIndent()
        val weather = json.decodeFromString<ForecastResponse>(bare).toWeatherNow()
        assertNull(weather.highC)
        assertNull(weather.lowC)
        assertNull(weather.pressureHpa)
        assertNull(weather.elevationM)
    }

    @Test
    fun `wind direction and the hourly temperature and weather code are read`() {
        val text = """
            {"utc_offset_seconds":0,
             "current":{"temperature_2m":28.0,"apparent_temperature":31.0,"relative_humidity_2m":72,
                        "weather_code":2,"wind_speed_10m":12.0,"wind_direction_10m":45.0},
             "hourly":{"time":["2026-09-29T10:00","2026-09-29T11:00"],"precipitation_probability":[10,70],
                       "precipitation":[0.0,1.2],"apparent_temperature":[30.0,29.0],
                       "temperature_2m":[28.0,null],"weather_code":[2,null]}}
        """.trimIndent()
        val response = json.decodeFromString<ForecastResponse>(text)
        assertEquals(45, response.toWeatherNow().windDirection)
        val hours = response.toHourlyForecast()
        assertEquals(28.0, hours[0].tempC!!, 0.001)
        assertEquals(2, hours[0].weatherCode)
        assertNull(hours[1].tempC)
        assertNull(hours[1].weatherCode)
    }

    @Test
    fun `air quality is read`() {
        val text = """{"current":{"us_aqi":34.0,"pm2_5":8.4}}"""
        val air = json.decodeFromString<AirQualityResponse>(text).toAirQuality()!!
        assertEquals(34, air.aqi)
        assertEquals(8.4, air.pm25, 0.001)
    }

    @Test
    fun `no air quality index gives no air quality`() {
        val text = """{"current":{"pm2_5":8.4}}"""
        assertNull(json.decodeFromString<AirQualityResponse>(text).toAirQuality())
    }

    @Test
    fun `missing fine dust counts as zero`() {
        val text = """{"current":{"us_aqi":60.0}}"""
        assertEquals(0.0, json.decodeFromString<AirQualityResponse>(text).toAirQuality()!!.pm25, 0.001)
    }
}
