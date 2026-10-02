package com.aira.app.domain.engine

import com.aira.app.domain.model.HourlyForecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForecastLookupTest {
    private val hour = 60 * 60 * 1000L
    private val start = 10 * hour
    private val forecast = listOf(
        HourlyForecast(time = start, precipProbability = 10, precipitationMm = 0.0, feelsLikeC = 30.0),
        HourlyForecast(time = start + hour, precipProbability = 70, precipitationMm = 1.0, feelsLikeC = 29.0),
        HourlyForecast(time = start + 2 * hour, precipProbability = 20, precipitationMm = 0.0, feelsLikeC = 28.0),
    )

    @Test
    fun `the hour that contains now is found, and an hour starts exactly at its time`() {
        assertEquals(10, ForecastLookup.currentHour(forecast, start + 30 * 60 * 1000L)?.precipProbability)
        assertEquals(70, ForecastLookup.currentHour(forecast, start + hour)?.precipProbability)
    }

    @Test
    fun `before the forecast or after its last hour there is no current hour`() {
        assertNull(ForecastLookup.currentHour(forecast, start - 1))
        assertNull(ForecastLookup.currentHour(forecast, start + 3 * hour))
        assertNull(ForecastLookup.currentHour(emptyList(), start))
    }

    @Test
    fun `next hours start with the current hour and are limited to count`() {
        val next = ForecastLookup.nextHours(forecast.reversed(), start + hour + 5, count = 2)
        assertEquals(listOf(start + hour, start + 2 * hour), next.map { it.time })
    }

    @Test
    fun `the next change is the first rainy or very hot hour`() {
        val hot = forecast + HourlyForecast(time = start + 3 * hour, precipProbability = 0, precipitationMm = 0.0, feelsLikeC = 40.0)
        assertEquals(ForecastChange(start + hour, ChangeKind.RAIN), ForecastLookup.nextChange(hot, start))
        assertEquals(ForecastChange(start + 3 * hour, ChangeKind.HEAT), ForecastLookup.nextChange(hot, start + 2 * hour))
    }

    @Test
    fun `no change when nothing crosses a limit or it is too far ahead`() {
        assertNull(ForecastLookup.nextChange(forecast, start + 2 * hour))
        assertNull(ForecastLookup.nextChange(forecast, start, hoursAhead = 1))
    }

    @Test
    fun `past hours are left out`() =
        assertEquals(emptyList<HourlyForecast>(), ForecastLookup.nextHours(forecast, start + 3 * hour, count = 5))
}
