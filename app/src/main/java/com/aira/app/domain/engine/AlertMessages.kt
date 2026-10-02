package com.aira.app.domain.engine

import com.aira.app.domain.model.AlertType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** The words of the alerts. Temperatures are written in °C; the screens convert them if needed. */
object AlertMessages {

    /** A short headline, saved with the alert and shown as the notification title. */
    fun headline(type: AlertType, value: Double?, hourText: String? = null): String = when (type) {
        AlertType.RAIN_SOON -> "Rain likely around ${hourText ?: "soon"}"
        AlertType.RAIN_NOW -> "It's raining where you are"
        AlertType.HEAT -> if (value != null) "Feels like ${value.roundToInt()} °C" else "It feels very hot"
        AlertType.STRONG_SUN ->
            if (value != null) "${value.roundToInt()} minutes in strong sun" else "Strong sun for 45+ minutes"
        AlertType.PRESSURE_DROP -> "Pressure falling, storm possible"
    }

    /** What to do about it. */
    fun advice(type: AlertType): String = when (type) {
        AlertType.RAIN_SOON -> "Take an umbrella and bring washing inside."
        AlertType.RAIN_NOW -> "Find shelter and protect your phone and bag."
        AlertType.HEAT -> "Drink water and rest in the shade."
        AlertType.STRONG_SUN -> "Drink water, find some shade."
        AlertType.PRESSURE_DROP -> "Rain may be coming. Close the windows."
    }

    /** A clock time such as "4 PM". */
    fun hourText(millis: Long, zone: ZoneId): String =
        DateTimeFormatter.ofPattern("h a", Locale.ENGLISH).format(Instant.ofEpochMilli(millis).atZone(zone))
}
