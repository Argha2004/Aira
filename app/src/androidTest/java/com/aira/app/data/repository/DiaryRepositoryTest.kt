package com.aira.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aira.app.data.local.AppDatabase
import com.aira.app.data.settings.SettingsStore
import com.aira.app.domain.engine.DayRange
import com.aira.app.domain.model.DiaryEventType
import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Note
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Rebuilding a day is done every 30 minutes by the background job, so it must never lose what the user
 * wrote: notes belong to timeline events, and an event that is still the same keeps its id.
 */
class DiaryRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var snapshots: SnapshotRepository
    private lateinit var diary: DiaryRepository

    private val zone = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 1, 10)
    private val range = DayRange.of(day, zone)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        snapshots = SnapshotRepository(db.snapshotDao())
        diary = DiaryRepository(
            db, db.diaryEventDao(), db.commuteDao(), db.noteDao(), snapshots, SettingsStore(context),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun indoorAt(hour: Int, minute: Int = 0, day: LocalDate = this.day) = Snapshot(
        timestamp = day.atTime(hour, minute).toInstant(zone).toEpochMilli(),
        latitude = 22.57, longitude = 88.36, temperature = 30.0, feelsLike = 32.0, humidity = 60,
        rainMm = 0.0, uvIndex = 3.0, windSpeed = 5.0, place = Place.INDOOR, movement = Movement.STILL,
    )

    private suspend fun events(day: LocalDate = this.day) =
        DayRange.of(day, zone).let { diary.observeEvents(it.first, it.last).first() }

    @Test
    fun rebuildDay_turnsSnapshotsIntoEvents() = runTest {
        snapshots.save(indoorAt(8))
        snapshots.save(indoorAt(8, 30))
        snapshots.save(indoorAt(9))
        diary.rebuildDay(day, zone)
        val event = events().single()
        assertEquals(DiaryEventType.INDOOR, event.type)
        assertEquals(indoorAt(8).timestamp, event.startTime)
    }

    @Test
    fun rebuildDay_keepsTheNoteWhenTheEventGrows() = runTest {
        snapshots.save(indoorAt(8))
        snapshots.save(indoorAt(8, 30))
        diary.rebuildDay(day, zone)
        val before = events().single()
        diary.addNote(Note(eventId = before.id, text = "Stayed in the library", createdAt = 1))

        // Another snapshot arrives: the same indoor stretch gets longer.
        snapshots.save(indoorAt(9))
        diary.rebuildDay(day, zone)

        val after = events().single()
        assertEquals(before.id, after.id)
        assertTrue(after.endTime > before.endTime)
        assertEquals(listOf("Stayed in the library"), diary.observeNotes(after.id).first().map { it.text })
    }

    @Test
    fun rebuildDay_twiceInARowChangesNothing() = runTest {
        snapshots.save(indoorAt(8))
        diary.rebuildDay(day, zone)
        val first = events()
        diary.rebuildDay(day, zone)
        assertEquals(first, events())
    }

    @Test
    fun rebuildDay_removesEventsWhoseSnapshotsAreGone() = runTest {
        snapshots.save(indoorAt(8))
        diary.rebuildDay(day, zone)
        snapshots.deleteAll()
        diary.rebuildDay(day, zone)
        assertTrue(events().isEmpty())
    }

    @Test
    fun rebuildDay_doesNotTouchOtherDays() = runTest {
        val otherDay = day.plusDays(1)
        snapshots.save(indoorAt(8, day = otherDay))
        diary.rebuildDay(otherDay, zone)
        val otherBefore = events(otherDay)

        snapshots.save(indoorAt(8))
        diary.rebuildDay(day, zone)

        assertEquals(otherBefore, events(otherDay))
        assertEquals(1, events().size)
    }

    @Test
    fun dailyStats_followTheSnapshotsOfThatDay() = runTest {
        snapshots.save(indoorAt(8))
        val stats = diary.observeDailyStats(day, zone).first()
        assertEquals(0, stats.sunMinutes)
        assertEquals(0, stats.outdoorMinutes)
    }
}
