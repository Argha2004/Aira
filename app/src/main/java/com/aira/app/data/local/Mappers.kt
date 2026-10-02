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

// Enums are stored as text. An unknown value (e.g. from a newer app version) falls back safely.
private fun placeOf(name: String) = Place.entries.firstOrNull { it.name == name } ?: Place.UNKNOWN
private fun movementOf(name: String) = Movement.entries.firstOrNull { it.name == name } ?: Movement.STILL
private fun eventTypeOf(name: String) = DiaryEventType.entries.firstOrNull { it.name == name } ?: DiaryEventType.INDOOR

fun Snapshot.toEntity() = SnapshotEntity(
    id = id, timestamp = timestamp, latitude = latitude, longitude = longitude,
    temperature = temperature, feelsLike = feelsLike, humidity = humidity, rainMm = rainMm,
    uvIndex = uvIndex, windSpeed = windSpeed, lux = lux, stepsDelta = stepsDelta,
    totalSteps = totalSteps, pressure = pressure, inPocket = inPocket,
    isOutdoor = isOutdoor, isMoving = isMoving, place = place.name, movement = movement.name,
    weatherPending = weatherPending,
)

fun SnapshotEntity.toDomain() = Snapshot(
    id = id, timestamp = timestamp, latitude = latitude, longitude = longitude,
    temperature = temperature, feelsLike = feelsLike, humidity = humidity, rainMm = rainMm,
    uvIndex = uvIndex, windSpeed = windSpeed, lux = lux, stepsDelta = stepsDelta,
    totalSteps = totalSteps, pressure = pressure, inPocket = inPocket,
    place = placeOf(place), movement = movementOf(movement), weatherPending = weatherPending,
)

fun DiaryEvent.toEntity() = DiaryEventEntity(id, startTime, endTime, type.name, summary, avgTemperature)

fun DiaryEventEntity.toDomain() =
    DiaryEvent(id, startTime, endTime, eventTypeOf(type), summary, avgTemperature)

fun Commute.toEntity() = CommuteEntity(id, fromPlace, toPlace, startTime, endTime, hadRain, maxFeelsLike)

fun CommuteEntity.toDomain() = Commute(id, fromPlace, toPlace, startTime, endTime, hadRain, maxFeelsLike)

private const val NO_TRIGGER = "NONE"

private fun alertTypeOf(name: String) = AlertType.entries.firstOrNull { it.name == name }
private fun repeatOf(name: String) = TaskRepeat.entries.firstOrNull { it.name == name } ?: TaskRepeat.ONCE
private fun statusOf(name: String) = TaskStatus.entries.firstOrNull { it.name == name } ?: TaskStatus.PENDING

fun WeatherAlert.toEntity() = AlertEntity(id, type.name, time, message, value, dismissed)

/** An alert of an unknown type (from a newer app version) cannot be shown, so it becomes null. */
fun AlertEntity.toDomain(): WeatherAlert? =
    alertTypeOf(type)?.let { WeatherAlert(id, it, time, message, value, dismissed) }

fun WeatherTask.toEntity() = TaskEntity(
    id = id, title = title, note = note, triggerType = trigger?.name ?: NO_TRIGGER, dueTime = dueTime,
    isOutdoor = isOutdoor, repeat = repeat.name, status = status.name, snoozedUntil = snoozedUntil,
    sourceAlertId = sourceAlertId, createdAt = createdAt, completedAt = completedAt,
)

fun TaskEntity.toDomain() = WeatherTask(
    id = id, title = title, note = note, trigger = alertTypeOf(triggerType), dueTime = dueTime,
    isOutdoor = isOutdoor, repeat = repeatOf(repeat), status = statusOf(status), snoozedUntil = snoozedUntil,
    sourceAlertId = sourceAlertId, createdAt = createdAt, completedAt = completedAt,
)

fun Note.toEntity() = NoteEntity(id, eventId, text, photoUri, createdAt)

fun NoteEntity.toDomain() = Note(id, eventId, text, photoUri, createdAt)
