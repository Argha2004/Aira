package com.aira.app.domain.model

/** Weather for one past hour. [time] is the start of that hour in epoch millis. */
data class HourlyWeather(
    val time: Long,
    val tempC: Double,
    val feelsLikeC: Double,
    val humidity: Int,
    val rainMm: Double,
    val uvIndex: Double,
    val windKmh: Double,
)

private const val HOUR_MS = 60 * 60 * 1000L

/** The hour that contains [timestamp], or null if the list has no such hour. */
fun List<HourlyWeather>.forTimestamp(timestamp: Long): HourlyWeather? =
    filter { it.time <= timestamp && timestamp - it.time < HOUR_MS }.maxByOrNull { it.time }
