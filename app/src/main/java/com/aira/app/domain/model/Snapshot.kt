package com.aira.app.domain.model

/** Where the user probably was. */
enum class Place { INDOOR, OUTDOOR, UNKNOWN }

/** How the user was moving between two snapshots. */
enum class Movement { STILL, WALKING, VEHICLE }

/**
 * One reading of the user's surroundings: location, weather and phone sensors.
 * Temperatures in °C, [timestamp] in epoch millis. Sensor values are null when the sensor is missing.
 */
data class Snapshot(
    val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val rainMm: Double,
    val uvIndex: Double,
    val windSpeed: Double,
    val lux: Float? = null,
    val stepsDelta: Int? = null,
    val totalSteps: Long? = null,
    val pressure: Float? = null,
    val inPocket: Boolean? = null,
    val place: Place,
    val movement: Movement,
    /** True when the weather could not be fetched; the weather numbers are placeholders until filled in. */
    val weatherPending: Boolean = false,
) {
    val isOutdoor: Boolean get() = place == Place.OUTDOOR
    val isMoving: Boolean get() = movement != Movement.STILL
}
