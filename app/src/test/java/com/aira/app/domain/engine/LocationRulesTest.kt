package com.aira.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationRulesTest {
    @Test
    fun `at most five places can be saved`() {
        assertTrue(LocationRules.canAdd(0))
        assertTrue(LocationRules.canAdd(4))
        assertFalse(LocationRules.canAdd(5))
        assertFalse(LocationRules.canAdd(6))
    }

    @Test
    fun `coordinates are rounded to two decimals`() {
        assertEquals(22.57, LocationRules.round2(22.5726), 0.0)
        assertEquals(88.36, LocationRules.round2(88.3639), 0.0)
        assertEquals(-33.87, LocationRules.round2(-33.8688), 0.0)
    }

    @Test
    fun `names are trimmed, shortened, and never blank`() {
        assertEquals("College", LocationRules.cleanName("  College ", "Pin"))
        assertEquals("Howrah", LocationRules.cleanName("   ", " Howrah "))
        assertEquals(LocationRules.MAX_NAME_LENGTH, LocationRules.cleanName("x".repeat(50), "Pin").length)
    }
}
