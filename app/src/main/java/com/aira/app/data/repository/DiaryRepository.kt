package com.aira.app.data.repository

import androidx.room.withTransaction
import com.aira.app.data.local.AppDatabase
import com.aira.app.data.local.CommuteDao
import com.aira.app.data.local.DiaryEventDao
import com.aira.app.data.local.NoteDao
import com.aira.app.data.local.toDomain
import com.aira.app.data.local.toEntity
import com.aira.app.data.settings.SettingsStore
import com.aira.app.domain.engine.CommuteDetector
import com.aira.app.domain.engine.DailyStats
import com.aira.app.domain.engine.DailyStatsCalculator
import com.aira.app.domain.engine.DayRange
import com.aira.app.domain.engine.DiaryBuilder
import com.aira.app.domain.engine.DiaryMerge
import com.aira.app.domain.model.Commute
import com.aira.app.domain.model.DiaryEvent
import com.aira.app.domain.model.Note
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Diary events, commutes and notes. Times are epoch millis. */
@Singleton
class DiaryRepository @Inject constructor(
    private val database: AppDatabase,
    private val eventDao: DiaryEventDao,
    private val commuteDao: CommuteDao,
    private val noteDao: NoteDao,
    private val snapshotRepository: SnapshotRepository,
    private val settings: SettingsStore,
) {
    suspend fun saveEvents(events: List<DiaryEvent>) = eventDao.insertAll(events.map { it.toEntity() })

    /**
     * Rebuilds the events and commutes of [date] from that day's snapshots. Events that are unchanged
     * keep their id, so notes attached to them stay; changed ones are updated, new ones added, gone
     * ones removed. Commutes are found only when both Home and College are set.
     */
    suspend fun rebuildDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()) {
        val range = DayRange.of(date, zone)
        val snapshots = snapshotRepository.getBetween(range.first, range.last)
        val fresh = DiaryBuilder.build(snapshots)
        val home = settings.home.first()
        val college = settings.college.first()
        val commutes = if (home != null && college != null) CommuteDetector.detect(snapshots, home, college) else emptyList()
        database.withTransaction {
            commuteDao.deleteBetween(range.first, range.last)
            if (commutes.isNotEmpty()) commuteDao.insertAll(commutes.map { it.toEntity() })
            val existing = eventDao.getBetween(range.first, range.last).map { it.toDomain() }
            val plan = DiaryMerge.plan(existing, fresh)
            if (plan.deleteIds.isNotEmpty()) eventDao.deleteByIds(plan.deleteIds)
            plan.update.forEach { eventDao.update(it.toEntity()) }
            if (plan.insert.isNotEmpty()) eventDao.insertAll(plan.insert.map { it.toEntity() })
        }
    }

    /** Events starting between [from] and [to], e.g. the start and end of one day. */
    fun observeEvents(from: Long, to: Long): Flow<List<DiaryEvent>> =
        eventDao.observeBetween(from, to).map { list -> list.map { it.toDomain() } }

    /** Sun, heat, rain, UV, outdoor time and steps for one day, updated as snapshots arrive. */
    fun observeDailyStats(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<DailyStats> {
        val range = DayRange.of(date, zone)
        return snapshotRepository.observeBetween(range.first, range.last).map { DailyStatsCalculator.calculate(it) }
    }

    suspend fun saveCommutes(commutes: List<Commute>) = commuteDao.insertAll(commutes.map { it.toEntity() })

    fun observeCommutes(from: Long, to: Long): Flow<List<Commute>> =
        commuteDao.observeBetween(from, to).map { list -> list.map { it.toDomain() } }

    suspend fun commuteCount(from: Long, to: Long): Int = commuteDao.countBetween(from, to)

    suspend fun rainyCommuteCount(from: Long, to: Long): Int = commuteDao.countWithRainBetween(from, to)

    suspend fun addNote(note: Note): Long = noteDao.insert(note.toEntity())

    fun observeNotes(eventId: Long): Flow<List<Note>> =
        noteDao.observeForEvent(eventId).map { list -> list.map { it.toDomain() } }

    /** Notes of every event that starts between [from] and [to]. */
    fun observeNotesInRange(from: Long, to: Long): Flow<List<Note>> =
        noteDao.observeForRange(from, to).map { list -> list.map { it.toDomain() } }

    suspend fun deleteNote(id: Long) = noteDao.deleteById(id)

    /** Removes all events (and their notes) and all commutes. */
    suspend fun deleteAll() {
        noteDao.deleteAll()
        eventDao.deleteAll()
        commuteDao.deleteAll()
    }
}
