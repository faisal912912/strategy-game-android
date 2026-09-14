package com.faisal.strategygame.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import kotlin.math.sin

@Composable
fun AnimatedKingdomBackdrop() {
    val transition = rememberInfiniteTransition(label = "world")
    val drift by transition.animateFloat(
        initialValue = -180f,
        targetValue = 1_200f,
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloud_drift",
    )
    val shimmer by transition.animateFloat(
        initialValue = .15f,
        targetValue = .42f,
        animationSpec = infiniteRepeatable(tween(2_800), RepeatMode.Reverse),
        label = "shimmer",
    )
    Canvas(Modifier.fillMaxSize().alpha(.42f)) {
        drawCircle(Color(0xFFD9F2D2).copy(alpha = shimmer), 72f, Offset(drift, size.height * .18f))
        drawCircle(Color(0xFFD9F2D2).copy(alpha = shimmer * .7f), 52f, Offset(drift + 90f, size.height * .16f))
        drawCircle(Color(0xFFFFD27A).copy(alpha = .10f), size.width * .42f, Offset(size.width * .85f, size.height * .08f))
        repeat(18) { index ->
            val x = (index * 97f + drift * .12f) % (size.width + 40f)
            val y = size.height * (.15f + (index % 7) * .12f)
            drawCircle(Color.White.copy(alpha = .10f), 2.5f + index % 3, Offset(x, y))
        }
    }
}

@Composable
fun AnimatedCrown() {
    val transition = rememberInfiniteTransition(label = "crown")
    val scale by transition.animateFloat(
        initialValue = .92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1_100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "crown_scale",
    )
    Text(
        "♛",
        modifier = Modifier.scale(scale),
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun AnimatedNumber(value: Long) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "resource_number",
    )
    Text(
        NumberFormat.getIntegerInstance().format(animated.toLong()),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun BouncyIcon(icon: String) {
    val transition = rememberInfiniteTransition(label = "building")
    val scale by transition.animateFloat(
        initialValue = .96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "building_breathe",
    )
    Text(icon, modifier = Modifier.scale(scale), style = MaterialTheme.typography.headlineMedium)
}

@Composable
fun FloatingWorldIcon() {
    val transition = rememberInfiniteTransition(label = "map")
    val offset by transition.animateFloat(
        initialValue = -5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "map_float",
    )
    Text("🗺️", modifier = Modifier.offset(y = offset.dp), style = MaterialTheme.typography.displayLarge)
}

@Composable
fun PulsingMissionIcon(completed: Boolean) {
    val transition = rememberInfiniteTransition(label = "mission")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (completed) 1f else 1.18f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "mission_pulse",
    )
    Icon(
        if (completed) Icons.Default.CheckCircle else Icons.Default.Flag,
        contentDescription = null,
        modifier = Modifier.scale(scale),
        tint = if (completed) Color(0xFF74A96B) else MaterialTheme.colorScheme.primary,
    )
}
