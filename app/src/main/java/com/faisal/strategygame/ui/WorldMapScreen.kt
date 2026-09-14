package com.faisal.strategygame.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faisal.strategygame.GameViewModel
import com.faisal.strategygame.model.WorldTarget

@Composable
fun WorldMapScreen(vm: GameViewModel) {
    var selected by remember { mutableStateOf<WorldTarget?>(null) }
    val transition = rememberInfiniteTransition(label = "world_map")
    val pulse by transition.animateFloat(
        1f, 1.12f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "target_pulse",
    )

    selected?.let { target ->
        AlertDialog(
            onDismissRequest = { selected = null },
            icon = { Text(target.icon, style = MaterialTheme.typography.displaySmall) },
            title = { Text(target.name) },
            text = {
                val scout = vm.scoutReports[target.id]
                Text(
                    if (scout == null)
                        "Level ${target.level}\nCoordinates X:${target.x} Y:${target.y}\nScout first or dispatch 250 troops."
                    else
                        "SCOUTED ✓\nEstimated troops: ${scout.estimatedTroops}\nPossible reward: ${scout.reward}\nDispatch 250 troops?"
                )
            },
            confirmButton = {
                Button(onClick = { vm.startMarch(target); selected = null }) { Text("MARCH") }
            },
            dismissButton = {
                TextButton(onClick = { vm.scoutTarget(target); selected = null }) { Text("SCOUT") }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        vm.returningMarches.firstOrNull()?.let { returning ->
            val seconds = ((returning.arrivesAt - System.currentTimeMillis()).coerceAtLeast(0) + 999) / 1_000
            Surface(color = Color(0xEE24402B)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🏠", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Army returning home", fontWeight = FontWeight.Bold)
                        Text("${returning.troops} survivors • ${returning.lootFood} food • ${seconds}s")
                    }
                }
            }
        }
        vm.marches.firstOrNull()?.let { march ->
            val seconds = ((march.arrivesAt - System.currentTimeMillis()).coerceAtLeast(0) + 999) / 1_000
            Surface(color = Color(0xEE3B2E18)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🐎", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Marching to ${march.target.name}", fontWeight = FontWeight.Bold)
                        Text("Arrives in ${seconds}s • 250 troops")
                    }
                    IconButton(onClick = { vm.recallMarch(march.id) }) { Icon(Icons.Default.Undo, "Recall") }
                }
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(Color(0xFF688354), Color(0xFF33472F), Color(0xFF1A281C)))
            )
        ) {
            repeat(14) { i ->
                Text("🌲", Modifier.align(
                    when (i % 5) {
                        0 -> Alignment.TopStart
                        1 -> Alignment.TopEnd
                        2 -> Alignment.CenterStart
                        3 -> Alignment.CenterEnd
                        else -> Alignment.BottomEnd
                    }
                ).padding((8 + i * 3).dp))
            }
            Surface(
                modifier = Modifier.align(Alignment.Center).scale(pulse),
                shape = CircleShape,
                color = Color(0xEE26382A),
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏰", style = MaterialTheme.typography.headlineLarge)
                    Text("Your City", fontWeight = FontWeight.Bold)
                    Text("412, 687", style = MaterialTheme.typography.labelSmall)
                }
            }
            TargetMarker(vm.worldTargets[0], Modifier.align(Alignment.TopEnd).padding(top = 90.dp, end = 28.dp)) { selected = it }
            TargetMarker(vm.worldTargets[1], Modifier.align(Alignment.CenterStart).padding(start = 22.dp, bottom = 120.dp)) { selected = it }
            TargetMarker(vm.worldTargets[2], Modifier.align(Alignment.BottomEnd).padding(end = 35.dp, bottom = 75.dp)) { selected = it }
            AssistChip(
                onClick = {},
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                label = { Text("World 500×500 • Marches ${vm.marches.size}/5") },
                leadingIcon = { Icon(Icons.Default.MyLocation, null) },
            )
        }
    }
}

@Composable
private fun TargetMarker(target: WorldTarget, modifier: Modifier, onClick: (WorldTarget) -> Unit) {
    Surface(
        onClick = { onClick(target) },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xEE19251C),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(target.icon, style = MaterialTheme.typography.headlineMedium)
            Text("Lv.${target.level}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("${target.x},${target.y}", style = MaterialTheme.typography.labelSmall)
        }
    }
}
