package com.verifyetb.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.verifyetb.app.R

val Fraunces = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

val IbmPlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
)

val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
)

val BirrTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

val AmountStyle = TextStyle(
    fontFamily = IbmPlexMono,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp,
)
