package com.aira.app.domain.engine

import com.aira.app.domain.model.Commute
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Writes the insight sentences shown on the Insights screen. Pure Kotlin. */
object InsightGenerator {

    /**
     * [summaries] are day summaries (any range that covers last week and this week). [monthCommutes] are
     * the commutes of the month that [today] is in. A sentence is left out when there is not enough data.
     */
    fun generate(summaries: List<DaySummary>, monthCommutes: List<Commute>, today: LocalDate): List<String> {
        val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastWeekStart = thisWeekStart.minusWeeks(1)
        val thisWeek = summaries.filter { it.date >= thisWeekStart && it.date <= today }
        val lastWeek = summaries.filter { it.date >= lastWeekStart && it.date < thisWeekStart }

        return listOfNotNull(
            sunComparison(thisWeek, lastWeek),
            rainyCommutes(monthCommutes),
            hottestDay(thisWeek),
            outdoorTime(thisWeek),
        )
    }

    /** Sun per recorded day, this week against last week, so a half-finished week is still fair. */
    private fun sunComparison(thisWeek: List<DaySummary>, lastWeek: List<DaySummary>): String? {
        if (thisWeek.size < Thresholds.MIN_DAYS_FOR_COMPARISON || lastWeek.size < Thresholds.MIN_DAYS_FOR_COMPARISON) {
            return null
        }
        val thisAverage = thisWeek.map { it.stats.sunMinutes }.average()
        val lastAverage = lastWeek.map { it.stats.sunMinutes }.average()
        if (lastAverage <= 0.0) return null
        val change = ((thisAverage - lastAverage) / lastAverage * 100).roundToInt()
        return when {
            abs(change) < Thresholds.SAME_AMOUNT_PERCENT -> "You got about the same amount of sun this week as last week."
            change > 0 -> "You got $change% more sun this week than last week."
            else -> "You got ${-change}% less sun this week than last week."
        }
    }

    private fun rainyCommutes(commutes: List<Commute>): String? {
        if (commutes.isEmpty()) return null
        val rainy = commutes.count { it.hadRain }
        return if (rainy == 0) {
            "None of your ${commutes.size} commutes were in the rain this month."
        } else {
            "It rained on $rainy of your ${commutes.size} commutes this month."
        }
    }

    /** The day of this week with the most outdoor time above 35 °C (at least 30 minutes). */
    private fun hottestDay(thisWeek: List<DaySummary>): String? {
        val hottest = thisWeek.maxByOrNull { it.hotOutdoorMinutes } ?: return null
        if (hottest.hotOutdoorMinutes < MIN_HOT_MINUTES) return null
        val name = hottest.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "Your hottest day was $name: ${formatMinutes(hottest.hotOutdoorMinutes)} outdoors above " +
            "${Thresholds.HOT_INSIGHT_C.roundToInt()} °C."
    }

    private fun outdoorTime(thisWeek: List<DaySummary>): String? {
        val minutes = thisWeek.sumOf { it.stats.outdoorMinutes }
        return if (minutes < MIN_OUTDOOR_MINUTES) null else "You spent ${formatMinutes(minutes)} outdoors this week."
    }

    private const val MIN_HOT_MINUTES = 30
    private const val MIN_OUTDOOR_MINUTES = 30
}
