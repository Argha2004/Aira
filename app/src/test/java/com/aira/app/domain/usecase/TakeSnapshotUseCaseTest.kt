package com.aira.app.domain.usecase

import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot
import com.aira.app.domain.model.WeatherNow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TakeSnapshotUseCaseTest {

    private class FakeSensors(
        var lux: Float? = 20_000f,
        var inPocket: Boolean? = false,
        var steps: Long? = 1000,
        var pressure: Float? = 1010f,
    ) : SensorSource {
        override suspend fun readLux() = lux
        override suspend fun readInPocket() = inPocket
        override suspend fun readStepCounter() = steps
        override suspend fun readPressure() = pressure
    }

    private class FakeLocation(var point: GeoPoint? = GeoPoint(22.57, 88.36)) : LocationSource {
        override suspend fun getCurrentLocation() = point
    }

    private class FakeWeather(var result: Result<WeatherNow> = Result.success(weather())) : WeatherSource {
        override suspend fun getWeather(latitude: Double, longitude: Double) = result
    }

    private class FakeStore(var latest: Snapshot? = null) : SnapshotStore {
        val saved = mutableListOf<Snapshot>()
        override suspend fun getBetween(from: Long, to: Long) = saved.filter { it.timestamp in from..to }
        override suspend fun getLatest() = latest
        override suspend fun save(snapshot: Snapshot): Long {
            saved += snapshot
            return saved.size.toLong()
        }
    }

    private val sensors = FakeSensors()
    private val location = FakeLocation()
    private val weather = FakeWeather()
    private val store = FakeStore()
    private var now = 10_000_000L

    private fun useCase() =
        TakeSnapshotUseCase(sensors, location, weather, store, java.time.ZoneOffset.UTC) { now }

    private fun previous(
        place: Place = Place.INDOOR,
        totalSteps: Long? = 1000,
        at: Long = now - 30 * 60 * 1000,
        lat: Double = 22.57,
        lon: Double = 88.36,
    ) = Snapshot(
        id = 1, timestamp = at, latitude = lat, longitude = lon, temperature = 30.0, feelsLike = 33.0,
        humidity = 60, rainMm = 0.0, uvIndex = 3.0, windSpeed = 5.0, totalSteps = totalSteps,
        place = place, movement = Movement.STILL,
    )

    @Test
    fun `bright daylight is saved as outdoor with weather and sensor values`() = runTest {
        val snapshot = useCase()().getOrThrow()
        assertEquals(Place.OUTDOOR, snapshot.place)
        assertEquals(31.5, snapshot.temperature, 0.0)
        assertEquals(20_000f, snapshot.lux)
        assertEquals(1010f, snapshot.pressure)
        assertEquals(now, snapshot.timestamp)
        assertEquals(1, store.saved.size)
        assertEquals(1L, snapshot.id)
    }

    @Test
    fun `dark room is saved as indoor`() = runTest {
        sensors.lux = 80f
        assertEquals(Place.INDOOR, useCase()().getOrThrow().place)
    }

    @Test
    fun `steps since the previous snapshot are computed`() = runTest {
        store.latest = previous(totalSteps = 1000)
        sensors.steps = 1100
        val snapshot = useCase()().getOrThrow()
        assertEquals(100, snapshot.stepsDelta)
        assertEquals(Movement.WALKING, snapshot.movement)
        assertEquals(1100L, snapshot.totalSteps)
    }

    @Test
    fun `when the previous snapshot has no step total the last known total is used`() = runTest {
        // Two snapshots ago the total was 1000. The snapshot after it missed the step read (null total).
        store.saved += previous(totalSteps = 1000, at = now - 60 * 60 * 1000)
        store.latest = previous(totalSteps = null, at = now - 30 * 60 * 1000)
        sensors.steps = 1080
        assertEquals(80, useCase()().getOrThrow().stepsDelta)
    }

    @Test
    fun `without any known step total there is no delta`() = runTest {
        store.latest = previous(totalSteps = null)
        sensors.steps = 1080
        assertNull(useCase()().getOrThrow().stepsDelta)
    }

    @Test
    fun `step counter reset after reboot uses the new total`() = runTest {
        store.latest = previous(totalSteps = 90_000)
        sensors.steps = 30
        assertEquals(30, useCase()().getOrThrow().stepsDelta)
    }

    @Test
    fun `first snapshot has no steps delta`() = runTest {
        assertNull(useCase()().getOrThrow().stepsDelta)
    }

    @Test
    fun `moving more than 1 km with few steps is a vehicle`() = runTest {
        store.latest = previous(lat = 22.50) // about 7.8 km away
        sensors.steps = 1005
        assertEquals(Movement.VEHICLE, useCase()().getOrThrow().movement)
    }

    @Test
    fun `an old previous snapshot is ignored`() = runTest {
        store.latest = previous(at = now - 5 * 60 * 60 * 1000, lat = 10.0, totalSteps = 0)
        sensors.steps = 5000
        val snapshot = useCase()().getOrThrow()
        assertNull(snapshot.stepsDelta)
        assertEquals(Movement.STILL, snapshot.movement)
    }

    @Test
    fun `unclear light keeps the previous place`() = runTest {
        store.latest = previous(place = Place.INDOOR)
        sensors.lux = 500f
        assertEquals(Place.INDOOR, useCase()().getOrThrow().place)
    }

    @Test
    fun `night ignores light`() = runTest {
        weather.result = Result.success(weather(isDay = false))
        store.latest = previous(place = Place.INDOOR)
        assertEquals(Place.INDOOR, useCase()().getOrThrow().place)
    }

    @Test
    fun `phone in pocket does not count as outdoor`() = runTest {
        sensors.inPocket = true
        assertEquals(Place.UNKNOWN, useCase()().getOrThrow().place)
    }

    @Test
    fun `all sensors missing still saves a snapshot`() = runTest {
        sensors.lux = null
        sensors.inPocket = null
        sensors.steps = null
        sensors.pressure = null
        val snapshot = useCase()().getOrThrow()
        assertEquals(Place.UNKNOWN, snapshot.place)
        assertEquals(Movement.STILL, snapshot.movement)
        assertNull(snapshot.lux)
        assertNull(snapshot.totalSteps)
        assertNull(snapshot.pressure)
        assertEquals(1, store.saved.size)
    }

    @Test
    fun `no location fails and saves nothing`() = runTest {
        location.point = null
        val result = useCase()()
        assertTrue(result.exceptionOrNull() is NoLocationException)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun `offline snapshot is saved with weather pending`() = runTest {
        weather.result = Result.failure(java.io.IOException("offline"))
        now = at(hour = 12)
        val snapshot = useCase()().getOrThrow()
        assertTrue(snapshot.weatherPending)
        assertEquals(0.0, snapshot.temperature, 0.0)
        assertEquals(20_000f, snapshot.lux)
        assertEquals(1, store.saved.size)
    }

    @Test
    fun `offline snapshot guesses daylight from the clock`() = runTest {
        weather.result = Result.failure(java.io.IOException("offline"))
        now = at(hour = 12)
        assertEquals(Place.OUTDOOR, useCase()().getOrThrow().place)
    }

    @Test
    fun `offline snapshot at night ignores light`() = runTest {
        weather.result = Result.failure(java.io.IOException("offline"))
        now = at(hour = 2)
        assertEquals(Place.UNKNOWN, useCase()().getOrThrow().place)
    }

    @Test
    fun `snapshot with weather is not pending`() = runTest {
        assertEquals(false, useCase()().getOrThrow().weatherPending)
    }

    /** Epoch millis for the given hour on a fixed day, in UTC (the tests use a UTC zone). */
    private fun at(hour: Int) = java.time.LocalDateTime.of(2026, 9, 29, hour, 0)
        .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()

    private companion object {
        fun weather(isDay: Boolean = true) = WeatherNow(
            tempC = 31.5, feelsLikeC = 37.0, humidity = 60, rainMm = 0.0, uvIndex = 6.0,
            windKmh = 8.0, weatherCode = 0, isDay = isDay, sunrise = null, sunset = null,
        )
    }
}
