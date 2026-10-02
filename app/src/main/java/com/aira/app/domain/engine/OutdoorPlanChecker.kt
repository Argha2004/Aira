package com.aira.app.domain.engine

import com.aira.app.domain.model.HourlyForecast
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

enum class PlanIssue { RAIN, HEAT }

/** [badHour] is the forecast for the planned hour; [betterHour] is the nearest good hour today, or null. */
data class PlanWarning(val issue: PlanIssue, val badHour: HourlyForecast, val betterHour: HourlyForecast?)

/** Checks an outdoor plan against the hourly forecast. Pure Kotlin. */
object OutdoorPlanChecker {

    private const val HOUR_MS = 60 * 60 * 1000L

    /**
     * Warns when rain is likely (60% or more) or it will feel hotter than 38 °C at the hour of [dueTime].
     * Then it looks for the nearest hour on the same day, not yet over, with neither problem.
     * Returns null when the plan is fine, or when the forecast has no entry for that hour.
     */
    fun check(dueTime: Long, forecast: List<HourlyForecast>, now: Long, zone: ZoneId): PlanWarning? {
        val planned = forecast.firstOrNull { covers(it, dueTime) } ?: return null
        val issue = when {
            planned.precipProbability >= Thresholds.RAIN_SOON_PROBABILITY -> PlanIssue.RAIN
            planned.feelsLikeC > Thresholds.HEAT_ALERT_C -> PlanIssue.HEAT
            else -> return null
        }
        val day = Instant.ofEpochMilli(dueTime).atZone(zone).toLocalDate()
        val better = forecast
            .filter { it != planned && it.time + HOUR_MS > now && isGood(it) }
            .filter { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() == day }
            .minWithOrNull(compareBy({ abs(it.time - planned.time) }, { it.time }))
        return PlanWarning(issue, planned, better)
    }

    private fun isGood(hour: HourlyForecast) =
        hour.precipProbability < Thresholds.RAIN_SOON_PROBABILITY && hour.feelsLikeC <= Thresholds.HEAT_ALERT_C

    private fun covers(hour: HourlyForecast, time: Long) = time >= hour.time && time < hour.time + HOUR_MS
}
