package com.example.deadmanswitch.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8AF8C0),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE9D8),
    onSecondaryContainer = Color(0xFF0A1F16),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FB),
    onTertiaryContainer = Color(0xFF001E2A),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C1A),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6DDBA4),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005237),
    onPrimaryContainer = Color(0xFF8AF8C0),
    secondary = Color(0xFFB3CDBC),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCFE9D8),
    tertiary = Color(0xFFA5CCDF),
    onTertiary = Color(0xFF073544),
    tertiaryContainer = Color(0xFF244B5A),
    onTertiaryContainer = Color(0xFFC1E8FB),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    background = Color(0xFF191C1A),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF191C1A),
    onSurface = Color(0xFFE1E3DF),
)

@Composable
fun DeadManSwitchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val activity = view.context as? Activity ?: return@SideEffect
                if (activity.isFinishing || activity.isDestroyed) return@SideEffect
                val window = activity.window
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            } catch (_: Exception) {
                // 忽略窗口操作异常
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
