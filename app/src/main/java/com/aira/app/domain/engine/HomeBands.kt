package com.aira.app.domain.engine

/** How the air feels to the body, from the feels-like temperature. */
enum class Comfort { COOL, COMFORTABLE, WARM, HOT }

/** The US air quality index in the usual bands. */
enum class AirBand { GOOD, MODERATE, SENSITIVE, UNHEALTHY }

/** The UV index in the usual bands (not the same as [UvBand], which is for a whole day's dose). */
enum class UvLevel { LOW, MODERATE, HIGH, VERY_HIGH }

/** How bright it is around the phone, from the light sensor. */
enum class LightLevel { STRONG_SUN, BRIGHT, INDOOR, DIM }

/** How windy it is, for the caption of the Home wind card. */
enum class WindLevel { CALM, GENTLE, BREEZY, WINDY }

/** Simple labels for the numbers on the Home screen. The words are chosen in the UI. */
object HomeBands {
    private const val COOL_BELOW_C = 18.0
    private const val WARM_ABOVE_C = 28.0

    /** Below 18 is cool, up to 28 comfortable, up to the heat limit (33) warm, above it hot. */
    fun comfort(feelsLikeC: Double): Comfort = when {
        feelsLikeC < COOL_BELOW_C -> Comfort.COOL
        feelsLikeC <= WARM_ABOVE_C -> Comfort.COMFORTABLE
        feelsLikeC <= Thresholds.HEAT_FEELS_LIKE_C -> Comfort.WARM
        else -> Comfort.HOT
    }

    /** Above 30,000 lux is strong sun, 1,000 lux or more is bright (outdoor), up to 300 lux is dim, the rest is indoor light. */
    fun light(lux: Float): LightLevel = when {
        lux > Thresholds.STRONG_SUN_LUX -> LightLevel.STRONG_SUN
        lux >= Thresholds.OUTDOOR_LUX -> LightLevel.BRIGHT
        lux > Thresholds.INDOOR_LUX -> LightLevel.INDOOR
        else -> LightLevel.DIM
    }

    /** Below 3 is low, below 6 moderate, below 8 high, 8 and above very high. */
    fun uv(index: Double): UvLevel = when {
        index < 3 -> UvLevel.LOW
        index < 6 -> UvLevel.MODERATE
        index < 8 -> UvLevel.HIGH
        else -> UvLevel.VERY_HIGH
    }

    /** Below 6 km/h is calm, below 20 a gentle breeze, below 39 breezy, from 39 windy. */
    fun wind(kmh: Double): WindLevel = when {
        kmh < 6 -> WindLevel.CALM
        kmh < 20 -> WindLevel.GENTLE
        kmh < 39 -> WindLevel.BREEZY
        else -> WindLevel.WINDY
    }

    /** Wind direction in degrees (where it comes from) as one of 8 compass points, e.g. 45° → "NE". */
    fun compass(degrees: Int): String {
        val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val normalized = ((degrees % 360) + 360) % 360
        return points[((normalized + 22) / 45) % 8]
    }

    /**
     * Humidity now against yesterday's average, in percentage points (negative = lower than yesterday).
     * Null when there is no humidity for yesterday.
     */
    fun humidityChange(nowPercent: Int, yesterdayAverage: Double?): Int? =
        yesterdayAverage?.let { Math.round(nowPercent - it).toInt() }

    /** 0 to 50 good, 51 to 100 moderate, 101 to 150 unhealthy for sensitive people, above that unhealthy. */
    fun air(aqi: Int): AirBand = when {
        aqi <= 50 -> AirBand.GOOD
        aqi <= 100 -> AirBand.MODERATE
        aqi <= 150 -> AirBand.SENSITIVE
        else -> AirBand.UNHEALTHY
    }
}
