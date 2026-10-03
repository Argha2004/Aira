package com.aira.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aira.app.ui.components.LocalOnSky

// Soft blue-and-white palette: a pale page, white cards, one strong blue for actions.
val BluePrimaryLight = Color(0xFF1D5BD6)
val BlueOnPrimaryLight = Color(0xFFFFFFFF)
val BlueContainerLight = Color(0xFFDCE8FB)
val BlueOnContainerLight = Color(0xFF0B2A66)
val SunSecondaryLight = Color(0xFF8A6D00)
val PageLight = Color(0xFFEEF3FB)
val CardLight = Color(0xFFFFFFFF)
val TileLight = Color(0xFFF1F5FC)
val OutlineLight = Color(0xFFDDE5F2)
val TextLight = Color(0xFF14213D)
val TextMutedLight = Color(0xFF5B6B86)

val BluePrimaryDark = Color(0xFF9DBDFF)
val BlueOnPrimaryDark = Color(0xFF002A6B)
val BlueContainerDark = Color(0xFF22406F)
val BlueOnContainerDark = Color(0xFFDCE8FB)
val SunSecondaryDark = Color(0xFFFFD54F)
val PageDark = Color(0xFF0D1422)
val CardDark = Color(0xFF172033)
val TileDark = Color(0xFF1F2A40)
val OutlineDark = Color(0xFF2C3954)
val TextDark = Color(0xFFE6ECF8)
val TextMutedDark = Color(0xFF9BA9C4)

// Meaning colours used by the rings, tiles and icons. On the glass pages (the weather sky) the deep versions are
// hard to see on the blue-grey glass, so lighter, brighter versions are used there.
val SunAmber: Color @Composable get() = if (LocalOnSky.current) Color(0xFFFFD166) else Color(0xFFF2A516)
val HeatCoral: Color @Composable get() = if (LocalOnSky.current) Color(0xFFFFA48C) else Color(0xFFF26B4E)
val RainBlue: Color @Composable get() = if (LocalOnSky.current) Color(0xFFA8D4FF) else Color(0xFF2F6FDE)
val LiveGreen: Color @Composable get() = if (LocalOnSky.current) Color(0xFF7CF0B4) else Color(0xFF22B573)
val AirGood = Color(0xFF22B573)
val AirModerate = Color(0xFFF2A516)
val AirPoor = Color(0xFFE5484D)
