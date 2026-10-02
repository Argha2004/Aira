package com.aira.app.domain.engine

import com.aira.app.domain.model.AlertType

object AlertCooldown {

    /**
     * Keeps only the alerts that were not sent in the last 3 hours, so the user gets at most one
     * alert of each type per 3 hours. [lastSent] holds the send time (epoch millis) of each type sent before.
     */
    fun filter(
        due: List<AlertType>,
        lastSent: Map<AlertType, Long>,
        now: Long,
        cooldownMs: Long = Thresholds.ALERT_COOLDOWN_MS,
    ): List<AlertType> = due.filter { type ->
        val last = lastSent[type]
        last == null || now - last >= cooldownMs
    }
}
