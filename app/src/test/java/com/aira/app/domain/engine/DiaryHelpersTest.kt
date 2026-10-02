package com.aira.app.domain.engine

import com.aira.app.domain.model.DiaryEvent
import com.aira.app.domain.model.DiaryEventType
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryMergeTest {

    private fun event(id: Long, type: DiaryEventType, start: Long, end: Long = start + 10, summary: String = "s") =
        DiaryEvent(id, start, end, type, summary, 30.0)

    @Test
    fun `identical events need no change`() {
        val plan = DiaryMerge.plan(
            existing = listOf(event(7, DiaryEventType.INDOOR, 0)),
            fresh = listOf(event(0, DiaryEventType.INDOOR, 0)),
        )
        assertTrue(plan.insert.isEmpty() && plan.update.isEmpty() && plan.deleteIds.isEmpty())
    }

    @Test
    fun `an event that grew is updated and keeps its id`() {
        val plan = DiaryMerge.plan(
            existing = listOf(event(7, DiaryEventType.INDOOR, 0, end = 10)),
            fresh = listOf(event(0, DiaryEventType.INDOOR, 0, end = 40, summary = "longer")),
        )
        assertEquals(1, plan.update.size)
        assertEquals(7L, plan.update[0].id)
        assertEquals(40L, plan.update[0].endTime)
        assertTrue(plan.insert.isEmpty() && plan.deleteIds.isEmpty())
    }

    @Test
    fun `a new event is inserted`() {
        val plan = DiaryMerge.plan(emptyList(), listOf(event(0, DiaryEventType.SUN, 100)))
        assertEquals(1, plan.insert.size)
    }

    @Test
    fun `an event that disappeared is deleted`() {
        val plan = DiaryMerge.plan(listOf(event(7, DiaryEventType.SUN, 100)), emptyList())
        assertEquals(listOf(7L), plan.deleteIds)
    }

    @Test
    fun `an event whose type changed is replaced`() {
        val plan = DiaryMerge.plan(
            existing = listOf(event(7, DiaryEventType.INDOOR, 0)),
            fresh = listOf(event(0, DiaryEventType.RAIN, 0)),
        )
        assertEquals(1, plan.insert.size)
        assertEquals(listOf(7L), plan.deleteIds)
    }
}

class FormatMinutesTest {
    @Test fun `under an hour`() = assertEquals("45 min", formatMinutes(45))
    @Test fun `zero minutes`() = assertEquals("0 min", formatMinutes(0))
    @Test fun `whole hours`() = assertEquals("2 h", formatMinutes(120))
    @Test fun `hours and minutes`() = assertEquals("1 h 20 min", formatMinutes(80))
}

class DayRangeTest {
    private val date = LocalDate.of(2026, 9, 29)

    @Test
    fun `a day in UTC covers exactly 24 hours`() {
        val range = DayRange.of(date, ZoneOffset.UTC)
        assertEquals(24 * 60 * 60 * 1000L, range.last - range.first + 1)
    }

    @Test
    fun `the next day starts right after this one ends`() {
        val today = DayRange.of(date, ZoneId.of("Asia/Kolkata"))
        val tomorrow = DayRange.of(date.plusDays(1), ZoneId.of("Asia/Kolkata"))
        assertEquals(today.last + 1, tomorrow.first)
    }

    @Test
    fun `range starts at local midnight`() {
        val range = DayRange.of(date, ZoneId.of("Asia/Kolkata"))
        // 2026-09-29 00:00 in Kolkata (+05:30) is 2026-09-28 18:30 UTC.
        val expected = java.time.LocalDateTime.of(2026, 9, 28, 18, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(expected, range.first)
    }
}

class PressureDropAmountTest {
    @Test
    fun `drop amount is highest in the window minus newest`() {
        val hour = 3_600_000L
        assertEquals(3.5f, PressureTrend.dropAmount(listOf(0L to 1012f, hour to 1010f, 2 * hour to 1008.5f)), 0.001f)
    }

    @Test
    fun `fewer than two readings is zero`() = assertEquals(0f, PressureTrend.dropAmount(listOf(0L to 1010f)), 0f)
}
