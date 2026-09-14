package com.faisal.strategygame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KingdomColors = darkColorScheme(
    primary = Color(0xFFD7A84A),
    secondary = Color(0xFF74A96B),
    background = Color(0xFF101812),
    surface = Color(0xFF19251C),
    surfaceVariant = Color(0xFF26382A),
    onPrimary = Color(0xFF211A08),
    onBackground = Color(0xFFF2EBD8),
    onSurface = Color(0xFFF2EBD8),
)

@Composable
fun StrategyGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KingdomColors, content = content)
}
