package com.aira.app.domain.engine

import java.time.LocalDate

/**
 * The numbers behind the Insights cards for one period (a week, a month or a year) compared with the period
 * before it. Temperatures in °C. Averages are per calendar day of the period.
 */
data class PeriodReflection(
    val sunMinutes: Int,
    /** Change in sun minutes against the previous period in percent, or null if the previous period had none. */
    val sunChangePercent: Int?,
    val sunPeakDate: LocalDate?,
    val avgSunMinutes: Int,
    val rainEncounters: Int,
    /** Bars (days, or months in the year view) with at least one rain encounter. */
    val rainDays: Int,
    val loggedDays: Int,
    val warmestDate: LocalDate?,
    val warmestFeelsLike: Double?,
    val coolestFeelsLike: Double?,
    val outdoorMinutes: Int = 0,
    /** Change in outdoor minutes against the previous period in percent, or null if the previous period had none. */
    val outdoorChangePercent: Int? = null,
    val avgHeatMinutes: Int = 0,
    /** Average of the bars' feels-like temperatures, or null if there are none. */
    val avgFeelsLike: Double? = null,
)

object PeriodReflections {

    /** Works out the reflection for [current], compared with [previous] (the period just before it). */
    fun of(current: ChartData, previous: ChartData?): PeriodReflection {
        val sun = current.sunMinutes.sum()
        val outdoor = current.outdoorMinutes.sum()
        val warmestIndex = current.avgFeelsLike.indices.filter { current.avgFeelsLike[it] != null }
            .maxByOrNull { current.avgFeelsLike[it]!! }
        val sunPeakIndex = current.sunMinutes.indices.maxByOrNull { current.sunMinutes[it] }?.takeIf { current.sunMinutes[it] > 0 }
        val days = current.dayCount
        val temps = current.avgFeelsLike.filterNotNull()
        return PeriodReflection(
            sunMinutes = sun,
            sunChangePercent = change(sun, previous?.sunMinutes?.sum() ?: 0),
            sunPeakDate = sunPeakIndex?.let { current.dates[it] },
            avgSunMinutes = if (days == 0) 0 else sun / days,
            rainEncounters = current.rainEncounters.sum(),
            rainDays = current.rainEncounters.count { it > 0 },
            loggedDays = current.dates.indices.count { current.sunMinutes[it] > 0 || current.rainEncounters[it] > 0 || current.avgFeelsLike[it] != null },
            warmestDate = warmestIndex?.let { current.dates[it] },
            warmestFeelsLike = warmestIndex?.let { current.avgFeelsLike[it] },
            coolestFeelsLike = temps.minOrNull(),
            outdoorMinutes = outdoor,
            outdoorChangePercent = change(outdoor, previous?.outdoorMinutes?.sum() ?: 0),
            avgHeatMinutes = if (days == 0) 0 else current.heatMinutes.sum() / days,
            avgFeelsLike = temps.takeIf { it.isNotEmpty() }?.average(),
        )
    }

    /** Percent change from [before] to [now], rounded; null when there is nothing to compare with. */
    private fun change(now: Int, before: Int): Int? =
        if (before > 0) Math.round((now - before) * 100.0 / before).toInt() else null
}
