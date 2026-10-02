package com.aira.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aira.app.R

/** Poppins, bundled in res/font (Open Font License), used for all text in the app. */
val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private val Base = Typography()

private fun TextStyle.poppins(weight: FontWeight? = null) =
    copy(fontFamily = Poppins, fontWeight = weight ?: fontWeight)

/** The default type scale in Poppins, with a big light number for temperatures and slightly bolder titles. */
val AiraTypography = Typography(
    displayLarge = TextStyle(fontFamily = Poppins, fontSize = 72.sp, lineHeight = 80.sp, fontWeight = FontWeight.Light),
    displayMedium = Base.displayMedium.poppins(FontWeight.Light),
    displaySmall = Base.displaySmall.poppins(),
    headlineLarge = Base.headlineLarge.poppins(),
    headlineMedium = Base.headlineMedium.poppins(FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.poppins(FontWeight.SemiBold),
    titleLarge = Base.titleLarge.poppins(FontWeight.SemiBold),
    titleMedium = Base.titleMedium.poppins(FontWeight.SemiBold),
    titleSmall = Base.titleSmall.poppins(FontWeight.Medium),
    bodyLarge = Base.bodyLarge.poppins(),
    bodyMedium = Base.bodyMedium.poppins(),
    bodySmall = Base.bodySmall.poppins(),
    labelLarge = Base.labelLarge.poppins(FontWeight.Medium),
    labelMedium = Base.labelMedium.poppins(FontWeight.Medium),
    labelSmall = Base.labelSmall.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
)
