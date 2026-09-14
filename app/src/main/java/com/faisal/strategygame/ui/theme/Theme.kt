package com.faisal.strategygame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KingdomColors = darkColorScheme(
    primary = Color(0xFFE8C675),
    secondary = Color(0xFF83D8C2),
    background = Color(0xFF0B1825),
    surface = Color(0xFF192D3C),
    surfaceVariant = Color(0xFF294352),
    onPrimary = Color(0xFF211A08),
    onBackground = Color(0xFFF2EBD8),
    onSurface = Color(0xFFF2EBD8),
)

@Composable
fun StrategyGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KingdomColors, content = content)
}
