package com.aira.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WeatherCodeEmojiTest {
    @Test
    fun clearSky_isSunByDayAndMoonByNight() {
        assertNotEquals(WeatherCode.emoji(0, true), WeatherCode.emoji(0, false))
    }

    @Test
    fun rainCodes_shareOneSymbol() {
        assertEquals(WeatherCode.emoji(61, true), WeatherCode.emoji(82, false))
    }

    @Test
    fun everyCode_hasASymbol() {
        for (code in listOf(0, 1, 2, 3, 45, 51, 56, 61, 66, 71, 80, 85, 95, 99, 1234)) {
            assert(WeatherCode.emoji(code, true).isNotEmpty())
        }
    }
}
