package com.aira.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BlueContainerLight,
    onPrimaryContainer = BlueOnContainerLight,
    secondary = SunSecondaryLight,
    secondaryContainer = BlueContainerLight,
    onSecondaryContainer = BlueOnContainerLight,
    background = PageLight,
    onBackground = TextLight,
    surface = CardLight,
    onSurface = TextLight,
    onSurfaceVariant = TextMutedLight,
    surfaceVariant = TileLight,
    surfaceContainer = CardLight,
    surfaceContainerHigh = CardLight,
    surfaceContainerHighest = TileLight,
    outline = TextMutedLight,
    outlineVariant = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = BlueOnContainerDark,
    secondary = SunSecondaryDark,
    secondaryContainer = BlueContainerDark,
    onSecondaryContainer = BlueOnContainerDark,
    background = PageDark,
    onBackground = TextDark,
    surface = CardDark,
    onSurface = TextDark,
    onSurfaceVariant = TextMutedDark,
    surfaceVariant = TileDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = CardDark,
    surfaceContainerHighest = TileDark,
    outline = TextMutedDark,
    outlineVariant = OutlineDark,
)

/** Rounded corners everywhere: cards are 24 dp, tiles 16 dp, chips are pills. */
private val AiraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)


/** Material 3 theme that follows the phone's light/dark setting. */
@Composable
fun AiraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AiraShapes,
        typography = AiraTypography,
        content = content,
    )
}
