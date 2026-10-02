package com.aira.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Runs every DAO against an in-memory database (nothing is written to the phone). */
class DatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() = db.close()

    private fun snapshot(
        time: Long,
        outdoor: Boolean = false,
        rain: Double = 0.0,
    ) = SnapshotEntity(
        timestamp = time, latitude = 22.57, longitude = 88.36, temperature = 30.0, feelsLike = 34.0,
        humidity = 60, rainMm = rain, uvIndex = 5.0, windSpeed = 8.0, lux = null, stepsDelta = null,
        totalSteps = null, pressure = null, inPocket = null, isOutdoor = outdoor, isMoving = false,
        place = if (outdoor) "OUTDOOR" else "INDOOR", movement = "STILL",
    )

    private fun event(start: Long) = DiaryEventEntity(
        startTime = start, endTime = start + 1000, type = "SUN", summary = "Sun", avgTemperature = 33.0,
    )

    // ---- snapshots ----

    @Test
    fun snapshots_queryByRange_isSortedAndInclusive() = runTest {
        val dao = db.snapshotDao()
        listOf(300L, 100L, 200L, 500L).forEach { dao.insert(snapshot(it)) }
        assertEquals(listOf(100L, 200L, 300L), dao.getBetween(100, 300).map { it.timestamp })
        assertEquals(3, dao.observeBetween(100, 300).first().size)
    }

    @Test
    fun snapshots_latest() = runTest {
        val dao = db.snapshotDao()
        assertNull(dao.getLatest())
        dao.insert(snapshot(100))
        dao.insert(snapshot(900))
        dao.insert(snapshot(400))
        assertEquals(900L, dao.getLatest()?.timestamp)
        assertEquals(900L, dao.observeLatest().first()?.timestamp)
    }

    @Test
    fun snapshots_nullableSensorsAreStored() = runTest {
        val dao = db.snapshotDao()
        dao.insert(snapshot(100).copy(lux = 1200f, inPocket = true, totalSteps = 5000))
        dao.insert(snapshot(200))
        val (first, second) = dao.getBetween(0, 1000)
        assertEquals(1200f, first.lux)
        assertEquals(true, first.inPocket)
        assertEquals(5000L, first.totalSteps)
        assertNull(second.lux)
        assertNull(second.inPocket)
    }

    @Test
    fun snapshots_aggregates() = runTest {
        val dao = db.snapshotDao()
        dao.insert(snapshot(100, outdoor = true, rain = 1.0))
        dao.insert(snapshot(200, outdoor = true, rain = 0.1)) // too little rain
        dao.insert(snapshot(300, outdoor = false, rain = 2.0)) // indoors
        dao.insert(snapshot(9000, outdoor = true, rain = 5.0)) // outside the range
        assertEquals(3, dao.countBetween(0, 1000))
        assertEquals(2, dao.countOutdoorBetween(0, 1000))
        assertEquals(1, dao.countOutdoorRainBetween(0, 1000, 0.2))
    }

    @Test
    fun snapshots_pendingWeatherCanBeFilled() = runTest {
        val dao = db.snapshotDao()
        val pendingId = dao.insert(snapshot(100).copy(weatherPending = true, temperature = 0.0))
        dao.insert(snapshot(200))
        assertEquals(listOf(pendingId), dao.getPendingWeather().map { it.id })

        dao.fillWeather(pendingId, 31.0, 36.0, 65, 1.5, 7.0, 9.0)

        assertTrue(dao.getPendingWeather().isEmpty())
        val filled = dao.getBetween(0, 1000).first { it.id == pendingId }
        assertEquals(31.0, filled.temperature, 0.0)
        assertEquals(1.5, filled.rainMm, 0.0)
        assertEquals(false, filled.weatherPending)
    }

    @Test
    fun snapshots_rainCountSkipsPendingWeather() = runTest {
        val dao = db.snapshotDao()
        dao.insert(snapshot(100, outdoor = true, rain = 1.0))
        dao.insert(snapshot(200, outdoor = true, rain = 1.0).copy(weatherPending = true))
        assertEquals(1, dao.countOutdoorRainBetween(0, 1000, 0.2))
    }

    @Test
    fun snapshots_deleteAll() = runTest {
        val dao = db.snapshotDao()
        dao.insert(snapshot(100))
        dao.deleteAll()
        assertTrue(dao.getBetween(0, 1000).isEmpty())
    }

    // ---- diary events ---- (alerts and tasks follow further down)

    @Test
    fun events_forOneDay() = runTest {
        val dao = db.diaryEventDao()
        dao.insertAll(listOf(event(100), event(200), event(5000)))
        assertEquals(listOf(100L, 200L), dao.observeBetween(0, 1000).first().map { it.startTime })
    }

    @Test
    fun events_deleteBetweenKeepsOtherDays() = runTest {
        val dao = db.diaryEventDao()
        dao.insertAll(listOf(event(100), event(5000)))
        dao.deleteBetween(0, 1000)
        assertEquals(listOf(5000L), dao.observeBetween(0, 10_000).first().map { it.startTime })
    }

    // ---- commutes ----

    @Test
    fun commutes_rangeAndRainCounts() = runTest {
        val dao = db.commuteDao()
        fun commute(start: Long, rain: Boolean) = CommuteEntity(
            fromPlace = "Home", toPlace = "College", startTime = start, endTime = start + 500,
            hadRain = rain, maxFeelsLike = 35.0,
        )
        dao.insertAll(listOf(commute(100, true), commute(200, false), commute(9000, true)))
        assertEquals(2, dao.observeBetween(0, 1000).first().size)
        assertEquals(2, dao.countBetween(0, 1000))
        assertEquals(1, dao.countWithRainBetween(0, 1000))
    }

    // ---- alerts ----

    private fun alert(time: Long, type: String = "HEAT", dismissed: Boolean = false) =
        AlertEntity(type = type, time = time, message = "msg $time", value = 40.0, dismissed = dismissed)

    @Test
    fun alerts_insertAndGet() = runTest {
        val id = db.alertDao().insert(alert(100))
        assertEquals("msg 100", db.alertDao().get(id)?.message)
        assertNull(db.alertDao().get(999))
    }

    @Test
    fun alerts_activeAreRecentAndNotDismissed_newestFirst() = runTest {
        val dao = db.alertDao()
        dao.insert(alert(50)) // too old
        dao.insert(alert(200))
        dao.insert(alert(300))
        val dismissed = dao.insert(alert(400))
        dao.dismiss(dismissed)
        assertEquals(listOf(300L, 200L), dao.observeActive(since = 100).first().map { it.time })
    }

    @Test
    fun alerts_historyKeepsEverythingNewestFirst() = runTest {
        val dao = db.alertDao()
        dao.insert(alert(100))
        dao.insert(alert(300, dismissed = true))
        dao.insert(alert(200))
        assertEquals(listOf(300L, 200L, 100L), dao.observeHistory().first().map { it.time })
    }

    @Test
    fun alerts_forOneDay() = runTest {
        val dao = db.alertDao()
        listOf(100L, 200L, 5000L).forEach { dao.insert(alert(it)) }
        assertEquals(listOf(100L, 200L), dao.observeBetween(0, 1000).first().map { it.time })
    }

    @Test
    fun alerts_deleteAll() = runTest {
        val dao = db.alertDao()
        dao.insert(alert(100))
        dao.deleteAll()
        assertTrue(dao.observeHistory().first().isEmpty())
    }

    // ---- tasks ----

    private fun task(
        title: String,
        trigger: String = "NONE",
        status: String = "PENDING",
        createdAt: Long = 1,
        completedAt: Long? = null,
    ) = TaskEntity(
        title = title, note = "", triggerType = trigger, dueTime = null, isOutdoor = false, repeat = "ONCE",
        status = status, snoozedUntil = null, sourceAlertId = null, createdAt = createdAt, completedAt = completedAt,
    )

    @Test
    fun tasks_insertGetUpdateDelete() = runTest {
        val dao = db.taskDao()
        val id = dao.insert(task("Buy milk"))
        assertEquals("Buy milk", dao.get(id)?.title)
        dao.update(dao.get(id)!!.copy(title = "Buy bread", isOutdoor = true))
        assertEquals("Buy bread", dao.get(id)?.title)
        assertEquals(true, dao.get(id)?.isOutdoor)
        dao.delete(id)
        assertNull(dao.get(id))
    }

    @Test
    fun tasks_pendingIncludesSnoozedButNotDone() = runTest {
        val dao = db.taskDao()
        dao.insert(task("pending", createdAt = 1))
        dao.insert(task("snoozed", status = "SNOOZED", createdAt = 2))
        dao.insert(task("done", status = "DONE", createdAt = 3, completedAt = 10))
        assertEquals(listOf("pending", "snoozed"), dao.observePending().first().map { it.title })
    }

    @Test
    fun tasks_doneMostRecentFirst() = runTest {
        val dao = db.taskDao()
        dao.insert(task("first", status = "DONE", completedAt = 100))
        dao.insert(task("second", status = "DONE", completedAt = 300))
        dao.insert(task("open"))
        assertEquals(listOf("second", "first"), dao.observeDone().first().map { it.title })
    }

    @Test
    fun tasks_completedInARange() = runTest {
        val dao = db.taskDao()
        dao.insert(task("in", status = "DONE", completedAt = 500))
        dao.insert(task("out", status = "DONE", completedAt = 9000))
        dao.insert(task("open"))
        assertEquals(listOf("in"), dao.observeCompletedBetween(0, 1000).first().map { it.title })
    }

    @Test
    fun tasks_getAllAndDeleteAll() = runTest {
        val dao = db.taskDao()
        dao.insert(task("a"))
        dao.insert(task("b", status = "DONE", completedAt = 1))
        assertEquals(2, dao.getAll().size)
        dao.deleteAll()
        assertTrue(dao.getAll().isEmpty())
    }

    // ---- notes ----

    private fun note(eventId: Long, text: String, at: Long = 1) =
        NoteEntity(eventId = eventId, text = text, photoUri = null, createdAt = at)

    @Test
    fun notes_forEvent() = runTest {
        val eventId = db.diaryEventDao().insert(event(100))
        val otherId = db.diaryEventDao().insert(event(200))
        db.noteDao().insert(note(eventId, "first", 1))
        db.noteDao().insert(note(eventId, "second", 2))
        db.noteDao().insert(note(otherId, "other"))
        assertEquals(listOf("first", "second"), db.noteDao().observeForEvent(eventId).first().map { it.text })
    }

    @Test
    fun notes_areDeletedWhenTheirEventIsDeleted() = runTest {
        val eventId = db.diaryEventDao().insert(event(100))
        db.noteDao().insert(note(eventId, "will be removed"))
        db.diaryEventDao().deleteAll()
        assertTrue(db.noteDao().observeForEvent(eventId).first().isEmpty())
    }
}
