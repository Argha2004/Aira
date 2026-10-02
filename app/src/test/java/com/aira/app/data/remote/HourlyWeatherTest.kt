package com.aira.app.data.remote

import com.aira.app.domain.model.HourlyWeather
import com.aira.app.domain.model.forTimestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HourlyWeatherTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val sample = """
        {"utc_offset_seconds":19800,
         "hourly":{"time":["2026-09-29T09:00","2026-09-29T10:00","2026-09-29T11:00"],
           "temperature_2m":[30.0,31.0,null],
           "apparent_temperature":[35.0,36.0,37.0],
           "relative_humidity_2m":[60,62,64],
           "rain":[0.0,0.4,null],
           "uv_index":[5.0,6.0,7.0],
           "wind_speed_10m":[8.0,9.0,10.0]}}
    """.trimIndent()

    private fun millis(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 9, 29, hour, minute)
        .toEpochSecond(ZoneOffset.ofTotalSeconds(19800)) * 1000

    @Test
    fun `hours are mapped with times converted using the utc offset`() {
        val hours = json.decodeFromString<HourlyResponse>(sample).toHourlyWeather()
        assertEquals(millis(9), hours[0].time)
        assertEquals(31.0, hours[1].tempC, 0.0)
        assertEquals(36.0, hours[1].feelsLikeC, 0.0)
        assertEquals(62, hours[1].humidity)
        assertEquals(0.4, hours[1].rainMm, 0.0)
    }

    @Test
    fun `an hour without temperature is skipped`() {
        assertEquals(2, json.decodeFromString<HourlyResponse>(sample).toHourlyWeather().size)
    }

    @Test
    fun `missing rain counts as zero`() {
        val noRain = sample.replace("\"temperature_2m\":[30.0,31.0,null]", "\"temperature_2m\":[30.0,31.0,32.0]")
        val hours = json.decodeFromString<HourlyResponse>(noRain).toHourlyWeather()
        assertEquals(0.0, hours[2].rainMm, 0.0)
    }

    // ---- forTimestamp ----

    private fun hour(start: Long) = HourlyWeather(start, 30.0, 35.0, 60, 0.0, 5.0, 8.0)

    private val hours = listOf(hour(millis(9)), hour(millis(10)), hour(millis(11)))

    @Test
    fun `timestamp inside an hour finds that hour`() =
        assertEquals(millis(10), hours.forTimestamp(millis(10, 45))?.time)

    @Test
    fun `timestamp exactly at the start of an hour finds that hour`() =
        assertEquals(millis(11), hours.forTimestamp(millis(11))?.time)

    @Test
    fun `timestamp before all hours finds nothing`() = assertNull(hours.forTimestamp(millis(8, 59)))

    @Test
    fun `timestamp after the last hour finds nothing`() = assertNull(hours.forTimestamp(millis(12)))

    @Test
    fun `empty list finds nothing`() = assertNull(emptyList<HourlyWeather>().forTimestamp(millis(10)))
}
