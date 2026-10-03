package com.aira.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizeTest {
    @Test
    fun twoByOne() = assertEquals(WidgetSize.SMALL_2X1, WidgetSize.of(110, 40))

    @Test
    fun twoByTwoAndTaller() {
        assertEquals(WidgetSize.SQUARE_2X2, WidgetSize.of(110, 110))
        assertEquals(WidgetSize.SQUARE_2X2, WidgetSize.of(130, 250))
    }

    @Test
    fun fourByOne() = assertEquals(WidgetSize.WIDE_4X1, WidgetSize.of(250, 40))

    @Test
    fun fourByTwoAndThree() {
        assertEquals(WidgetSize.WIDE_4X2, WidgetSize.of(250, 110))
        assertEquals(WidgetSize.WIDE_4X2, WidgetSize.of(250, 180))
    }

    @Test
    fun fourByFour() = assertEquals(WidgetSize.LARGE_4X4, WidgetSize.of(250, 250))

    @Test
    fun threeColumnsAreTreatedAsWide() = assertEquals(WidgetSize.WIDE_4X2, WidgetSize.of(180, 110))
}
