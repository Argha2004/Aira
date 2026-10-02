package com.aira.app.domain.model

enum class DiaryEventType { SUN, RAIN, HEAT, COMMUTE, INDOOR, OUTDOOR, PRESSURE_DROP }

/** A stretch of the day worth showing on the timeline, e.g. "Outdoors in strong sun, 45 min". */
data class DiaryEvent(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val type: DiaryEventType,
    val summary: String,
    val avgTemperature: Double,
)

/** A trip between two named places (for example Home to College). */
data class Commute(
    val id: Long = 0,
    val fromPlace: String,
    val toPlace: String,
    val startTime: Long,
    val endTime: Long,
    val hadRain: Boolean,
    val maxFeelsLike: Double,
)

/** A note the user attached to a diary event. */
data class Note(
    val id: Long = 0,
    val eventId: Long,
    val text: String,
    val photoUri: String? = null,
    val createdAt: Long,
)
