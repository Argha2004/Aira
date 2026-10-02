package com.aira.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.aira.app.domain.engine.Temperature
import com.aira.app.domain.engine.TemperatureUnit

/** The unit chosen in Settings. Provided once at the top of the app, so every screen follows it. */
val LocalTemperatureUnit = staticCompositionLocalOf { TemperatureUnit.CELSIUS }

/** A temperature stored in °C, formatted in the chosen unit, e.g. "34°C" or "93°F". */
@Composable
fun formatTemperature(celsius: Double): String = Temperature.format(celsius, LocalTemperatureUnit.current)

/** A temperature without the unit letter, e.g. "28°", for tight places like the hourly forecast. */
@Composable
fun formatDegrees(celsius: Double): String = formatTemperature(celsius).trimEnd('C', 'F')

/** Converts "34 °C" inside a sentence (diary and insight texts are written in °C) to the chosen unit. */
@Composable
fun localizeTemperatures(text: String): String = Temperature.convertText(text, LocalTemperatureUnit.current)
