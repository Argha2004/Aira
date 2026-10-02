package com.aira.app.domain.engine

import com.aira.app.domain.model.Commute
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightGeneratorTest {

    // Sunday 4 October 2026: this week is Mon 28 Sep - Sun 4 Oct, last week is Mon 21 - Sun 27 Sep.
    private val today = LocalDate.of(2026, 10, 4)
    private fun sep(day: Int) = LocalDate.of(2026, 9, day)

    private fun commutes(total: Int, rainy: Int) = List(total) {
        Commute(fromPlace = "Home", toPlace = "College", startTime = it.toLong(), endTime = it + 1L,
            hadRain = it < rainy, maxFeelsLike = 33.0)
    }

    private fun generate(days: List<DaySummary> = emptyList(), commutes: List<Commute> = emptyList()) =
        InsightGenerator.generate(days, commutes, today)

    // ---- sun this week vs last week ----

    @Test
    fun `more sun this week`() {
        val days = listOf(day(sep(21), sun = 50), day(sep(22), sun = 50), day(sep(28), sun = 70), day(sep(29), sun = 70))
        assertTrue("You got 40% more sun this week than last week." in generate(days))
    }

    @Test
    fun `less sun this week`() {
        val days = listOf(day(sep(21), sun = 60), day(sep(22), sun = 60), day(sep(28), sun = 30), day(sep(29), sun = 30))
        assertTrue("You got 50% less sun this week than last week." in generate(days))
    }

    @Test
    fun `about the same sun`() {
        val days = listOf(day(sep(21), sun = 50), day(sep(22), sun = 50), day(sep(28), sun = 52), day(sep(29), sun = 52))
        assertTrue("You got about the same amount of sun this week as last week." in generate(days))
    }

    @Test
    fun `sun is compared per recorded day so a short week is fair`() {
        // Last week: 7 days of 10 min. This week: only 2 days but 20 min each (double the daily sun).
        val last = (21..27).map { day(sep(it), sun = 10) }
        val days = last + listOf(day(sep(28), sun = 20), day(sep(29), sun = 20))
        assertTrue("You got 100% more sun this week than last week." in generate(days))
    }

    @Test
    fun `too little data this week gives no sun sentence`() {
        val days = listOf(day(sep(21), sun = 50), day(sep(22), sun = 50), day(sep(28), sun = 70))
        assertTrue(generate(days).none { "sun" in it })
    }

    @Test
    fun `no sun last week gives no sun sentence`() {
        val days = listOf(day(sep(21)), day(sep(22)), day(sep(28), sun = 70), day(sep(29), sun = 70))
        assertTrue(generate(days).none { "sun" in it })
    }

    // ---- rainy commutes ----

    @Test
    fun `rain on some commutes`() =
        assertTrue("It rained on 5 of your 18 commutes this month." in generate(commutes = commutes(18, 5)))

    @Test
    fun `no rain on any commute`() =
        assertTrue("None of your 18 commutes were in the rain this month." in generate(commutes = commutes(18, 0)))

    @Test
    fun `no commutes gives no commute sentence`() = assertTrue(generate().none { "commute" in it })

    // ---- hottest day ----

    @Test
    fun `hottest day of the week is named with its hot hours`() {
        val days = listOf(day(sep(28), hot = 60), day(sep(29), hot = 180))
        // 29 September 2026 is a Tuesday.
        assertTrue("Your hottest day was Tuesday: 3 h outdoors above 35 °C." in generate(days))
    }

    @Test
    fun `less than 30 hot minutes is not worth a sentence`() {
        assertTrue(generate(listOf(day(sep(29), hot = 20))).none { "hottest" in it })
    }

    @Test
    fun `hot days of last week are ignored`() {
        assertTrue(generate(listOf(day(sep(22), hot = 300))).none { "hottest" in it })
    }

    // ---- outdoor time ----

    @Test
    fun `outdoor time this week`() {
        val days = listOf(day(sep(28), outdoor = 80), day(sep(29), outdoor = 40))
        assertTrue("You spent 2 h outdoors this week." in generate(days))
    }

    @Test
    fun `under 30 outdoor minutes gives no sentence`() =
        assertTrue(generate(listOf(day(sep(29), outdoor = 20))).none { "outdoors this week" in it })

    @Test
    fun `no data at all gives no sentences`() = assertEquals(emptyList<String>(), generate())
}
