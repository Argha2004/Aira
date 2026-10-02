package com.aira.app.domain.engine

object BatteryRules {

    /**
     * Skip reading sensors in the background when the battery is below [thresholdPercent] and the phone
     * is not charging. An unknown battery level (null) never skips.
     */
    fun shouldSkip(levelPercent: Int?, charging: Boolean, thresholdPercent: Int): Boolean =
        levelPercent != null && !charging && levelPercent < thresholdPercent
}
