package com.aira.app.domain.engine

import com.aira.app.domain.model.HourlyForecast

/** Looks things up in an hourly forecast. */
/** A coming change in the weather: rain likely, or very hot. */
enum class ChangeKind { RAIN, HEAT }

data class ForecastChange(val time: Long, val kind: ChangeKind)

object ForecastLookup {
    private const val HOUR_MS = 60 * 60 * 1000L

    /** The forecast hour that contains [now] (an hour starts at its time and lasts one hour), or null if there is none. */
    fun currentHour(forecast: List<HourlyForecast>, now: Long): HourlyForecast? =
        forecast.lastOrNull { now >= it.time && now < it.time + HOUR_MS }

    /**
     * The first coming hour (within [hoursAhead] hours of [now]) where rain becomes likely (60% or more) or it
     * feels hotter than the heat alert limit (38 °C); rain wins if both. Null if nothing changes.
     */
    fun nextChange(forecast: List<HourlyForecast>, now: Long, hoursAhead: Int = 12): ForecastChange? =
        nextHours(forecast, now, hoursAhead).firstNotNullOfOrNull { hour ->
            when {
                hour.precipProbability >= Thresholds.RAIN_SOON_PROBABILITY -> ForecastChange(hour.time, ChangeKind.RAIN)
                hour.feelsLikeC > Thresholds.HEAT_ALERT_C -> ForecastChange(hour.time, ChangeKind.HEAT)
                else -> null
            }
        }

    /** Up to [count] forecast hours, starting with the hour that contains [now], in time order. */
    fun nextHours(forecast: List<HourlyForecast>, now: Long, count: Int): List<HourlyForecast> =
        forecast.sortedBy { it.time }.filter { it.time + HOUR_MS > now }.take(count)
}
