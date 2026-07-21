package com.aviatechnik.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette of the mobile web reference (resources/views/mobile/*):
//   page background #343A40, menu bar = bootstrap primary #0D6EFD,
//   accents = bootstrap info/cyan #0DCAF0, drafts = warning #FFC107,
//   active menu ring = #00FF66. Dynamic color is intentionally NOT used.
val AviaBlue = Color(0xFF0D6EFD)
val AviaDeepSkyBlue = Color(0xFF0DCAF0)
val AviaSurface = Color(0xFF343A40)
val AviaSurfaceRaised = Color(0xFF2B3035)
val AviaTextSecondary = Color(0xFF9AA4B2)
val AviaWarning = Color(0xFFFFC107)
val AviaMenuActive = Color(0xFF00FF66)
val AviaMenuDisabled = Color(0xFF8B949E)

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
    // Dark-first, same as the web mobile shell (data-bs-theme=dark).
    MaterialTheme(colorScheme = DarkColors, content = content)
}
