package com.aira.app.domain.engine

import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place

enum class UvBand { LOW, MODERATE, HIGH }

/** Rules for what weather the user was exposed to. Pure Kotlin. */
object ExposureRules {

    /** Sun exposure: outdoors and brighter than 30,000 lux. */
    fun isSunExposure(place: Place, lux: Float?): Boolean =
        place == Place.OUTDOOR && lux != null && lux > Thresholds.STRONG_SUN_LUX

    /** Heat exposure: outdoors or walking, and it feels hotter than 33 °C. */
    fun isHeatExposure(place: Place, movement: Movement, feelsLikeC: Double): Boolean =
        isExposedToWeather(place, movement) && feelsLikeC > Thresholds.HEAT_FEELS_LIKE_C

    /** Rain encounter: outdoors or walking, and more than 0.2 mm of rain in that hour. */
    fun isRainEncounter(place: Place, movement: Movement, rainMm: Double): Boolean =
        isExposedToWeather(place, movement) && rainMm > Thresholds.RAIN_MM

    /** UV dose of one stretch outdoors: UV index times minutes. Add these up over the day. */
    fun uvDose(uvIndex: Double, outdoorMinutes: Double): Double = uvIndex * outdoorMinutes

    /** Low below 100, Moderate from 100 up to 300, High from 300. */
    fun uvDoseBand(dose: Double): UvBand = when {
        dose >= Thresholds.UV_DOSE_HIGH -> UvBand.HIGH
        dose >= Thresholds.UV_DOSE_MODERATE -> UvBand.MODERATE
        else -> UvBand.LOW
    }

    private fun isExposedToWeather(place: Place, movement: Movement) =
        place == Place.OUTDOOR || movement == Movement.WALKING
}
