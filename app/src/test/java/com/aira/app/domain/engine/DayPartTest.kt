package com.aira.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class DayPartTest {
    @Test
    fun `parts of the day at the boundaries`() {
        assertEquals(DayPart.NIGHT, DayParts.of(4))
        assertEquals(DayPart.MORNING, DayParts.of(5))
        assertEquals(DayPart.MORNING, DayParts.of(11))
        assertEquals(DayPart.AFTERNOON, DayParts.of(12))
        assertEquals(DayPart.AFTERNOON, DayParts.of(16))
        assertEquals(DayPart.EVENING, DayParts.of(17))
        assertEquals(DayPart.EVENING, DayParts.of(20))
        assertEquals(DayPart.NIGHT, DayParts.of(21))
        assertEquals(DayPart.NIGHT, DayParts.of(0))
    }
}
