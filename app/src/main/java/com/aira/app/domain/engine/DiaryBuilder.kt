package com.aira.app.domain.engine

import com.aira.app.domain.model.DiaryEvent
import com.aira.app.domain.model.DiaryEventType
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Turns a day's snapshots into timeline events. Pure Kotlin.
 *
 * Each snapshot gets one state; neighbouring snapshots with the same state are merged into one event.
 * State priority: rain, strong sun, heat, then plain outdoor or indoor. Snapshots where the user's
 * place is unknown and nothing special happened produce no event. Falling pressure is reported as
 * its own event that may overlap the others.
 */
object DiaryBuilder {

    fun build(snapshots: List<Snapshot>): List<DiaryEvent> {
        val sorted = snapshots.sortedBy { it.timestamp }
        val minutes = SnapshotTiming.minutes(sorted)
        val events = stateEvents(sorted, minutes) + pressureEvents(sorted, minutes)
        return events.sortedBy { it.startTime }
    }

    /** The state of one snapshot, or null when there is nothing to show. */
    fun stateOf(s: Snapshot): DiaryEventType? {
        val weatherKnown = !s.weatherPending
        return when {
            weatherKnown && ExposureRules.isRainEncounter(s.place, s.movement, s.rainMm) -> DiaryEventType.RAIN
            ExposureRules.isSunExposure(s.place, s.lux) -> DiaryEventType.SUN
            weatherKnown && ExposureRules.isHeatExposure(s.place, s.movement, s.feelsLike) -> DiaryEventType.HEAT
            s.place == Place.OUTDOOR -> DiaryEventType.OUTDOOR
            s.place == Place.INDOOR -> DiaryEventType.INDOOR
            else -> null
        }
    }

    private fun stateEvents(sorted: List<Snapshot>, minutes: List<Double>): List<DiaryEvent> {
        val events = mutableListOf<DiaryEvent>()
        var runStart = -1
        var runType: DiaryEventType? = null

        fun closeRun(lastIndex: Int) {
            val type = runType ?: return
            events += eventFor(type, sorted, minutes, runStart, lastIndex)
            runType = null
        }

        sorted.forEachIndexed { i, snapshot ->
            val type = stateOf(snapshot)
            val continuesRun = type != null && type == runType && !hasLongGap(sorted, i - 1, i)
            if (!continuesRun) {
                closeRun(i - 1)
                if (type != null) {
                    runType = type
                    runStart = i
                }
            }
        }
        closeRun(sorted.lastIndex)
        return events
    }

    private fun pressureEvents(sorted: List<Snapshot>, minutes: List<Double>): List<DiaryEvent> {
        val readings = mutableListOf<Pair<Long, Float>>()
        val dropAt = sorted.map { s ->
            s.pressure?.let { readings += s.timestamp to it }
            if (s.pressure != null && PressureTrend.detectDrop(readings)) PressureTrend.dropAmount(readings) else null
        }

        val events = mutableListOf<DiaryEvent>()
        var i = 0
        while (i < sorted.size) {
            if (dropAt[i] == null) {
                i++
                continue
            }
            var last = i
            while (last + 1 < sorted.size && dropAt[last + 1] != null && !hasLongGap(sorted, last, last + 1)) last++
            val worstDrop = (i..last).maxOf { dropAt[it] ?: 0f }
            events += DiaryEvent(
                startTime = sorted[i].timestamp,
                endTime = endOf(sorted, minutes, last),
                type = DiaryEventType.PRESSURE_DROP,
                summary = String.format(Locale.US, "Pressure falling: %.1f hPa in 3 h", worstDrop),
                avgTemperature = averageTemperature(sorted, i, last),
            )
            i = last + 1
        }
        return events
    }

    private fun eventFor(
        type: DiaryEventType,
        sorted: List<Snapshot>,
        minutes: List<Double>,
        from: Int,
        to: Int,
    ): DiaryEvent {
        val run = sorted.subList(from, to + 1)
        val duration = formatMinutes((from..to).sumOf { minutes[it] }.roundToInt())
        val hasWeather = run.any { !it.weatherPending }
        val avgTemp = averageTemperature(sorted, from, to)
        val summary = when (type) {
            DiaryEventType.SUN ->
                "Outdoors in strong sun, $duration" + if (hasWeather) ", ${avgTemp.roundToInt()} °C" else ""
            DiaryEventType.RAIN -> "Rain while outdoors, $duration"
            // A heat run always has weather (pending snapshots are never HEAT), so the average exists.
            DiaryEventType.HEAT ->
                "Outdoors in the heat, $duration, feels like ${run.map { it.feelsLike }.average().roundToInt()} °C"
            DiaryEventType.OUTDOOR -> "Outdoors, $duration"
            else -> "Indoors, $duration"
        }
        return DiaryEvent(
            startTime = sorted[from].timestamp,
            endTime = endOf(sorted, minutes, to),
            type = type,
            summary = summary,
            avgTemperature = avgTemp,
        )
    }

    private fun endOf(sorted: List<Snapshot>, minutes: List<Double>, index: Int): Long =
        sorted[index].timestamp + (minutes[index] * 60_000).toLong()

    /** Average temperature of the snapshots that have weather; 0 if none of them do. */
    private fun averageTemperature(sorted: List<Snapshot>, from: Int, to: Int): Double =
        sorted.subList(from, to + 1).filter { !it.weatherPending }.map { it.temperature }
            .takeIf { it.isNotEmpty() }?.average() ?: 0.0

    private fun hasLongGap(sorted: List<Snapshot>, earlier: Int, later: Int): Boolean =
        earlier >= 0 && sorted[later].timestamp - sorted[earlier].timestamp > Thresholds.MAX_SNAPSHOT_GAP_MS
}
