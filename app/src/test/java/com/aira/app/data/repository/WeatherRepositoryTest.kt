package com.aira.app.data.repository

import com.aira.app.data.remote.CurrentDto
import com.aira.app.data.remote.ForecastResponse
import com.aira.app.data.remote.HourlyDto
import com.aira.app.data.remote.HourlyResponse
import com.aira.app.data.remote.OpenMeteoApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {

    private class FakeApi(var fail: Boolean = false) : OpenMeteoApi {
        var calls = 0
        var lastLat = 0.0
        override suspend fun forecast(
            latitude: Double, longitude: Double, current: String,
            daily: String, hourly: String, timezone: String, forecastDays: Int,
        ): ForecastResponse {
            calls++
            lastLat = latitude
            if (fail) throw java.io.IOException("offline")
            return ForecastResponse(
                current = CurrentDto(
                    temperature = 30.0, apparentTemperature = 35.0, humidity = 60,
                    weatherCode = 0, windSpeed = 5.0,
                ),
            )
        }

        override suspend fun hourlyHistory(
            latitude: Double, longitude: Double, hourly: String,
            pastDays: Int, forecastDays: Int, timezone: String,
        ): HourlyResponse {
            calls++
            if (fail) throw java.io.IOException("offline")
            return HourlyResponse(
                utcOffsetSeconds = 0,
                hourly = HourlyDto(
                    time = listOf("2026-09-29T10:00"),
                    temperature = listOf(30.0), apparentTemperature = listOf(35.0), humidity = listOf(60),
                ),
            )
        }
    }

    private var now = 0L

    @Test
    fun `weather and forecast for the same place come from one request`() = runTest {
        val api = FakeApi()
        val repo = WeatherRepository(api) { now }
        repo.getWeather(22.57, 88.36)
        repo.getForecast(22.571, 88.361)
        assertEquals(1, api.calls)
    }

    @Test
    fun `a forecast failure is returned as a failure`() = runTest {
        assertTrue(WeatherRepository(FakeApi(fail = true)) { now }.getForecast(22.57, 88.36).isFailure)
    }

    @Test
    fun `recent hours are mapped and network errors become failures`() = runTest {
        val ok = WeatherRepository(FakeApi()) { now }.getRecentHours(22.57, 88.36)
        assertEquals(1, ok.getOrThrow().size)
        assertTrue(WeatherRepository(FakeApi(fail = true)) { now }.getRecentHours(22.57, 88.36).isFailure)
    }

    @Test
    fun `second call for same rounded location uses the cache`() = runTest {
        val api = FakeApi()
        val repo = WeatherRepository(api) { now }
        repo.getWeather(22.571, 88.361)
        repo.getWeather(22.574, 88.364)
        assertEquals(1, api.calls)
    }

    @Test
    fun `location is rounded to 2 decimals before calling the api`() = runTest {
        val api = FakeApi()
        WeatherRepository(api) { now }.getWeather(22.5749, 88.36)
        assertEquals(22.57, api.lastLat, 0.0)
    }

    @Test
    fun `cache expires after 60 minutes`() = runTest {
        val api = FakeApi()
        val repo = WeatherRepository(api) { now }
        repo.getWeather(22.57, 88.36)
        now = WeatherRepository.CACHE_MILLIS
        repo.getWeather(22.57, 88.36)
        assertEquals(2, api.calls)
    }

    @Test
    fun `switching between places keeps each one cached`() = runTest {
        val api = FakeApi()
        val repo = WeatherRepository(api) { now }
        repo.getWeather(22.57, 88.36) // live location
        repo.getWeather(1.29, 103.85) // a saved place
        repo.getWeather(22.57, 88.36) // back to live: from the cache
        repo.getWeather(1.29, 103.85) // and back again: from the cache
        assertEquals(2, api.calls)
    }

    @Test
    fun `only the most recent places are kept`() = runTest {
        val api = FakeApi()
        val repo = WeatherRepository(api) { now }
        (0..WeatherRepository.MAX_CACHED).forEach { repo.getWeather(it.toDouble(), 0.0) } // one more than fits
        repo.getWeather(0.0, 0.0) // the oldest was dropped, so it is loaded again
        assertEquals(WeatherRepository.MAX_CACHED + 2, api.calls)
    }

    @Test
    fun `different location is not served from cache`() = runTest {
        val api = FakeApi()
        val repo = WeatherRepository(api) { now }
        repo.getWeather(22.57, 88.36)
        repo.getWeather(28.61, 77.20)
        assertEquals(2, api.calls)
    }

    @Test
    fun `network error is returned as failure`() = runTest {
        val repo = WeatherRepository(FakeApi(fail = true)) { now }
        assertTrue(repo.getWeather(22.57, 88.36).isFailure)
    }
}
