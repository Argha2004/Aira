package com.aira.app.domain.engine

/** Every number the rules use, in one place so they are easy to tune. */
object Thresholds {
    // Indoor / outdoor (light sensor)
    const val OUTDOOR_LUX = 1000f
    const val INDOOR_LUX = 300f
    const val STRONG_SUN_LUX = 30_000f

    // Movement
    const val WALKING_STEPS = 20
    const val VEHICLE_DISTANCE_KM = 1.0

    /** Minutes a snapshot stands for: until the next one, at most 60; the last one of a day counts 30. */
    const val MAX_SNAPSHOT_MINUTES = 60.0
    const val DEFAULT_SNAPSHOT_MINUTES = 30.0

    /** Two snapshots are never taken closer together than this (the 5-minute check and the hourly safety job). */
    const val MIN_SNAPSHOT_SPACING_MS = 4 * 60 * 1000L

    /** A previous snapshot older than this is ignored when working out movement and place. */
    const val MAX_SNAPSHOT_GAP_MS = 2 * 60 * 60 * 1000L

    // Weather exposure
    const val HEAT_FEELS_LIKE_C = 33.0
    const val RAIN_MM = 0.2

    // UV dose = sum of (UV index x outdoor minutes)
    const val UV_DOSE_MODERATE = 100.0
    const val UV_DOSE_HIGH = 300.0

    // Falling pressure
    const val PRESSURE_DROP_HPA = 3.0
    const val PRESSURE_WINDOW_MS = 3 * 60 * 60 * 1000L

    // Background logging: no snapshots from 23:00 up to 06:00
    const val NIGHT_PAUSE_START_HOUR = 23
    const val NIGHT_PAUSE_END_HOUR = 6

    // When the weather is unknown, daylight is guessed from the clock: 06:00 up to 18:00
    const val DAY_START_HOUR = 6
    const val DAY_END_HOUR = 18

    // Alerts
    const val HEAT_ALERT_C = 38.0
    const val STRONG_SUN_ALERT_MINUTES = 45.0
    const val ALERT_COOLDOWN_MS = 3 * 60 * 60 * 1000L
    /** How far back the alert rules look (the pressure rule needs 3 hours). */
    const val ALERT_LOOKBACK_MS = 3 * 60 * 60 * 1000L

    // Weather tasks
    /** Rain is "likely" from this forecast probability (percent). */
    const val RAIN_SOON_PROBABILITY = 60
    /** RAIN_SOON looks at the forecast for this long from now. */
    const val RAIN_SOON_LOOKAHEAD_MS = 3 * 60 * 60 * 1000L
    /** An outdoor task is checked against the forecast this long before it is due. */
    const val PLAN_CHECK_LEAD_MS = 3 * 60 * 60 * 1000L

    // Battery: below this percent (and not charging) background sensor reading is skipped
    const val DEFAULT_BATTERY_SAVER_PERCENT = 15

    // Calendar: what makes a day "hot", "sunny" or "mostly indoors"
    const val HOT_DAY_MINUTES = 60
    const val SUNNY_DAY_MINUTES = 30

    // Insights
    const val HOT_INSIGHT_C = 35.0
    const val MIN_DAYS_FOR_COMPARISON = 2
    const val SAME_AMOUNT_PERCENT = 5

    // Commute
    const val COMMUTE_RADIUS_M = 300.0
    const val COMMUTE_MAX_MS = 2 * 60 * 60 * 1000L
}
