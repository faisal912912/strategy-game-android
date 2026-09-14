package com.faisal.strategygame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faisal.strategygame.model.CityState
import java.text.NumberFormat

private enum class GameTab(val label: String, val icon: ImageVector) {
    CITY("City", Icons.Default.Home),
    WORLD("World", Icons.Default.Public),
    ALLIANCE("Alliance", Icons.Default.Groups),
    COMMANDERS("Commanders", Icons.Default.Shield),
}

@Composable
fun GameApp() {
    var tab by remember { mutableStateOf(GameTab.CITY) }
    val city = remember { CityState() }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = Color(0xEE101812)) {
                GameTab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, contentDescription = it.label) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF304B32), Color(0xFF101812)))
                )
                .padding(padding)
        ) {
            when (tab) {
                GameTab.CITY -> CityScreen(city)
                else -> ComingSoon(tab.label)
            }
        }
    }
}

@Composable
private fun CityScreen(city: CityState) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header(city)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResourceChip("Food", city.resources.food, Icons.Default.Restaurant, Modifier.weight(1f))
            ResourceChip("Wood", city.resources.wood, Icons.Default.Forest, Modifier.weight(1f))
            ResourceChip("Stone", city.resources.stone, Icons.Default.Landscape, Modifier.weight(1f))
            ResourceChip("Gold", city.resources.gold, Icons.Default.Paid, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Text("Kingdom Buildings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            city.buildings.forEach { building ->
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xDD26382A)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Icon(Icons.Default.Castle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        Text(building.name, fontWeight = FontWeight.Bold)
                        Text("Level ${building.level}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(building.status, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Development build • Server: 10.0.2.2:8080",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = .65f),
        )
    }
}

@Composable
private fun Header(city: CityState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xAA101812)) {
            Icon(Icons.Default.Person, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(city.governorName, fontWeight = FontWeight.Bold)
            Text("Power ${NumberFormat.getIntegerInstance().format(city.power)}")
        }
        Spacer(Modifier.weight(1f))
        AssistChip(
            onClick = {},
            label = { Text("City Hall Lv. ${city.cityLevel}") },
            leadingIcon = { Icon(Icons.Default.Castle, null) },
        )
    }
}

@Composable
private fun ResourceChip(
    label: String,
    amount: Long,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, color = Color(0xCC19251C), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(NumberFormat.getIntegerInstance().format(amount), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ComingSoon(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Construction, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Ready for the next development phase")
        }
    }
}
