package com.aira.app.domain.model

/** How often a task linked to an alert comes back: only once, or every time that alert fires. */
enum class TaskRepeat { ONCE, EVERY_TIME }

enum class TaskStatus { PENDING, DONE, SNOOZED }

/**
 * A to-do. It can be a plain timed task, or linked to an alert type ("when rain is expected, bring
 * clothes inside"). [trigger] is null for a plain task. All times are epoch millis.
 */
data class WeatherTask(
    val id: Long = 0,
    val title: String,
    val note: String = "",
    val trigger: AlertType? = null,
    val dueTime: Long? = null,
    val isOutdoor: Boolean = false,
    val repeat: TaskRepeat = TaskRepeat.ONCE,
    val status: TaskStatus = TaskStatus.PENDING,
    val snoozedUntil: Long? = null,
    /** The alert this task was created from, if any. */
    val sourceAlertId: Long? = null,
    val createdAt: Long = 0,
    val completedAt: Long? = null,
)

/** A weather alert that fired. [message] is a short headline, [value] the number behind it (or null). */
data class WeatherAlert(
    val id: Long = 0,
    val type: AlertType,
    val time: Long,
    val message: String,
    val value: Double? = null,
    val dismissed: Boolean = false,
)

/** Forecast for one hour. [time] is the start of that hour in epoch millis. */
data class HourlyForecast(
    val time: Long,
    val precipProbability: Int,
    val precipitationMm: Double,
    val feelsLikeC: Double,
    /** Air temperature and WMO weather code of that hour; null if the API did not send them. */
    val tempC: Double? = null,
    val weatherCode: Int? = null,
)
