package com.aira.app.domain.engine

import kotlin.math.pow
import kotlin.math.roundToInt

/** Altitude from air pressure. */
object Altitude {
    /** A barometer reading older than this is not used for "your altitude now". */
    const val MAX_READING_AGE_MS = 30 * 60 * 1000L

    /** The average sea-level pressure, used when today's value for where the user is not known. */
    const val STANDARD_SEA_LEVEL_HPA = 1013.25

    /**
     * Height above sea level in metres from the phone's barometer [pressureHpa] and today's sea-level pressure
     * [seaLevelHpa] (from the weather service). Air pressure falls by about 1 hPa for every 8 m you climb; this is
     * the standard formula h = 44330 × (1 − (p / p0)^(1 / 5.255)), the same one Android uses.
     * Null when a value is missing or not a real pressure.
     */
    fun fromPressure(pressureHpa: Double?, seaLevelHpa: Double?): Int? {
        if (pressureHpa == null || seaLevelHpa == null || pressureHpa <= 0 || seaLevelHpa <= 0) return null
        return (44_330.0 * (1 - (pressureHpa / seaLevelHpa).pow(1 / 5.255))).roundToInt()
    }

    /** Metres to feet, for people who use °F. */
    fun toFeet(metres: Int): Int = (metres * 3.28084).roundToInt()

    /** Feet to metres, for a height typed in feet. */
    fun toMetres(feet: Int): Int = (feet / 3.28084).roundToInt()

    /**
     * Calibration: the user says their real height, [actualMetres], while the phone measured [measuredMetres] (before
     * any calibration). The difference is saved and added to every later reading, which removes the phone's own
     * barometer error. Weather changes are still handled by the sea-level pressure, so the offset stays valid.
     */
    fun offsetFor(measuredMetres: Int, actualMetres: Int): Int = actualMetres - measuredMetres
}
