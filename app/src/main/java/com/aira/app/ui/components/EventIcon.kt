package com.aira.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.aira.app.domain.engine.DayPart
import com.aira.app.domain.engine.DayParts
import com.aira.app.R
import com.aira.app.domain.model.DiaryEventType
import java.time.Instant
import java.time.ZoneId

fun DiaryEventType.imageVector(): ImageVector = when (this) {
    DiaryEventType.SUN -> Icons.Filled.WbSunny
    DiaryEventType.RAIN -> Icons.Filled.WaterDrop
    DiaryEventType.HEAT -> Icons.Filled.Thermostat
    DiaryEventType.COMMUTE -> Icons.Filled.DirectionsCar
    DiaryEventType.INDOOR -> Icons.Filled.Home
    DiaryEventType.OUTDOOR -> Icons.Filled.Park
    DiaryEventType.PRESSURE_DROP -> Icons.Filled.Compress
}

/** A short name for the event type, e.g. "Strong sun". */
@StringRes
fun DiaryEventType.labelRes(): Int = when (this) {
    DiaryEventType.SUN -> R.string.event_sun
    DiaryEventType.RAIN -> R.string.event_rain
    DiaryEventType.HEAT -> R.string.event_heat
    DiaryEventType.COMMUTE -> R.string.event_commute
    DiaryEventType.INDOOR -> R.string.event_indoor
    DiaryEventType.OUTDOOR -> R.string.event_outdoor
    DiaryEventType.PRESSURE_DROP -> R.string.event_pressure
}

/**
 * A short name for a diary event from its type and the part of the day it started in, e.g.
 * "Morning commute" or "Afternoon shower".
 */
@Composable
fun eventTitle(type: DiaryEventType, startMillis: Long): String {
    val hour = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).hour
    val part = stringResource(
        when (DayParts.of(hour)) {
            DayPart.MORNING -> R.string.part_morning
            DayPart.AFTERNOON -> R.string.part_afternoon
            DayPart.EVENING -> R.string.part_evening
            DayPart.NIGHT -> R.string.part_night
        },
    )
    return stringResource(
        when (type) {
            DiaryEventType.SUN -> R.string.title_sun
            DiaryEventType.RAIN -> R.string.title_rain
            DiaryEventType.HEAT -> R.string.title_heat
            DiaryEventType.COMMUTE -> R.string.title_commute
            DiaryEventType.INDOOR -> R.string.title_indoor
            DiaryEventType.OUTDOOR -> R.string.title_outdoor
            DiaryEventType.PRESSURE_DROP -> R.string.title_pressure
        },
        part,
    )
}

/** The colour of an event type, shared by its icon and its tile. */
fun DiaryEventType.tint(): Color = when (this) {
    DiaryEventType.SUN -> Color(0xFFF9A825)
    DiaryEventType.RAIN -> Color(0xFF1E88E5)
    DiaryEventType.HEAT -> Color(0xFFE64A19)
    DiaryEventType.COMMUTE -> Color(0xFF6D4C41)
    DiaryEventType.INDOOR -> Color(0xFF78909C)
    DiaryEventType.OUTDOOR -> Color(0xFF43A047)
    DiaryEventType.PRESSURE_DROP -> Color(0xFF8E24AA)
}

/**
 * The icon for a diary event type, in that type's colour. It is decorative by default (the summary text next
 * to it says the same thing); pass a [contentDescription] if it stands alone.
 */
@Composable
fun EventIcon(type: DiaryEventType, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Icon(imageVector = type.imageVector(), contentDescription = contentDescription, tint = type.tint(), modifier = modifier)
}
