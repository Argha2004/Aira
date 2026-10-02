package com.aira.app.domain.engine

import com.aira.app.data.remote.ForecastResponse
import com.aira.app.data.remote.toHourlyForecast
import com.aira.app.domain.model.AlertType
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskSuggestionsTest {

    @Test
    fun `rain soon suggests an umbrella first`() =
        assertEquals(
            listOf("Carry an umbrella / raincoat", "Bring clothes inside", "Cover bike seat"),
            TaskSuggestions.forAlert(AlertType.RAIN_SOON),
        )

    @Test
    fun `each alert has the suggestions of the project notes`() {
        assertEquals(listOf("Find shelter", "Protect phone and bag"), TaskSuggestions.forAlert(AlertType.RAIN_NOW))
        assertEquals(listOf("Drink water", "Rest in shade", "Carry a water bottle"), TaskSuggestions.forAlert(AlertType.HEAT))
        assertEquals(listOf("Apply sunscreen", "Wear a cap"), TaskSuggestions.forAlert(AlertType.STRONG_SUN))
        assertEquals(listOf("Close windows", "Bring clothes inside"), TaskSuggestions.forAlert(AlertType.PRESSURE_DROP))
    }

    @Test
    fun `every alert has two or three suggestions`() =
        assertTrue(AlertType.entries.all { TaskSuggestions.forAlert(it).size in 2..3 })
}

class AlertMessagesTest {

    @Test
    fun `rain soon names the hour`() =
        assertEquals("Rain likely around 4 PM", AlertMessages.headline(AlertType.RAIN_SOON, 80.0, "4 PM"))

    @Test
    fun `rain soon without an hour still reads well`() =
        assertEquals("Rain likely around soon", AlertMessages.headline(AlertType.RAIN_SOON, 80.0))

    @Test
    fun `heat names the temperature`() =
        assertEquals("Feels like 40 °C", AlertMessages.headline(AlertType.HEAT, 40.2))

    @Test
    fun `strong sun names the minutes`() =
        assertEquals("45 minutes in strong sun", AlertMessages.headline(AlertType.STRONG_SUN, 45.0))

    @Test
    fun `rain now and pressure have fixed headlines`() {
        assertEquals("It's raining where you are", AlertMessages.headline(AlertType.RAIN_NOW, 1.0))
        assertEquals("Pressure falling, storm possible", AlertMessages.headline(AlertType.PRESSURE_DROP, 4.0))
    }

    @Test
    fun `every alert has advice`() = assertTrue(AlertType.entries.all { AlertMessages.advice(it).isNotBlank() })

    @Test
    fun `hour text is a clock time with am or pm`() {
        val at4pm = LocalDateTime.of(2026, 9, 29, 16, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("4 PM", AlertMessages.hourText(at4pm, ZoneOffset.UTC))
        val at9am = LocalDateTime.of(2026, 9, 29, 9, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("9 AM", AlertMessages.hourText(at9am, ZoneOffset.UTC))
    }

    @Test
    fun `headline temperatures convert to fahrenheit for the notification`() =
        assertEquals("Feels like 104 °F", Temperature.convertText(AlertMessages.headline(AlertType.HEAT, 40.0), TemperatureUnit.FAHRENHEIT))
}

class ForecastMapperTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val sample = """
        {"utc_offset_seconds":19800,
         "current":{"temperature_2m":31.5,"apparent_temperature":37.7,"relative_humidity_2m":64,
           "weather_code":0,"wind_speed_10m":8.1},
         "hourly":{"time":["2026-09-29T10:00","2026-09-29T11:00","2026-09-29T12:00"],
           "precipitation_probability":[10,70,null],
           "precipitation":[0.0,1.2,null],
           "apparent_temperature":[36.0,37.5,null]}}
    """.trimIndent()

    private fun millis(hour: Int) = LocalDateTime.of(2026, 9, 29, hour, 0)
        .toEpochSecond(ZoneOffset.ofTotalSeconds(19800)) * 1000

    @Test
    fun `hours are mapped with times converted using the utc offset`() {
        val hours = json.decodeFromString<ForecastResponse>(sample).toHourlyForecast()
        assertEquals(listOf(millis(10), millis(11)), hours.map { it.time })
        assertEquals(70, hours[1].precipProbability)
        assertEquals(1.2, hours[1].precipitationMm, 0.0)
        assertEquals(37.5, hours[1].feelsLikeC, 0.0)
    }

    @Test
    fun `an hour without a feels like temperature is skipped`() =
        assertEquals(2, json.decodeFromString<ForecastResponse>(sample).toHourlyForecast().size)

    @Test
    fun `a missing probability or amount counts as zero`() {
        val text = sample.replace("[36.0,37.5,null]", "[36.0,37.5,35.0]")
        val third = json.decodeFromString<ForecastResponse>(text).toHourlyForecast()[2]
        assertEquals(0, third.precipProbability)
        assertEquals(0.0, third.precipitationMm, 0.0)
    }

    @Test
    fun `a response without an hourly block gives an empty forecast`() {
        val noHourly = """{"utc_offset_seconds":0,"current":{"temperature_2m":30.0,"apparent_temperature":33.0,
            "relative_humidity_2m":60,"weather_code":0,"wind_speed_10m":5.0}}"""
        assertTrue(json.decodeFromString<ForecastResponse>(noHourly).toHourlyForecast().isEmpty())
    }
}
