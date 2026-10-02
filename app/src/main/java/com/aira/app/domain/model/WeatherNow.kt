package com.aira.app.domain.model

/** Current weather at a location. Temperatures in °C, wind in km/h, times in epoch millis. */
data class WeatherNow(
    val tempC: Double,
    val feelsLikeC: Double,
    val humidity: Int,
    val rainMm: Double,
    val uvIndex: Double,
    val windKmh: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val sunrise: Long?,
    val sunset: Long?,
    /** Today's highest and lowest temperature, and sea-level pressure; null when the API did not send them. */
    val highC: Double? = null,
    val lowC: Double? = null,
    val pressureHpa: Double? = null,
    /** Where the wind comes from, in degrees (0 = north, 90 = east); null if unknown. */
    val windDirection: Int? = null,
    /** Height of the ground here above sea level, in metres; null if unknown. */
    val elevationM: Double? = null,
)

/** Air quality now: the US air quality index (0 to 500, lower is better) and fine dust in µg/m³. */
data class AirQuality(val aqi: Int, val pm25: Double)
