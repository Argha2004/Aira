package com.aira.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AltitudeTest {
    @Test
    fun `same pressure as at sea level is altitude zero`() = assertEquals(0, Altitude.fromPressure(1013.25, 1013.25))

    @Test
    fun `about 12 hPa less is about 100 metres up`() {
        val metres = Altitude.fromPressure(1001.3, 1013.25)!!
        assertTrue("was $metres", metres in 95..105)
    }

    @Test
    fun `850 hPa is about 1450 metres up`() {
        val metres = Altitude.fromPressure(850.0, 1013.25)!!
        assertTrue("was $metres", metres in 1430..1470)
    }

    @Test
    fun `higher pressure than at sea level is below sea level`() = assertTrue(Altitude.fromPressure(1020.0, 1013.25)!! < 0)

    @Test
    fun `missing or impossible values give no altitude`() {
        assertNull(Altitude.fromPressure(null, 1013.25))
        assertNull(Altitude.fromPressure(1000.0, null))
        assertNull(Altitude.fromPressure(0.0, 1013.25))
    }

    @Test
    fun `metres to feet`() {
        assertEquals(328, Altitude.toFeet(100))
        assertEquals(0, Altitude.toFeet(0))
    }
}
