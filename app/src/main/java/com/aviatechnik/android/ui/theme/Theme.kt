package com.aviatechnik.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette from public/app-config (dark theme, blue → deepskyblue gradient).
// Dynamic color (Material You) is intentionally NOT used — see the brief.
val AviaBlue = Color(0xFF0000FF)
val AviaDeepSkyBlue = Color(0xFF00BFFF)
val AviaSurface = Color(0xFF121826)
val AviaSurfaceRaised = Color(0xFF1B2436)
val AviaTextSecondary = Color(0xFF9AA4B2)

private val DarkColors = darkColorScheme(
    primary = AviaDeepSkyBlue,
    onPrimary = Color.Black,
    secondary = AviaBlue,
    background = AviaSurface,
    surface = AviaSurfaceRaised,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun AviaTheme(content: @Composable () -> Unit) {
    // The product is dark-first (app-config theme = "dark"); light mode is not a v1 goal.
    MaterialTheme(colorScheme = DarkColors, content = content)
}
