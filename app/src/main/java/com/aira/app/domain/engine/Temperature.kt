package com.aira.app.domain.engine

import kotlin.math.roundToInt

enum class TemperatureUnit(val symbol: String) { CELSIUS("°C"), FAHRENHEIT("°F") }

/** Temperatures are stored in °C; this converts for display only. */
object Temperature {

    fun toFahrenheit(celsius: Double): Double = celsius * 9 / 5 + 32

    /** The number to show, rounded, in the chosen unit. */
    fun value(celsius: Double, unit: TemperatureUnit): Int =
        (if (unit == TemperatureUnit.FAHRENHEIT) toFahrenheit(celsius) else celsius).roundToInt()

    /** For example "34°C" or "93°F". */
    fun format(celsius: Double, unit: TemperatureUnit): String = "${value(celsius, unit)}${unit.symbol}"

    private val celsiusInText = Regex("(-?\\d+) °C")

    /**
     * Converts temperatures written like "34 °C" inside a sentence (diary summaries and insights are
     * written in °C) to Fahrenheit. In Celsius mode the text is returned unchanged.
     */
    fun convertText(text: String, unit: TemperatureUnit): String =
        if (unit == TemperatureUnit.CELSIUS) {
            text
        } else {
            celsiusInText.replace(text) { match ->
                "${value(match.groupValues[1].toDouble(), unit)} ${unit.symbol}"
            }
        }
}
