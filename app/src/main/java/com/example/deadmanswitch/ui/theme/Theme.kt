package com.example.deadmanswitch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4C),
    secondary = Color(0xFF4D6357),
    tertiary = Color(0xFF3D6373),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFEFEFE),
    surface = Color(0xFFFEFEFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFF81D4FA),
    error = Color(0xFFCF6679),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

@Composable
fun DeadManSwitchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}