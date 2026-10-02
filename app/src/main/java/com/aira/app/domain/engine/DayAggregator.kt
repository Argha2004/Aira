package com.aira.app.domain.engine

import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Everything the calendar and insights need to know about one day. */
data class DaySummary(
    val date: LocalDate,
    val stats: DailyStats,
    /** Average feels-like temperature of the outdoor snapshots, or null if there were none. */
    val avgOutdoorFeelsLike: Double?,
    /** Minutes outdoors with a feels-like temperature above 35 °C. */
    val hotOutdoorMinutes: Int,
    /** Highest feels-like temperature while walking, or null if there was no walk. */
    val maxWalkFeelsLike: Double?,
    /** The day's weather over all snapshots with weather (not only outdoor ones); null when there are none. */
    val day: DayWeather? = null,
)

/** Average, highest and lowest temperature, average feels-like and average wind of one day. °C and km/h. */
data class DayWeather(
    val avgTempC: Double,
    val maxTempC: Double,
    val minTempC: Double,
    val avgFeelsLikeC: Double,
    val avgWindKmh: Double,
)

object DayAggregator {

    /** One [DaySummary] per calendar day (in [zone]) that has snapshots, oldest first. */
    fun summarize(snapshots: List<Snapshot>, zone: ZoneId): List<DaySummary> =
        snapshots
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .toSortedMap()
            .map { (date, day) -> summarizeDay(date, day) }

    private fun summarizeDay(date: LocalDate, snapshots: List<Snapshot>): DaySummary {
        val sorted = snapshots.sortedBy { it.timestamp }
        val minutes = SnapshotTiming.minutes(sorted)
        // Snapshots waiting for weather have placeholder temperatures; leave them out.
        val outdoorWithWeather = sorted.filter { it.place == Place.OUTDOOR && !it.weatherPending }
        val hotMinutes = sorted.indices
            .filter {
                val s = sorted[it]
                s.place == Place.OUTDOOR && !s.weatherPending && s.feelsLike > Thresholds.HOT_INSIGHT_C
            }
            .sumOf { minutes[it] }

        val withWeather = sorted.filter { !it.weatherPending }
        val dayWeather = withWeather.takeIf { it.isNotEmpty() }?.let { list ->
            DayWeather(
                avgTempC = list.map { it.temperature }.average(),
                maxTempC = list.maxOf { it.temperature },
                minTempC = list.minOf { it.temperature },
                avgFeelsLikeC = list.map { it.feelsLike }.average(),
                avgWindKmh = list.map { it.windSpeed }.average(),
            )
        }

        return DaySummary(
            date = date,
            stats = DailyStatsCalculator.calculate(sorted),
            avgOutdoorFeelsLike = outdoorWithWeather.map { it.feelsLike }.takeIf { it.isNotEmpty() }?.average(),
            hotOutdoorMinutes = hotMinutes.toInt(),
            maxWalkFeelsLike = sorted.filter { it.movement == Movement.WALKING && !it.weatherPending }
                .maxOfOrNull { it.feelsLike },
            day = dayWeather,
        )
    }
}
