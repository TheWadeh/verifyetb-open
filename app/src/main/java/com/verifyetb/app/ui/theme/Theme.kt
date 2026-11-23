package com.verifyetb.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    fun toPref(): String = when (this) {
        SYSTEM -> "system"
        LIGHT -> "light"
        DARK -> "dark"
    }

    companion object {
        fun fromPref(value: String?): ThemeMode = when (value) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}

private fun schemeFor(colors: BirrColors) = if (colors.isDark) {
    darkColorScheme(
        primary = colors.gold,
        onPrimary = colors.onGold,
        secondary = colors.green,
        onSecondary = colors.textPrimary,
        tertiary = colors.rust,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        onSurfaceVariant = colors.textMuted,
        outline = colors.divider,
        error = colors.rust,
        onError = colors.textPrimary,
    )
} else {
    lightColorScheme(
        primary = colors.gold,
        onPrimary = colors.onGold,
        secondary = colors.green,
        onSecondary = Color.White,
        tertiary = colors.rust,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        onSurfaceVariant = colors.textMuted,
        outline = colors.divider,
        error = colors.rust,
        onError = Color.White,
    )
}

@Composable
fun VerifyetbTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkBirrColors else LightBirrColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !dark
            insets.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalBirrColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = BirrTypography,
            content = content,
        )
    }
}

val ScrimNav = Color(0xCC0F1A17)
