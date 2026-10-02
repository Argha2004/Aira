package com.aira.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class HeadingTest {
    @Test
    fun `angles are brought into 0 to 360`() {
        assertEquals(270f, Heading.normalize(-90f), 0.001f)
        assertEquals(10f, Heading.normalize(370f), 0.001f)
        assertEquals(0f, Heading.normalize(360f), 0.001f)
    }

    @Test
    fun `the first reading is taken as it is`() = assertEquals(45f, Heading.smooth(null, 45f), 0.001f)

    @Test
    fun `smoothing moves part of the way`() = assertEquals(10f, Heading.smooth(0f, 100f, alpha = 0.1f), 0.001f)

    @Test
    fun `smoothing takes the short way round north`() {
        assertEquals(355f, Heading.smooth(350f, 10f, alpha = 0.25f), 0.001f) // 20° apart: moves 5°
        assertEquals(355f, Heading.smooth(5f, 345f, alpha = 0.5f), 0.001f)
    }
}
