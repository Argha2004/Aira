package com.aira.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aira.app.R
import com.aira.app.domain.model.AlertType

private fun AlertType.imageVector(): ImageVector = when (this) {
    AlertType.RAIN_SOON, AlertType.RAIN_NOW -> Icons.Filled.WaterDrop
    AlertType.HEAT -> Icons.Filled.Thermostat
    AlertType.STRONG_SUN -> Icons.Filled.WbSunny
    AlertType.PRESSURE_DROP -> Icons.Filled.Compress
}

/** The colour of an alert type; lighter on the glass pages so it stands out on the blue-grey glass. */
@Composable
private fun AlertType.tint(): Color {
    val glass = LocalOnSky.current
    return when (this) {
        AlertType.RAIN_SOON, AlertType.RAIN_NOW -> if (glass) Color(0xFFA8D4FF) else Color(0xFF1E88E5)
        AlertType.HEAT -> if (glass) Color(0xFFFFA48C) else Color(0xFFE64A19)
        AlertType.STRONG_SUN -> if (glass) Color(0xFFFFD166) else Color(0xFFF9A825)
        AlertType.PRESSURE_DROP -> if (glass) Color(0xFFE1B3FF) else Color(0xFF8E24AA)
    }
}

/** The words for an alert type, e.g. in the "Remind me when" list. */
@StringRes
fun AlertType.labelRes(): Int = when (this) {
    AlertType.RAIN_SOON -> R.string.alert_type_rain_soon
    AlertType.RAIN_NOW -> R.string.alert_type_rain_now
    AlertType.HEAT -> R.string.alert_type_heat
    AlertType.STRONG_SUN -> R.string.alert_type_strong_sun
    AlertType.PRESSURE_DROP -> R.string.alert_type_pressure_drop
}

/**
 * The icon of an alert type, in that type's colour. It is decorative by default (the words next to it say
 * the same thing, so a screen reader should not read it twice); pass a [contentDescription] if it stands alone.
 */
@Composable
fun AlertIcon(type: AlertType, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Icon(imageVector = type.imageVector(), contentDescription = contentDescription, tint = type.tint(), modifier = modifier)
}
