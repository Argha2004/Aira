package com.aira.app.domain.engine

import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextEngineTest {

    private fun place(
        lux: Float? = null,
        inPocket: Boolean? = false,
        day: Boolean = true,
        previous: Place? = null,
        movement: Movement = Movement.STILL,
    ) = ContextEngine.classifyPlace(lux, inPocket, day, previous, movement)

    // ---- classifyPlace: light rules ----

    @Test
    fun `lux exactly 1000 is outdoor`() = assertEquals(Place.OUTDOOR, place(lux = 1000f))

    @Test
    fun `lux just under 1000 is unknown so the previous place is kept`() =
        assertEquals(Place.INDOOR, place(lux = 999f, previous = Place.INDOOR))

    @Test
    fun `lux exactly 300 is indoor`() = assertEquals(Place.INDOOR, place(lux = 300f))

    @Test
    fun `lux just over 300 is unknown so the previous place is kept`() =
        assertEquals(Place.OUTDOOR, place(lux = 301f, previous = Place.OUTDOOR))

    @Test
    fun `unknown with no previous place stays unknown`() =
        assertEquals(Place.UNKNOWN, place(lux = 500f, previous = null))

    @Test
    fun `phone not in pocket does not block the light rule`() =
        assertEquals(Place.OUTDOOR, place(lux = 20_000f, inPocket = false))

    // ---- classifyPlace: cases where light cannot be trusted ----

    @Test
    fun `in pocket ignores bright light and keeps previous place`() =
        assertEquals(Place.INDOOR, place(lux = 50_000f, inPocket = true, previous = Place.INDOOR))

    @Test
    fun `night ignores light and keeps previous place`() =
        assertEquals(Place.INDOOR, place(lux = 5000f, day = false, previous = Place.INDOOR))

    @Test
    fun `missing light sensor keeps previous place`() =
        assertEquals(Place.OUTDOOR, place(lux = null, previous = Place.OUTDOOR))

    @Test
    fun `missing proximity sensor does not block the light rule`() =
        assertEquals(Place.INDOOR, place(lux = 100f, inPocket = null))

    // ---- classifyPlace: resolving unknown by movement ----

    @Test
    fun `unknown while walking becomes outdoor`() =
        assertEquals(Place.OUTDOOR, place(lux = 500f, previous = Place.INDOOR, movement = Movement.WALKING))

    @Test
    fun `in pocket while walking becomes outdoor`() =
        assertEquals(Place.OUTDOOR, place(lux = null, inPocket = true, movement = Movement.WALKING))

    @Test
    fun `unknown in a vehicle keeps previous place`() =
        assertEquals(Place.INDOOR, place(lux = 500f, previous = Place.INDOOR, movement = Movement.VEHICLE))

    @Test
    fun `a clear light reading wins over walking`() =
        assertEquals(Place.INDOOR, place(lux = 100f, movement = Movement.WALKING))

    // ---- classifyMovement ----

    @Test
    fun `21 steps is walking`() = assertEquals(Movement.WALKING, ContextEngine.classifyMovement(21, 0.0))

    @Test
    fun `exactly 20 steps is not walking`() = assertEquals(Movement.STILL, ContextEngine.classifyMovement(20, 0.0))

    @Test
    fun `few steps but more than 1 km is a vehicle`() =
        assertEquals(Movement.VEHICLE, ContextEngine.classifyMovement(3, 1.5))

    @Test
    fun `exactly 1 km is still`() = assertEquals(Movement.STILL, ContextEngine.classifyMovement(0, 1.0))

    @Test
    fun `walking wins over distance`() = assertEquals(Movement.WALKING, ContextEngine.classifyMovement(500, 3.0))

    @Test
    fun `missing steps and distance is still`() = assertEquals(Movement.STILL, ContextEngine.classifyMovement(null, null))

    @Test
    fun `missing step counter but large distance is a vehicle`() =
        assertEquals(Movement.VEHICLE, ContextEngine.classifyMovement(null, 2.0))

    // ---- stepsSince ----

    @Test
    fun `steps since is the difference of the totals`() = assertEquals(150, ContextEngine.stepsSince(1150, 1000))

    @Test
    fun `no new steps gives zero`() = assertEquals(0, ContextEngine.stepsSince(1000, 1000))

    @Test
    fun `step counter reset after reboot uses the new total`() = assertEquals(40, ContextEngine.stepsSince(40, 90_000))

    @Test
    fun `missing total gives null`() {
        assertNull(ContextEngine.stepsSince(null, 100))
        assertNull(ContextEngine.stepsSince(100, null))
    }
}
