package com.example.deadmanswitch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF006C4C),
    secondary = androidx.compose.ui.graphics.Color(0xFF4D6357),
    tertiary = androidx.compose.ui.graphics.Color(0xFF3D6373),
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A)
)

@Composable
fun DeadManSwitchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}