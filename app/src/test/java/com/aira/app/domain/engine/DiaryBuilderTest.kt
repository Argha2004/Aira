package com.aira.app.domain.engine

import com.aira.app.domain.model.DiaryEventType
import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN = 60_000L

/** A snapshot at [minute] minutes after time zero. */
internal fun snap(
    minute: Int,
    place: Place = Place.INDOOR,
    movement: Movement = Movement.STILL,
    lux: Float? = 200f,
    temp: Double = 30.0,
    feels: Double = 30.0,
    rain: Double = 0.0,
    uv: Double = 5.0,
    steps: Int? = 0,
    pressure: Float? = null,
    pending: Boolean = false,
    lat: Double = 22.57,
    lon: Double = 88.36,
    atMillis: Long? = null,
) = Snapshot(
    timestamp = atMillis ?: (minute * MIN), latitude = lat, longitude = lon, temperature = temp, feelsLike = feels,
    humidity = 60, rainMm = rain, uvIndex = uv, windSpeed = 8.0, lux = lux, stepsDelta = steps,
    totalSteps = null, pressure = pressure, inPocket = false, place = place, movement = movement,
    weatherPending = pending,
)

class DiaryBuilderTest {

    private fun sun(minute: Int) = snap(minute, Place.OUTDOOR, lux = 40_000f, temp = 34.0, feels = 34.0)

    @Test
    fun `no snapshots give no events`() = assertTrue(DiaryBuilder.build(emptyList()).isEmpty())

    @Test
    fun `neighbouring indoor snapshots merge into one event`() {
        val events = DiaryBuilder.build(listOf(snap(0), snap(30), snap(60)))
        assertEquals(1, events.size)
        assertEquals(DiaryEventType.INDOOR, events[0].type)
        assertEquals(0L, events[0].startTime)
        assertEquals(90 * MIN, events[0].endTime) // the last snapshot counts 30 minutes
        assertEquals("Indoors, 1 h 30 min", events[0].summary)
    }

    @Test
    fun `strong sun outdoors is a sun event with duration and temperature`() {
        val events = DiaryBuilder.build(listOf(sun(0), sun(30)))
        assertEquals(1, events.size)
        assertEquals(DiaryEventType.SUN, events[0].type)
        assertEquals("Outdoors in strong sun, 1 h, 34 °C", events[0].summary)
        assertEquals(34.0, events[0].avgTemperature, 0.0)
    }

    @Test
    fun `rain beats sun`() {
        val events = DiaryBuilder.build(listOf(snap(0, Place.OUTDOOR, lux = 40_000f, rain = 1.0)))
        assertEquals(DiaryEventType.RAIN, events.single().type)
        assertEquals("Rain while outdoors, 30 min", events.single().summary)
    }

    @Test
    fun `walking in rain counts even when the place is unknown`() {
        val events = DiaryBuilder.build(listOf(snap(0, Place.UNKNOWN, Movement.WALKING, rain = 1.0)))
        assertEquals(DiaryEventType.RAIN, events.single().type)
    }

    @Test
    fun `heat outdoors without strong sun is a heat event`() {
        val events = DiaryBuilder.build(listOf(snap(0, Place.OUTDOOR, lux = 5000f, feels = 36.0)))
        assertEquals(DiaryEventType.HEAT, events.single().type)
        assertEquals("Outdoors in the heat, 30 min, feels like 36 °C", events.single().summary)
    }

    @Test
    fun `plain outdoor is an outdoor event`() {
        val events = DiaryBuilder.build(listOf(snap(0, Place.OUTDOOR, lux = 5000f, feels = 28.0)))
        assertEquals(DiaryEventType.OUTDOOR, events.single().type)
        assertEquals("Outdoors, 30 min", events.single().summary)
    }

    @Test
    fun `unknown place with nothing special gives no event`() =
        assertTrue(DiaryBuilder.build(listOf(snap(0, Place.UNKNOWN))).isEmpty())

    @Test
    fun `different states become separate events in time order`() {
        val events = DiaryBuilder.build(listOf(snap(0), sun(30), snap(60)))
        assertEquals(
            listOf(DiaryEventType.INDOOR, DiaryEventType.SUN, DiaryEventType.INDOOR),
            events.map { it.type },
        )
        assertEquals(listOf(0L, 30 * MIN, 60 * MIN), events.map { it.startTime })
    }

    @Test
    fun `a state that is interrupted by an unknown snapshot is split`() {
        val events = DiaryBuilder.build(listOf(snap(0), snap(30, Place.UNKNOWN, Movement.VEHICLE), snap(60)))
        assertEquals(2, events.size)
    }

    @Test
    fun `a gap of more than 2 hours splits a run`() {
        val events = DiaryBuilder.build(listOf(snap(0), snap(30), snap(300), snap(330)))
        assertEquals(2, events.size)
        assertEquals(30 * MIN + 60 * MIN, events[0].endTime) // gap is capped at 60 minutes
    }

    @Test
    fun `snapshots in any order give the same events`() {
        val ordered = DiaryBuilder.build(listOf(snap(0), sun(30), snap(60)))
        val shuffled = DiaryBuilder.build(listOf(snap(60), snap(0), sun(30)))
        assertEquals(ordered, shuffled)
    }

    @Test
    fun `average temperature is the mean of the run`() {
        val events = DiaryBuilder.build(listOf(snap(0, temp = 28.0), snap(30, temp = 32.0)))
        assertEquals(30.0, events.single().avgTemperature, 0.0)
    }

    // ---- snapshots still waiting for weather ----

    @Test
    fun `pending weather never creates rain or heat events`() {
        val events = DiaryBuilder.build(
            listOf(snap(0, Place.OUTDOOR, lux = 5000f, feels = 0.0, rain = 5.0, temp = 0.0, pending = true)),
        )
        assertEquals(DiaryEventType.OUTDOOR, events.single().type)
        assertEquals(0.0, events.single().avgTemperature, 0.0)
    }

    @Test
    fun `sun event without weather has no temperature in the summary`() {
        val events = DiaryBuilder.build(listOf(snap(0, Place.OUTDOOR, lux = 40_000f, temp = 0.0, pending = true)))
        assertEquals("Outdoors in strong sun, 30 min", events.single().summary)
    }

    // ---- pressure ----

    @Test
    fun `falling pressure gives a pressure drop event`() {
        val events = DiaryBuilder.build(
            listOf(
                snap(0, pressure = 1012f),
                snap(60, pressure = 1010f),
                snap(120, pressure = 1008.5f),
            ),
        )
        val drop = events.single { it.type == DiaryEventType.PRESSURE_DROP }
        assertEquals(120 * MIN, drop.startTime)
        assertEquals("Pressure falling: 3.5 hPa in 3 h", drop.summary)
    }

    @Test
    fun `steady pressure gives no pressure event`() {
        val events = DiaryBuilder.build(listOf(snap(0, pressure = 1010f), snap(60, pressure = 1009.5f)))
        assertTrue(events.none { it.type == DiaryEventType.PRESSURE_DROP })
    }

    @Test
    fun `phone without barometer gives no pressure event`() {
        val events = DiaryBuilder.build(listOf(snap(0, pressure = null), snap(120, pressure = null)))
        assertTrue(events.none { it.type == DiaryEventType.PRESSURE_DROP })
    }

    @Test
    fun `state of an unknown walking snapshot outdoors is outdoor`() {
        assertNull(DiaryBuilder.stateOf(snap(0, Place.UNKNOWN)))
        assertEquals(DiaryEventType.OUTDOOR, DiaryBuilder.stateOf(snap(0, Place.OUTDOOR, lux = 2000f, feels = 25.0)))
    }
}
