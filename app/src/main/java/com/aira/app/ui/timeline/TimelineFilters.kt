package com.aira.app.ui.timeline

import com.aira.app.domain.model.DiaryEventType

/** The filter chips above the timeline. */
enum class TimelineFilter { ALL, OUTDOORS, INDOORS, COMMUTE, ALERTS }

/** The numbers in the "Daily ambient log" card. Temperatures in °C; null when the day has no events. */
data class DaySummary(
    val highC: Double?,
    val lowC: Double?,
    val avgC: Double?,
    val rainEvents: Int,
    val chapters: Int,
    /** When the first rain event of the day started (epoch millis), or null if it did not rain on the user. */
    val firstRainAt: Long? = null,
    /** How long that first rain event lasted, in minutes (0 if there was none). */
    val firstRainMinutes: Int = 0,
)

/** Pure helpers for the Timeline screen (no Android code, so they are unit tested). */
object TimelineFilters {
    private val OUTDOOR_TYPES = setOf(DiaryEventType.OUTDOOR, DiaryEventType.SUN, DiaryEventType.RAIN, DiaryEventType.HEAT)

    /** Whether [entry] shows under [filter]. Alerts and finished tasks show under All and Alerts. */
    fun matches(entry: TimelineEntry, filter: TimelineFilter): Boolean = when (filter) {
        TimelineFilter.ALL -> true
        TimelineFilter.ALERTS -> entry !is TimelineEntry.Event
        TimelineFilter.OUTDOORS -> entry is TimelineEntry.Event && entry.item.event.type in OUTDOOR_TYPES
        TimelineFilter.INDOORS -> entry is TimelineEntry.Event && entry.item.event.type == DiaryEventType.INDOOR
        TimelineFilter.COMMUTE -> entry is TimelineEntry.Event && entry.item.event.type == DiaryEventType.COMMUTE
    }

    fun count(entries: List<TimelineEntry>, filter: TimelineFilter): Int = entries.count { matches(it, filter) }

    /**
     * High, low and average of the events' temperatures (events without a temperature, stored as 0, are
     * left out), the number of rain events, and the number of events ("chapters").
     */
    fun summary(entries: List<TimelineEntry>): DaySummary {
        val events = entries.filterIsInstance<TimelineEntry.Event>().map { it.item.event }
        val temps = events.map { it.avgTemperature }.filter { it != 0.0 }
        val firstRain = events.filter { it.type == DiaryEventType.RAIN }.minByOrNull { it.startTime }
        return DaySummary(
            highC = temps.maxOrNull(),
            lowC = temps.minOrNull(),
            avgC = temps.takeIf { it.isNotEmpty() }?.average(),
            rainEvents = events.count { it.type == DiaryEventType.RAIN },
            chapters = events.size,
            firstRainAt = firstRain?.startTime,
            firstRainMinutes = firstRain?.let { ((it.endTime - it.startTime) / 60_000).toInt() } ?: 0,
        )
    }
}
