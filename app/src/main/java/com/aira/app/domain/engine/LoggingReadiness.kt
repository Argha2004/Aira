package com.aira.app.domain.engine

/** Whether background logging can really record something, and if not, why. */
enum class LoggingStatus {
    /** Logging is on and has what it needs. */
    OK,

    /** The user turned logging off. */
    OFF,

    /** No location permission at all: every snapshot would be skipped. */
    NEEDS_LOCATION,

    /** Android 10+ needs "Allow all the time"; without it the background job cannot get a location. */
    NEEDS_BACKGROUND_LOCATION,
}

object LoggingReadiness {

    /** Background location became a separate permission in Android 10 (API 29). */
    private const val BACKGROUND_PERMISSION_SDK = 29

    /**
     * Logging that is switched on but cannot get a location would say "On" while saving nothing.
     * This tells the Settings screen when to show a warning instead. [sdk] is the Android version.
     */
    fun check(loggingEnabled: Boolean, locationGranted: Boolean, backgroundLocationGranted: Boolean, sdk: Int): LoggingStatus =
        when {
            !loggingEnabled -> LoggingStatus.OFF
            !locationGranted -> LoggingStatus.NEEDS_LOCATION
            sdk >= BACKGROUND_PERMISSION_SDK && !backgroundLocationGranted -> LoggingStatus.NEEDS_BACKGROUND_LOCATION
            else -> LoggingStatus.OK
        }
}
