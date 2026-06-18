package com.childlearning.robot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7FF),
    onPrimaryContainer = Color(0xFF3D2E8D),
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0EB),
    onSecondaryContainer = Color(0xFF8B1A4A),
    background = LightBackground,
    onBackground = Color(0xFF1A1A2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF3F0FF),
    onSurfaceVariant = Color(0xFF6B6B8D),
    error = Error,
    onError = Color.White,
    outline = Color(0xFFE0DCE8),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9B93FF),
    onPrimary = Color(0xFF2D2470),
    primaryContainer = Color(0xFF4A40A0),
    secondary = Color(0xFFFFB0C8),
    background = DarkBackground,
    surface = Color(0xFF1E1E2E),
    error = Error,
)

@Composable
fun RobotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
