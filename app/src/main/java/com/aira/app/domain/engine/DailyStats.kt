package com.aira.app.domain.engine

import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot
import kotlin.math.roundToInt

/** The numbers shown on Home for one day. */
data class DailyStats(
    val sunMinutes: Int,
    val heatMinutes: Int,
    val rainEncounters: Int,
    val uvDoseBand: UvBand,
    val outdoorMinutes: Int,
    val steps: Int,
) {
    companion object {
        val EMPTY = DailyStats(0, 0, 0, UvBand.LOW, 0, 0)
    }
}

/** Adds up a day's snapshots with the exposure rules. Pure Kotlin. */
object DailyStatsCalculator {

    fun calculate(snapshots: List<Snapshot>): DailyStats {
        val sorted = snapshots.sortedBy { it.timestamp }
        val minutes = SnapshotTiming.minutes(sorted)

        fun minutesWhere(test: (Snapshot) -> Boolean) =
            sorted.indices.filter { test(sorted[it]) }.sumOf { minutes[it] }

        // Snapshots still waiting for weather have placeholder weather numbers, so they are skipped.
        val uvDose = sorted.indices
            .filter { sorted[it].place == Place.OUTDOOR && !sorted[it].weatherPending }
            .sumOf { ExposureRules.uvDose(sorted[it].uvIndex, minutes[it]) }

        return DailyStats(
            sunMinutes = minutesWhere { ExposureRules.isSunExposure(it.place, it.lux) }.roundToInt(),
            heatMinutes = minutesWhere {
                !it.weatherPending && ExposureRules.isHeatExposure(it.place, it.movement, it.feelsLike)
            }.roundToInt(),
            rainEncounters = countRainEncounters(sorted),
            uvDoseBand = ExposureRules.uvDoseBand(uvDose),
            outdoorMinutes = minutesWhere { it.place == Place.OUTDOOR }.roundToInt(),
            steps = sorted.sumOf { it.stepsDelta ?: 0 },
        )
    }

    /** Neighbouring rainy snapshots count as one encounter. */
    private fun countRainEncounters(sorted: List<Snapshot>): Int {
        var count = 0
        var inRain = false
        for (s in sorted) {
            val rainy = !s.weatherPending && ExposureRules.isRainEncounter(s.place, s.movement, s.rainMm)
            if (rainy && !inRain) count++
            inRain = rainy
        }
        return count
    }
}
