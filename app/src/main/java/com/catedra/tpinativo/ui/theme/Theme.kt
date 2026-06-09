package com.catedra.tpinativo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary          = HabitViolet,
    onPrimary        = HabitOnPrimary,
    primaryContainer = HabitSurfaceVar,
    secondary        = HabitSecondary,
    tertiary         = HabitTertiary,
    background       = HabitBackground,
    surface          = HabitSurface,
    surfaceVariant   = HabitSurfaceVar,
    onBackground     = HabitOnBackground,
    onSurface        = HabitOnBackground,
    error            = HabitCoral
)

private val DarkColorScheme = darkColorScheme(
    primary          = HabitSecondary,
    onPrimary        = HabitOnPrimary,
    primaryContainer = HabitPurple,
    secondary        = HabitPink,
    tertiary         = HabitTertiary,
    background       = HabitDarkBg,
    surface          = HabitDarkSurface,
    onBackground     = HabitDarkOnBg,
    onSurface        = HabitDarkOnBg,
    error            = HabitCoral
)

@Composable
fun TPINativoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
