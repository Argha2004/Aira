package com.aira.app.domain.engine

import java.time.LocalDate
import java.time.YearMonth

/**
 * One value per bar for the Insights charts, oldest first. A bar is one day (week and month views) or one
 * month (year view, then [dates] holds the first day of each month and the values are that month's totals).
 */
data class ChartData(
    val dates: List<LocalDate>,
    val sunMinutes: List<Int>,
    val rainEncounters: List<Int>,
    /** Average feels-like outdoors; null for bars without outdoor time. */
    val avgFeelsLike: List<Double?>,
    val outdoorMinutes: List<Int> = emptyList(),
    val heatMinutes: List<Int> = emptyList(),
    /** How many calendar days the bars cover (7, 30, or the days of the 12 months). */
    val dayCount: Int = dates.size,
) {
    val hasData: Boolean get() = sunMinutes.any { it > 0 } || rainEncounters.any { it > 0 } || avgFeelsLike.any { it != null }

    companion object {
        /** The [days] days ending on [endDate]. A day without a summary counts as zero (or null for temperature). */
        fun lastDays(summaries: List<DaySummary>, endDate: LocalDate, days: Int): ChartData {
            val byDate = summaries.associateBy { it.date }
            val dates = (days - 1 downTo 0).map { endDate.minusDays(it.toLong()) }
            return ChartData(
                dates = dates,
                sunMinutes = dates.map { byDate[it]?.stats?.sunMinutes ?: 0 },
                rainEncounters = dates.map { byDate[it]?.stats?.rainEncounters ?: 0 },
                avgFeelsLike = dates.map { byDate[it]?.avgOutdoorFeelsLike },
                outdoorMinutes = dates.map { byDate[it]?.stats?.outdoorMinutes ?: 0 },
                heatMinutes = dates.map { byDate[it]?.stats?.heatMinutes ?: 0 },
            )
        }

        /**
         * The [months] months ending with [endMonth], one bar per month: sun, rain, outdoor and heat are summed;
         * the feels-like is the average of the days that have one.
         */
        fun lastMonths(summaries: List<DaySummary>, endMonth: YearMonth, months: Int): ChartData {
            val shown = (months - 1 downTo 0).map { endMonth.minusMonths(it.toLong()) }
            val byMonth = summaries.groupBy { YearMonth.from(it.date) }
            fun sum(month: YearMonth, value: (DaySummary) -> Int) = byMonth[month].orEmpty().sumOf(value)
            return ChartData(
                dates = shown.map { it.atDay(1) },
                sunMinutes = shown.map { m -> sum(m) { it.stats.sunMinutes } },
                rainEncounters = shown.map { m -> sum(m) { it.stats.rainEncounters } },
                avgFeelsLike = shown.map { m -> byMonth[m].orEmpty().mapNotNull { it.avgOutdoorFeelsLike }.takeIf { it.isNotEmpty() }?.average() },
                outdoorMinutes = shown.map { m -> sum(m) { it.stats.outdoorMinutes } },
                heatMinutes = shown.map { m -> sum(m) { it.stats.heatMinutes } },
                dayCount = shown.sumOf { it.lengthOfMonth() },
            )
        }
    }
}
