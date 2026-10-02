package com.aira.app.ui.timeline

import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.DiaryEvent
import com.aira.app.domain.model.DiaryEventType
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.model.WeatherTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineFiltersTest {
    private fun event(id: Long, type: DiaryEventType, temp: Double) =
        TimelineEntry.Event(TimelineItem(DiaryEvent(id, id, id + 1, type, "", temp), emptyList()))

    private val entries = listOf(
        event(1, DiaryEventType.SUN, 34.0),
        event(2, DiaryEventType.RAIN, 28.0),
        event(3, DiaryEventType.INDOOR, 29.0),
        event(4, DiaryEventType.COMMUTE, 27.0),
        event(5, DiaryEventType.PRESSURE_DROP, 0.0),
        TimelineEntry.Alert(WeatherAlert(1, AlertType.HEAT, 10, "Hot")),
        TimelineEntry.TaskDone(WeatherTask(1, "Drink water", completedAt = 11)),
    )

    @Test
    fun `each filter counts its entries`() {
        assertEquals(7, TimelineFilters.count(entries, TimelineFilter.ALL))
        assertEquals(2, TimelineFilters.count(entries, TimelineFilter.OUTDOORS))
        assertEquals(1, TimelineFilters.count(entries, TimelineFilter.INDOORS))
        assertEquals(1, TimelineFilters.count(entries, TimelineFilter.COMMUTE))
        assertEquals(2, TimelineFilters.count(entries, TimelineFilter.ALERTS))
    }

    @Test
    fun `summary skips events without a temperature`() {
        val summary = TimelineFilters.summary(entries)
        assertEquals(34.0, summary.highC!!, 0.001)
        assertEquals(27.0, summary.lowC!!, 0.001)
        assertEquals(29.5, summary.avgC!!, 0.001)
        assertEquals(1, summary.rainEvents)
        assertEquals(5, summary.chapters)
        assertEquals(2L, summary.firstRainAt)
        assertEquals(0, summary.firstRainMinutes) // the test event lasts 1 ms
    }

    @Test
    fun `an empty day has no temperatures`() {
        val summary = TimelineFilters.summary(emptyList())
        assertNull(summary.highC)
        assertNull(summary.avgC)
        assertEquals(0, summary.chapters)
        assertNull(summary.firstRainAt)
    }
}
