package com.mohamed.miqaat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mohamed.miqaat.R

// IBM Plex Sans Arabic (SIL OFL 1.1) : police d'interface sobre, embarquée pour
// rester 100 % hors ligne. Trois graisses suffisent pour toute la hiérarchie.
val MiqaatFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
)

// lineHeight ~1,5–1,6× la taille : l'arabe a des hampes et diacritiques hautes/basses
// qui seraient rognées avec les interlignes Material par défaut.
// letterSpacing toujours 0 : les lettres arabes sont liées, on ne les espace pas.
private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(
    fontFamily = MiqaatFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)

val Typography = Typography(
    displayLarge = style(57, 84, FontWeight.SemiBold),
    displayMedium = style(45, 68, FontWeight.SemiBold),
    displaySmall = style(36, 54, FontWeight.SemiBold),
    headlineLarge = style(32, 48, FontWeight.SemiBold),
    headlineMedium = style(28, 42, FontWeight.Medium),
    headlineSmall = style(24, 36, FontWeight.Medium),
    titleLarge = style(22, 34, FontWeight.Medium),
    titleMedium = style(16, 26, FontWeight.Medium),
    titleSmall = style(14, 22, FontWeight.Medium),
    bodyLarge = style(16, 26),
    bodyMedium = style(14, 22),
    bodySmall = style(12, 19),
    labelLarge = style(14, 22, FontWeight.Medium),
    labelMedium = style(12, 19, FontWeight.Medium),
    labelSmall = style(11, 17, FontWeight.Medium),
)
