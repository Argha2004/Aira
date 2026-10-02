package com.aira.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationShortTest {
    @Test
    fun `compact durations`() {
        assertEquals("0m", formatMinutesShort(0))
        assertEquals("42m", formatMinutesShort(42))
        assertEquals("1h", formatMinutesShort(60))
        assertEquals("1h 20m", formatMinutesShort(80))
        assertEquals("26h 5m", formatMinutesShort(1565))
    }
}
