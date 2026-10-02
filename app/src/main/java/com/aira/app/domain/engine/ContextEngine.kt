package com.aira.app.domain.engine

import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place

/** Decides where the user is (indoor/outdoor) and how they are moving. Pure Kotlin. */
object ContextEngine {

    /**
     * Indoor or outdoor, in plain English:
     * - Phone in a pocket, or it is night, or there is no light reading -> we cannot tell from light.
     * - Light of 1000 lux or more -> OUTDOOR. Light of 300 lux or less -> INDOOR.
     * - In between -> cannot tell.
     * When we cannot tell: walking means OUTDOOR, otherwise we keep the previous place.
     */
    fun classifyPlace(
        lux: Float?,
        inPocket: Boolean?,
        isDaytime: Boolean,
        previousPlace: Place?,
        movement: Movement,
    ): Place {
        val fromLight = placeFromLight(lux, inPocket, isDaytime)
        if (fromLight != Place.UNKNOWN) return fromLight
        return if (movement == Movement.WALKING) Place.OUTDOOR else previousPlace ?: Place.UNKNOWN
    }

    private fun placeFromLight(lux: Float?, inPocket: Boolean?, isDaytime: Boolean): Place = when {
        inPocket == true -> Place.UNKNOWN
        !isDaytime -> Place.UNKNOWN
        lux == null -> Place.UNKNOWN
        lux >= Thresholds.OUTDOOR_LUX -> Place.OUTDOOR
        lux <= Thresholds.INDOOR_LUX -> Place.INDOOR
        else -> Place.UNKNOWN
    }

    /**
     * Movement since the last snapshot:
     * more than 20 steps -> WALKING; few steps but moved more than 1 km -> VEHICLE; otherwise STILL.
     * A missing value counts as zero.
     */
    fun classifyMovement(stepsDelta: Int?, distanceMovedKm: Double?): Movement = when {
        (stepsDelta ?: 0) > Thresholds.WALKING_STEPS -> Movement.WALKING
        (distanceMovedKm ?: 0.0) > Thresholds.VEHICLE_DISTANCE_KM -> Movement.VEHICLE
        else -> Movement.STILL
    }

    /**
     * Steps taken since the previous snapshot. The step counter counts since the phone booted,
     * so if the new total is smaller than the old one the phone restarted and the new total
     * is the number of steps since then. Returns null when either total is unknown.
     */
    fun stepsSince(totalSteps: Long?, previousTotalSteps: Long?): Int? {
        if (totalSteps == null || previousTotalSteps == null) return null
        val delta = if (totalSteps < previousTotalSteps) totalSteps else totalSteps - previousTotalSteps
        return delta.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
