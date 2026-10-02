package com.aira.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Table "snapshots". [place] and [movement] hold the enum names as text. */
@Entity(tableName = "snapshots", indices = [Index("timestamp")])
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val rainMm: Double,
    val uvIndex: Double,
    val windSpeed: Double,
    val lux: Float?,
    val stepsDelta: Int?,
    val totalSteps: Long?,
    val pressure: Float?,
    val inPocket: Boolean?,
    val isOutdoor: Boolean,
    val isMoving: Boolean,
    val place: String,
    val movement: String,
    @ColumnInfo(defaultValue = "0") val weatherPending: Boolean = false,
)

/** Table "diary_events". [type] holds a DiaryEventType name. */
@Entity(tableName = "diary_events", indices = [Index("startTime")])
data class DiaryEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val type: String,
    val summary: String,
    val avgTemperature: Double,
)

/** Table "commutes". */
@Entity(tableName = "commutes", indices = [Index("startTime")])
data class CommuteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromPlace: String,
    val toPlace: String,
    val startTime: Long,
    val endTime: Long,
    val hadRain: Boolean,
    val maxFeelsLike: Double,
)

/** Table "alerts". [type] holds an AlertType name. [value] is the number behind the alert, if any. */
@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val time: Long,
    val message: String,
    val value: Double?,
    val dismissed: Boolean,
)

/**
 * Table "tasks". [triggerType] is an AlertType name, or "NONE" for a plain task.
 * [repeat] is "ONCE" or "EVERY_TIME"; [status] is "PENDING", "DONE" or "SNOOZED".
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String,
    val triggerType: String,
    val dueTime: Long?,
    val isOutdoor: Boolean,
    val repeat: String,
    val status: String,
    val snoozedUntil: Long?,
    val sourceAlertId: Long?,
    val createdAt: Long,
    val completedAt: Long?,
)

/** Table "notes". Deleting a diary event deletes its notes too. */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("eventId")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val text: String,
    val photoUri: String?,
    val createdAt: Long,
)
