package com.aira.app.widget

/** The layouts of the weather widget: from a small 2 x 1 tile to a big 4 x 4 card. */
enum class WidgetSize {
    /** Icon, temperature and condition. */
    SMALL_2X1,

    /** A centred card: location, icon, temperature, condition, high and low. */
    SQUARE_2X2,

    /** One wide row. */
    WIDE_4X1,

    /** The main widget: today's weather and the next five hours. */
    WIDE_4X2,

    /** Everything: today's weather, details and the next six hours. */
    LARGE_4X4;

    companion object {
        /**
         * Picks the layout for the space the home screen gives the widget, in dp. Android's rule of thumb is that a
         * widget n cells wide is about 70 × n − 30 dp, so n = (size + 30) / 70.
         */
        fun of(widthDp: Int, heightDp: Int): WidgetSize {
            val columns = (widthDp + 30) / 70
            val rows = (heightDp + 30) / 70
            return when {
                columns <= 2 -> if (rows <= 1) SMALL_2X1 else SQUARE_2X2
                rows <= 1 -> WIDE_4X1
                rows <= 3 -> WIDE_4X2
                else -> LARGE_4X4
            }
        }
    }
}
