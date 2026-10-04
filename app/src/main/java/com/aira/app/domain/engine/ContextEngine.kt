package com.aira.app.domain.engine

import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place

/** Decides where the user is (indoor/outdoor) and how they are moving. Pure Kotlin. */
object ContextEngine {

    /**
     * Indoor or outdoor, in plain English:
     * - Phone in a pocket, or it is night, or there is no light reading -> we cannot tell from light.
     * - Light of 1000 lux or more -> OUTDOOR (5000 lux on a clear, sunny day, because a room by a window can be
     *   brighter than 1000 lux but outdoors in the sun is much brighter still). Light of 300 lux or less -> INDOOR.
     * - In between -> cannot tell.
     * When we cannot tell: a real walk outside -> OUTDOOR. A real walk is a steady one (about one step a second or
     * more, [stepsPerMinute]) or one that moved the user at least 200 m ([movedKm]); a few steps around the house
     * are not. Otherwise we keep the previous place.
     */
    fun classifyPlace(
        lux: Float?,
        inPocket: Boolean?,
        isDaytime: Boolean,
        previousPlace: Place?,
        movement: Movement,
        stepsPerMinute: Double? = null,
        movedKm: Double? = null,
        clearSky: Boolean = false,
    ): Place {
        val fromLight = placeFromLight(lux, inPocket, isDaytime, clearSky)
        if (fromLight != Place.UNKNOWN) return fromLight
        val realWalk = movement == Movement.WALKING &&
            ((stepsPerMinute ?: 0.0) >= Thresholds.STEADY_WALK_STEPS_PER_MIN || (movedKm ?: 0.0) >= Thresholds.LEFT_BUILDING_KM)
        return if (realWalk) Place.OUTDOOR else previousPlace ?: Place.UNKNOWN
    }

    private fun placeFromLight(lux: Float?, inPocket: Boolean?, isDaytime: Boolean, clearSky: Boolean): Place = when {
        inPocket == true -> Place.UNKNOWN
        !isDaytime -> Place.UNKNOWN
        lux == null -> Place.UNKNOWN
        lux >= (if (clearSky) Thresholds.OUTDOOR_LUX_CLEAR_SKY else Thresholds.OUTDOOR_LUX) -> Place.OUTDOOR
        lux <= Thresholds.INDOOR_LUX -> Place.INDOOR
        else -> Place.UNKNOWN
    }

    /**
     * Movement since the last snapshot:
     * more than 20 steps -> WALKING; few steps but moved more than 1 km at 10 km/h or faster on average -> VEHICLE;
     * otherwise STILL. The speed check stops a jump in the (coarse) location, or a slow walk without step counts over
     * a long gap, from looking like a car ride. When [minutes] is unknown only the distance counts.
     * A missing value counts as zero.
     */
    fun classifyMovement(stepsDelta: Int?, distanceMovedKm: Double?, minutes: Double? = null): Movement {
        val distance = distanceMovedKm ?: 0.0
        val fastEnough = minutes == null || minutes <= 0 || distance / (minutes / 60) >= Thresholds.VEHICLE_MIN_SPEED_KMH
        return when {
            (stepsDelta ?: 0) > Thresholds.WALKING_STEPS -> Movement.WALKING
            distance > Thresholds.VEHICLE_DISTANCE_KM && fastEnough -> Movement.VEHICLE
            else -> Movement.STILL
        }
    }

    /** Average steps per minute between two snapshots, or null when it cannot be worked out. */
    fun stepsPerMinute(stepsDelta: Int?, minutes: Double?): Double? =
        if (stepsDelta == null || minutes == null || minutes <= 0) null else stepsDelta / minutes

    /**
     * One light value from several readings: the first one is skipped when there are three or more (many phones
     * first report an old or zero value), and the median of the rest is used, so a hand passing over the sensor or
     * a single odd value does not decide indoor or outdoor.
     */
    fun typicalLux(samples: List<Float>): Float? {
        if (samples.isEmpty()) return null
        val used = (if (samples.size >= 3) samples.drop(1) else samples).sorted()
        val middle = used.size / 2
        return if (used.size % 2 == 1) used[middle] else (used[middle - 1] + used[middle]) / 2
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
