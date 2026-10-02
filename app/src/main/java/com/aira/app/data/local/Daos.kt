package com.aira.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Insert
    suspend fun insert(snapshot: SnapshotEntity): Long

    @Insert
    suspend fun insertAll(snapshots: List<SnapshotEntity>)

    @Query("DELETE FROM snapshots WHERE timestamp BETWEEN :from AND :to")
    suspend fun deleteBetween(from: Long, to: Long)

    @Query("SELECT * FROM snapshots WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp")
    fun observeBetween(from: Long, to: Long): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp")
    suspend fun getBetween(from: Long, to: Long): List<SnapshotEntity>

    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<SnapshotEntity?>

    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): SnapshotEntity?

    // Aggregates for insights.
    @Query("SELECT COUNT(*) FROM snapshots WHERE timestamp BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM snapshots WHERE isOutdoor = 1 AND timestamp BETWEEN :from AND :to")
    suspend fun countOutdoorBetween(from: Long, to: Long): Int

    /** Snapshots still waiting for weather are skipped: their rain value is only a placeholder. */
    @Query(
        "SELECT COUNT(*) FROM snapshots WHERE isOutdoor = 1 AND weatherPending = 0 " +
            "AND rainMm > :minRainMm AND timestamp BETWEEN :from AND :to",
    )
    suspend fun countOutdoorRainBetween(from: Long, to: Long, minRainMm: Double): Int

    // Offline snapshots that still need their weather.
    @Query("SELECT * FROM snapshots WHERE weatherPending = 1 ORDER BY timestamp")
    suspend fun getPendingWeather(): List<SnapshotEntity>

    @Query(
        "UPDATE snapshots SET temperature = :temperature, feelsLike = :feelsLike, humidity = :humidity, " +
            "rainMm = :rainMm, uvIndex = :uvIndex, windSpeed = :windSpeed, weatherPending = 0 WHERE id = :id",
    )
    suspend fun fillWeather(
        id: Long,
        temperature: Double,
        feelsLike: Double,
        humidity: Int,
        rainMm: Double,
        uvIndex: Double,
        windSpeed: Double,
    )

    @Query("DELETE FROM snapshots")
    suspend fun deleteAll()
}

@Dao
interface DiaryEventDao {
    @Insert
    suspend fun insert(event: DiaryEventEntity): Long

    @Insert
    suspend fun insertAll(events: List<DiaryEventEntity>)

    /** Events that start inside the given range (use a day's start and end for one day). */
    @Query("SELECT * FROM diary_events WHERE startTime BETWEEN :from AND :to ORDER BY startTime")
    fun observeBetween(from: Long, to: Long): Flow<List<DiaryEventEntity>>

    @Query("SELECT * FROM diary_events WHERE startTime BETWEEN :from AND :to ORDER BY startTime")
    suspend fun getBetween(from: Long, to: Long): List<DiaryEventEntity>

    @Update
    suspend fun update(event: DiaryEventEntity)

    @Query("DELETE FROM diary_events WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM diary_events WHERE startTime BETWEEN :from AND :to")
    suspend fun deleteBetween(from: Long, to: Long)

    @Query("DELETE FROM diary_events")
    suspend fun deleteAll()
}

@Dao
interface CommuteDao {
    @Insert
    suspend fun insert(commute: CommuteEntity): Long

    @Insert
    suspend fun insertAll(commutes: List<CommuteEntity>)

    @Query("SELECT * FROM commutes WHERE startTime BETWEEN :from AND :to ORDER BY startTime")
    fun observeBetween(from: Long, to: Long): Flow<List<CommuteEntity>>

    @Query("SELECT COUNT(*) FROM commutes WHERE startTime BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM commutes WHERE hadRain = 1 AND startTime BETWEEN :from AND :to")
    suspend fun countWithRainBetween(from: Long, to: Long): Int

    @Query("DELETE FROM commutes WHERE startTime BETWEEN :from AND :to")
    suspend fun deleteBetween(from: Long, to: Long)

    @Query("DELETE FROM commutes")
    suspend fun deleteAll()
}

@Dao
interface AlertDao {
    @Insert
    suspend fun insert(alert: AlertEntity): Long

    @Query("SELECT * FROM alerts WHERE id = :id")
    suspend fun get(id: Long): AlertEntity?

    /** Alerts from the last 24 hours (pass "now minus 24 h" as [since]) that were not dismissed, newest first. */
    @Query("SELECT * FROM alerts WHERE dismissed = 0 AND time >= :since ORDER BY time DESC")
    fun observeActive(since: Long): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts ORDER BY time DESC")
    fun observeHistory(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE time BETWEEN :from AND :to ORDER BY time")
    fun observeBetween(from: Long, to: Long): Flow<List<AlertEntity>>

    @Query("UPDATE alerts SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("DELETE FROM alerts")
    suspend fun deleteAll()
}

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY createdAt")
    suspend fun getAll(): List<TaskEntity>

    /** Tasks that are not done: pending and snoozed ones. */
    @Query("SELECT * FROM tasks WHERE status != 'DONE' ORDER BY createdAt")
    fun observePending(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'DONE' ORDER BY completedAt DESC")
    fun observeDone(): Flow<List<TaskEntity>>

    /** Tasks ticked off inside the range (one day's completed tasks for the timeline). */
    @Query("SELECT * FROM tasks WHERE status = 'DONE' AND completedAt BETWEEN :from AND :to ORDER BY completedAt")
    fun observeCompletedBetween(from: Long, to: Long): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Query("SELECT * FROM notes WHERE eventId = :eventId ORDER BY createdAt")
    fun observeForEvent(eventId: Long): Flow<List<NoteEntity>>

    /** Notes of all events that start inside the range (one day's notes). */
    @Query(
        "SELECT notes.* FROM notes INNER JOIN diary_events ON notes.eventId = diary_events.id " +
            "WHERE diary_events.startTime BETWEEN :from AND :to ORDER BY notes.createdAt",
    )
    fun observeForRange(from: Long, to: Long): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
