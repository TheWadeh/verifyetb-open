package com.verifyetb.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class BirrColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val blue: Color,
    val rust: Color,
    val green: Color,
    val divider: Color,
    val onBlue: Color,
    val isDark: Boolean,
)

val DarkBirrColors = BirrColors(
    background = Color(0xFF0F1A17),
    surface = Color(0xFF1A2823),
    surfaceElevated = Color(0xFF22332D),
    textPrimary = Color(0xFFF2EFE6),
    textMuted = Color(0xFF8B958F),
    blue = Color(0xFF5B8DEF),
    rust = Color(0xFFC4634F),
    green = Color(0xFF6B9E7F),
    divider = Color(0xFF2A3A34),
    onBlue = Color(0xFFFFFFFF),
    isDark = true,
)

val LightBirrColors = BirrColors(
    background = Color(0xFFF3F7F5),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFE7EFEA),
    textPrimary = Color(0xFF15201C),
    textMuted = Color(0xFF5F6F68),
    blue = Color(0xFF2563EB),
    rust = Color(0xFFB04A38),
    green = Color(0xFF3F7A58),
    divider = Color(0xFFD5E0DB),
    onBlue = Color(0xFFFFFFFF),
    isDark = false,
)

val LocalBirrColors = staticCompositionLocalOf { DarkBirrColors }

/** Theme-aware tokens — use inside @Composable only. */
val BgPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalBirrColors.current.background

val BgSurface: Color
    @Composable @ReadOnlyComposable get() = LocalBirrColors.current.surface

val BgElevated: Color
    @Composable @ReadOnlyComposable get() = LocalBirrColors.current.surfaceElevated

val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalBirrColors.current.textPrimary

val TextMuted: Color
    @Composable @ReadOnlyComposable get() = LocalBirrColors.current.textMuted

val AccentBlue: Color
    @Composable @ReadOnlyComposable get() = LocalBirrColors.current.blue

val AccentRust: Color
    @Composable @ReadOnlyComposable get() = LocalBirrColors.current.rust

val AccentGreen: Color
    @Composable @ReadOnlyComposable get() = LocalBirrColors.current.green

val Divider: Color
    @Composable @ReadOnlyComposable get() = LocalBirrColors.current.divider

val OnBlue: Color
    @Composable @ReadOnlyComposable get() = LocalBirrColors.current.onBlue
