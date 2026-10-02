package com.aira.app.data.local

import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.Commute
import com.aira.app.domain.model.TaskRepeat
import com.aira.app.domain.model.TaskStatus
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.model.WeatherTask
import com.aira.app.domain.model.DiaryEvent
import com.aira.app.domain.model.DiaryEventType
import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Note
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    private val snapshot = Snapshot(
        id = 7, timestamp = 1000, latitude = 22.57, longitude = 88.36, temperature = 31.0,
        feelsLike = 37.0, humidity = 60, rainMm = 0.3, uvIndex = 6.5, windSpeed = 9.0,
        lux = 1500f, stepsDelta = 25, totalSteps = 9000, pressure = 1005f, inPocket = false,
        place = Place.OUTDOOR, movement = Movement.WALKING,
    )

    @Test
    fun `snapshot survives a round trip`() {
        assertEquals(snapshot, snapshot.toEntity().toDomain())
    }

    @Test
    fun `weather pending flag survives a round trip`() {
        assertEquals(false, snapshot.toEntity().toDomain().weatherPending)
        val pending = snapshot.copy(weatherPending = true)
        assertTrue(pending.toEntity().weatherPending)
        assertEquals(pending, pending.toEntity().toDomain())
    }

    @Test
    fun `entity stores derived flags and enum names`() {
        val entity = snapshot.toEntity()
        assertTrue(entity.isOutdoor)
        assertTrue(entity.isMoving)
        assertEquals("OUTDOOR", entity.place)
        assertEquals("WALKING", entity.movement)
    }

    @Test
    fun `still indoor snapshot is not outdoor or moving`() {
        val entity = snapshot.copy(place = Place.INDOOR, movement = Movement.STILL).toEntity()
        assertFalse(entity.isOutdoor)
        assertFalse(entity.isMoving)
    }

    @Test
    fun `missing sensor values stay null`() {
        val bare = snapshot.copy(lux = null, stepsDelta = null, totalSteps = null, pressure = null, inPocket = null)
        val back = bare.toEntity().toDomain()
        assertNull(back.lux)
        assertNull(back.pressure)
        assertNull(back.inPocket)
    }

    @Test
    fun `unknown enum text falls back to a safe default`() {
        val entity = snapshot.toEntity().copy(place = "SPACE", movement = "TELEPORT")
        val back = entity.toDomain()
        assertEquals(Place.UNKNOWN, back.place)
        assertEquals(Movement.STILL, back.movement)
    }

    @Test
    fun `alert round trips and an unknown type becomes null`() {
        val alert = WeatherAlert(4, AlertType.HEAT, 1000, "Feels like 40 °C", 40.0, dismissed = true)
        assertEquals(alert, alert.toEntity().toDomain())
        assertNull(alert.toEntity().copy(type = "METEOR").toDomain())
        val noValue = alert.copy(value = null)
        assertNull(noValue.toEntity().toDomain()!!.value)
    }

    @Test
    fun `task round trips with all its fields`() {
        val task = WeatherTask(
            id = 9, title = "Bring clothes inside", note = "n", trigger = AlertType.RAIN_SOON, dueTime = 5_000,
            isOutdoor = true, repeat = TaskRepeat.EVERY_TIME, status = TaskStatus.SNOOZED, snoozedUntil = 6_000,
            sourceAlertId = 3, createdAt = 100, completedAt = 200,
        )
        assertEquals(task, task.toEntity().toDomain())
    }

    @Test
    fun `a task without an alert type is stored as NONE and read back as null`() {
        val plain = WeatherTask(title = "Buy milk")
        assertEquals("NONE", plain.toEntity().triggerType)
        assertNull(plain.toEntity().toDomain().trigger)
    }

    @Test
    fun `unknown task text falls back to safe defaults`() {
        val entity = WeatherTask(title = "x").toEntity().copy(repeat = "SOMETIMES", status = "LOST", triggerType = "METEOR")
        val back = entity.toDomain()
        assertEquals(TaskRepeat.ONCE, back.repeat)
        assertEquals(TaskStatus.PENDING, back.status)
        assertNull(back.trigger)
    }

    @Test
    fun `event commute and note round trip`() {
        val event = DiaryEvent(1, 10, 20, DiaryEventType.RAIN, "Rain", 28.0)
        val commute = Commute(2, "Home", "College", 10, 20, true, 36.0)
        val note = Note(3, 1, "wet shoes", "content://photo/1", 30)
        assertEquals(event, event.toEntity().toDomain())
        assertEquals(commute, commute.toEntity().toDomain())
        assertEquals(note, note.toEntity().toDomain())
    }
}
