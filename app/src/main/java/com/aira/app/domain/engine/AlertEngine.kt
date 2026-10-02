package com.aira.app.domain.engine

import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.HourlyForecast
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot

/** Decides which weather alerts apply right now. Pure Kotlin. */
object AlertEngine {

    private const val HOUR_MS = 60 * 60 * 1000L

    /**
     * Alerts that apply to the newest of [recent] (recent snapshots in any order, about the last 3 hours):
     * - RAIN_SOON: the hourly [forecast] shows a rain probability of 60% or more in the next 3 hours.
     * - RAIN_NOW: raining more than 0.2 mm while outdoors or walking.
     * - HEAT: outdoors and it feels hotter than 38 °C.
     * - STRONG_SUN: outdoors in strong sun (over 30,000 lux) for 45 minutes or more without a break.
     * - PRESSURE_DROP: pressure fell more than 3 hPa in 3 hours (phones with a barometer only).
     * Snapshots still waiting for weather cannot cause HEAT or RAIN_NOW. Without a forecast there is no RAIN_SOON.
     */
    fun evaluate(
        recent: List<Snapshot>,
        forecast: List<HourlyForecast> = emptyList(),
        now: Long = recent.maxOfOrNull { it.timestamp } ?: 0L,
    ): List<AlertType> {
        val sorted = recent.sortedBy { it.timestamp }
        val newest = sorted.lastOrNull() ?: return emptyList()
        return buildList {
            if (rainSoonHour(forecast, now) != null) add(AlertType.RAIN_SOON)
            if (!newest.weatherPending && ExposureRules.isRainEncounter(newest.place, newest.movement, newest.rainMm)) {
                add(AlertType.RAIN_NOW)
            }
            if (!newest.weatherPending && newest.place == Place.OUTDOOR && newest.feelsLike > Thresholds.HEAT_ALERT_C) {
                add(AlertType.HEAT)
            }
            if (minutesInStrongSun(sorted) >= Thresholds.STRONG_SUN_ALERT_MINUTES) add(AlertType.STRONG_SUN)
            val readings = sorted.mapNotNull { s -> s.pressure?.let { s.timestamp to it } }
            if (PressureTrend.detectDrop(readings)) add(AlertType.PRESSURE_DROP)
        }
    }

    /**
     * The first forecast hour in the next 3 hours with a rain probability of 60% or more, or null.
     * The hour we are in counts (rain may already be on its way), so an hour is "in range" when it
     * is not over yet and starts within 3 hours from [now].
     */
    fun rainSoonHour(forecast: List<HourlyForecast>, now: Long): HourlyForecast? =
        forecast
            .filter { it.time + HOUR_MS > now && it.time <= now + Thresholds.RAIN_SOON_LOOKAHEAD_MS }
            .filter { it.precipProbability >= Thresholds.RAIN_SOON_PROBABILITY }
            .minByOrNull { it.time }

    /**
     * How long the newest snapshot has been in an unbroken run of strong sun: the time from the first
     * snapshot of the run to the newest one. A gap of more than 60 minutes between snapshots ends the run.
     */
    fun minutesInStrongSun(sorted: List<Snapshot>): Double {
        val newest = sorted.lastOrNull() ?: return 0.0
        if (!ExposureRules.isSunExposure(newest.place, newest.lux)) return 0.0
        var start = newest
        for (i in sorted.lastIndex - 1 downTo 0) {
            val s = sorted[i]
            val tooFarApart = (start.timestamp - s.timestamp) / 60_000.0 > Thresholds.MAX_SNAPSHOT_MINUTES
            if (!ExposureRules.isSunExposure(s.place, s.lux) || tooFarApart) break
            start = s
        }
        return (newest.timestamp - start.timestamp) / 60_000.0
    }
}
