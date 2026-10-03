package com.aira.app.domain.model

/** Turns a WMO weather code (as sent by Open-Meteo) into a short English description. */
object WeatherCode {
    fun describe(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81 -> "Rain showers"
        82 -> "Violent rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    /** A weather symbol for [code] (for the home-screen widget); the moon for clear nights. */
    fun emoji(code: Int, isDay: Boolean): String = when (code) {
        0, 1 -> if (isDay) "\u2600\uFE0F" else "\uD83C\uDF19"
        2 -> if (isDay) "\u26C5" else "\u2601\uFE0F"
        3 -> "\u2601\uFE0F"
        45, 48 -> "\uD83C\uDF2B\uFE0F"
        51, 53, 55, 56, 57 -> "\uD83C\uDF26\uFE0F"
        61, 63, 65, 66, 67, 80, 81, 82 -> "\uD83C\uDF27\uFE0F"
        71, 73, 75, 77, 85, 86 -> "\u2744\uFE0F"
        95, 96, 99 -> "\u26C8\uFE0F"
        else -> "\uD83C\uDF21\uFE0F"
    }
}
