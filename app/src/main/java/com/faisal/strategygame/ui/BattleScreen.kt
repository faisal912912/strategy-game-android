package com.faisal.strategygame.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faisal.strategygame.model.BattleState
import kotlinx.coroutines.delay

@Composable
fun BattleOverlay(
    battle: BattleState,
    onRound: () -> Unit,
    onLeave: () -> Unit,
) {
    LaunchedEffect(battle.round, battle.status) {
        if (battle.status == "FIGHTING") {
            delay(900)
            onRound()
        }
    }
    val playerHp by animateFloatAsState(battle.playerHp / 100f, tween(500), label = "player_hp")
    val enemyHp by animateFloatAsState(battle.enemyHp / 100f, tween(500), label = "enemy_hp")
    val hitScale by animateFloatAsState(if (battle.round % 2 == 0) 1f else 1.16f, spring(), label = "hit")
    val sky = Brush.verticalGradient(listOf(Color(0xFF1D3440), Color(0xFF4F603A), Color(0xFF272315)))

    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Box(Modifier.fillMaxSize().background(sky).padding(16.dp)) {
            IconButton(onClick = onLeave, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Close, "Leave battle")
            }
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BATTLE • ROUND ${battle.round}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Text(battle.enemyName, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(20.dp))
                HealthBar("YOUR ARMY", battle.playerHp, playerHp, Color(0xFF65C96B))
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Army("🛡️\n⚔️\n🏹", "2,450", hitScale)
                    Text("⚔", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                    Army("👹\n🪓\n🏹", "Raiders", if (battle.round % 2 == 0) 1.16f else 1f)
                }
                Text(battle.lastHit, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))
                HealthBar("ENEMY ARMY", battle.enemyHp, enemyHp, Color(0xFFE05B4F))
                Spacer(Modifier.height(18.dp))
                AnimatedVisibility(
                    visible = battle.status != "FIGHTING",
                    enter = scaleIn() + fadeIn(),
                    exit = fadeOut(),
                ) {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = if (battle.status == "VICTORY") Color(0xEE3A4D24) else Color(0xEE572A25)
                    )) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MilitaryTech, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                if (battle.status == "VICTORY") "VICTORY!" else "DEFEAT",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                            )
                            Text(if (battle.status == "VICTORY") "+5,000 Food • +200 Power" else "Upgrade and try again")
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = onLeave, modifier = Modifier.fillMaxWidth()) { Text("RETURN TO WORLD") }
                        }
                    }
                }
                if (battle.status == "FIGHTING") {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Combat resolving…", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun Army(symbols: String, caption: String, scale: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale)) {
        Text(symbols, style = MaterialTheme.typography.headlineLarge)
        Text(caption, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HealthBar(label: String, hp: Int, progress: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            Text("$hp / 100")
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(12.dp),
            color = color,
            trackColor = Color.Black.copy(alpha = .45f),
        )
    }
}
