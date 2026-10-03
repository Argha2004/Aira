package com.aira.app.domain.engine

import kotlin.math.cos
import kotlin.math.floor

/** The eight phases of the moon, from new moon round to waning crescent. */
enum class MoonPhase {
    NEW, WAXING_CRESCENT, FIRST_QUARTER, WAXING_GIBBOUS, FULL, WANING_GIBBOUS, LAST_QUARTER, WANING_CRESCENT
}

/**
 * The moon on a day: its age in days since the last new moon (0 to 29.5), how much of it is lit (0 to 1),
 * its phase, and roughly when it rises and sets (epoch millis).
 */
data class MoonInfo(
    val ageDays: Double,
    val illumination: Double,
    val phase: MoonPhase,
    val waxing: Boolean,
    val rise: Long?,
    val set: Long?,
)

/** Where the sun and moon are: rules for the Sun and Moon cards on Home. */
object SkyEvents {
    /** Days from one new moon to the next. */
    const val SYNODIC_MONTH_DAYS = 29.530588853

    /** A known new moon: 6 January 2000, 18:14 UTC. */
    private const val KNOWN_NEW_MOON_MS = 947_182_440_000L

    private const val DAY_MS = 24 * 60 * 60 * 1000L

    /**
     * How far the sun is along today's path: 0 at sunrise, 1 at sunset, and in between during the day.
     * Before sunrise it is 0, after sunset 1. Null when sunrise or sunset is unknown.
     */
    fun sunProgress(now: Long, sunrise: Long?, sunset: Long?): Float? {
        if (sunrise == null || sunset == null || sunset <= sunrise) return null
        return ((now - sunrise).toFloat() / (sunset - sunrise)).coerceIn(0f, 1f)
    }

    /**
     * The moon at [now]. The age comes from counting whole moon cycles since a known new moon. The lit part is
     * (1 − cos(age as an angle)) / 2: 0 at new moon, 1 at full moon.
     *
     * Moonrise and moonset are estimated: at new moon the moon rises and sets with the sun, and each day of age
     * makes it about 49 minutes later (24 h over a 29.5-day cycle). Off by up to about an hour, which is enough for
     * a card. The times are moved into the 24 hours after [dayStart] (midnight today).
     */
    fun moon(now: Long, sunrise: Long?, sunset: Long?, dayStart: Long): MoonInfo {
        val cycles = (now - KNOWN_NEW_MOON_MS) / (SYNODIC_MONTH_DAYS * DAY_MS)
        val age = (cycles - floor(cycles)) * SYNODIC_MONTH_DAYS
        val fraction = age / SYNODIC_MONTH_DAYS
        val illumination = (1 - cos(2 * Math.PI * fraction)) / 2
        val delay = (fraction * DAY_MS).toLong()
        return MoonInfo(
            ageDays = age,
            illumination = illumination,
            phase = phaseOf(fraction),
            waxing = fraction < 0.5,
            rise = sunrise?.let { intoDay(it + delay, dayStart) },
            set = sunset?.let { intoDay(it + delay, dayStart) },
        )
    }

    /** Eight equal parts of the cycle, each centred on its phase (new moon is the part around 0). */
    fun phaseOf(fraction: Double): MoonPhase {
        val index = floor(fraction * 8 + 0.5).toInt() % 8
        return MoonPhase.entries[index]
    }

    /** Moves [time] by whole days so it falls on the day that starts at [dayStart]. */
    private fun intoDay(time: Long, dayStart: Long): Long = dayStart + Math.floorMod(time - dayStart, DAY_MS)
}
