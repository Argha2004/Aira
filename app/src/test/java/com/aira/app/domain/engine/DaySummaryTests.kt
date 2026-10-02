package com.aira.app.domain.engine

import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A day summary with just the numbers a test cares about. */
internal fun day(
    date: LocalDate,
    sun: Int = 0,
    rain: Int = 0,
    heat: Int = 0,
    outdoor: Int = 0,
    hot: Int = 0,
    avgFeels: Double? = null,
    walk: Double? = null,
) = DaySummary(
    date = date,
    stats = DailyStats(sun, heat, rain, UvBand.LOW, outdoor, 0),
    avgOutdoorFeelsLike = avgFeels,
    hotOutdoorMinutes = hot,
    maxWalkFeelsLike = walk,
)

class DayAggregatorTest {

    private val utc = ZoneOffset.UTC
    private fun millis(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 9, day, hour, minute).toInstant(utc).toEpochMilli()

    @Test
    fun `no snapshots give no summaries`() = assertTrue(DayAggregator.summarize(emptyList(), utc).isEmpty())

    @Test
    fun `snapshots are grouped by local day, oldest day first`() {
        val summaries = DayAggregator.summarize(
            listOf(snap(0, atMillis = millis(29, 10)), snap(0, atMillis = millis(28, 23)), snap(0, atMillis = millis(29, 11))),
            utc,
        )
        assertEquals(listOf(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 29)), summaries.map { it.date })
    }

    @Test
    fun `day weather covers every snapshot with weather, indoors too`() {
        val summary = DayAggregator.summarize(
            listOf(
                snap(0, Place.INDOOR, temp = 26.0, feels = 28.0, atMillis = millis(29, 9)),
                snap(0, Place.OUTDOOR, temp = 32.0, feels = 36.0, atMillis = millis(29, 13)),
                snap(0, Place.OUTDOOR, temp = 99.0, feels = 99.0, pending = true, atMillis = millis(29, 14)),
            ),
            utc,
        ).single()
        val day = summary.day!!
        assertEquals(29.0, day.avgTempC, 0.001)
        assertEquals(32.0, day.maxTempC, 0.001)
        assertEquals(26.0, day.minTempC, 0.001)
        assertEquals(32.0, day.avgFeelsLikeC, 0.001)
        assertEquals(8.0, day.avgWindKmh, 0.001)
    }

    @Test
    fun `a day with only snapshots waiting for weather has no day weather`() {
        val summary = DayAggregator.summarize(listOf(snap(0, pending = true, atMillis = millis(29, 9))), utc).single()
        assertNull(summary.day)
    }

    @Test
    fun `the day is decided by the zone, not by UTC`() {
        // 22:00 UTC on the 28th is already the 29th in Kolkata (+05:30).
        val kolkata = java.time.ZoneId.of("Asia/Kolkata")
        val summary = DayAggregator.summarize(listOf(snap(0, atMillis = millis(28, 22))), kolkata).single()
        assertEquals(LocalDate.of(2026, 9, 29), summary.date)
    }

    @Test
    fun `outdoor feels like is averaged over outdoor snapshots only`() {
        val summary = DayAggregator.summarize(
            listOf(
                snap(0, Place.OUTDOOR, feels = 30.0, atMillis = millis(29, 10)),
                snap(0, Place.OUTDOOR, feels = 36.0, atMillis = millis(29, 11)),
                snap(0, Place.INDOOR, feels = 20.0, atMillis = millis(29, 12)),
            ),
            utc,
        ).single()
        assertEquals(33.0, summary.avgOutdoorFeelsLike!!, 0.0)
    }

    @Test
    fun `no outdoor time gives no outdoor temperature`() {
        val summary = DayAggregator.summarize(listOf(snap(0, atMillis = millis(29, 10))), utc).single()
        assertNull(summary.avgOutdoorFeelsLike)
    }

    @Test
    fun `hot outdoor minutes count outdoor time above 35`() {
        val summary = DayAggregator.summarize(
            listOf(
                snap(0, Place.OUTDOOR, feels = 36.0, atMillis = millis(29, 10)),
                snap(0, Place.OUTDOOR, feels = 35.0, atMillis = millis(29, 10, 30)), // exactly 35 is not above
                snap(0, Place.INDOOR, feels = 40.0, atMillis = millis(29, 11)),
            ),
            utc,
        ).single()
        assertEquals(30, summary.hotOutdoorMinutes)
    }

    @Test
    fun `hottest walk is the highest feels like while walking`() {
        val summary = DayAggregator.summarize(
            listOf(
                snap(0, Place.OUTDOOR, Movement.WALKING, feels = 34.0, atMillis = millis(29, 10)),
                snap(0, Place.OUTDOOR, Movement.WALKING, feels = 39.0, atMillis = millis(29, 11)),
                snap(0, Place.OUTDOOR, Movement.STILL, feels = 45.0, atMillis = millis(29, 12)),
            ),
            utc,
        ).single()
        assertEquals(39.0, summary.maxWalkFeelsLike!!, 0.0)
    }

    @Test
    fun `snapshots waiting for weather add no temperatures`() {
        val summary = DayAggregator.summarize(
            listOf(snap(0, Place.OUTDOOR, Movement.WALKING, feels = 0.0, pending = true, atMillis = millis(29, 10))),
            utc,
        ).single()
        assertNull(summary.avgOutdoorFeelsLike)
        assertNull(summary.maxWalkFeelsLike)
        assertEquals(0, summary.hotOutdoorMinutes)
    }
}

class DayClassifierTest {
    private val date = LocalDate.of(2026, 9, 29)

    @Test fun `no summary is no data`() = assertEquals(DayCondition.NO_DATA, DayClassifier.classify(null))

    @Test fun `any rain encounter is rainy`() =
        assertEquals(DayCondition.RAINY, DayClassifier.classify(day(date, rain = 1)))

    @Test fun `rain wins over heat and sun`() =
        assertEquals(DayCondition.RAINY, DayClassifier.classify(day(date, rain = 1, heat = 200, sun = 200)))

    @Test fun `60 minutes of heat is hot`() =
        assertEquals(DayCondition.HOT, DayClassifier.classify(day(date, heat = 60)))

    @Test fun `59 minutes of heat is not hot`() =
        assertEquals(DayCondition.MOSTLY_INDOORS, DayClassifier.classify(day(date, heat = 59)))

    @Test fun `heat wins over sun`() =
        assertEquals(DayCondition.HOT, DayClassifier.classify(day(date, heat = 90, sun = 90)))

    @Test fun `30 minutes of sun is sunny`() =
        assertEquals(DayCondition.SUNNY, DayClassifier.classify(day(date, sun = 30)))

    @Test fun `29 minutes of sun is not sunny`() =
        assertEquals(DayCondition.MOSTLY_INDOORS, DayClassifier.classify(day(date, sun = 29)))

    @Test fun `a day with data but nothing special is mostly indoors`() =
        assertEquals(DayCondition.MOSTLY_INDOORS, DayClassifier.classify(day(date)))
}
