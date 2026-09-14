package com.faisal.strategygame.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faisal.strategygame.GameViewModel
import com.faisal.strategygame.model.*
import java.text.NumberFormat

private enum class GameTab(val label: String, val icon: ImageVector) {
    CITY("City", Icons.Default.Home),
    WORLD("World", Icons.Default.Public),
    COMMANDERS("Heroes", Icons.Default.Shield),
    ALLIANCE("Alliance", Icons.Default.Groups),
    MORE("More", Icons.Default.Menu),
}

@Composable
fun GameApp(vm: GameViewModel = viewModel()) {
    val state = vm.state
    if (state.governorName.isBlank()) {
        LoginScreen(vm)
    } else {
        KingdomScreen(vm)
    }
}

@Composable
private fun LoginScreen(vm: GameViewModel) {
    var name by remember { mutableStateOf("") }
    var gateway by remember { mutableStateOf(vm.serverUrl) }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF263F2A), Color(0xFF0B130D)))
        ).padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xEE19251C))) {
            Column(
                Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedCrown()
                Text("KINGDOM FRONTIER", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("BETA • SELECT KINGDOM", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = vm.selectedServer == 1,
                        onClick = { vm.selectServer(1) },
                        label = { Text("Server 1") },
                        leadingIcon = { Icon(Icons.Default.Public, null) },
                    )
                    FilterChip(
                        selected = vm.selectedServer == 2,
                        onClick = { vm.selectServer(2) },
                        label = { Text("Server 2") },
                        leadingIcon = { Icon(Icons.Default.Public, null) },
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(18) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Governor name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = gateway,
                    onValueChange = { gateway = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Secure server gateway (optional)") },
                    placeholder = { Text("https://game.example.com/") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (vm.connectionStatus.startsWith("Online")) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        null,
                        tint = if (vm.connectionStatus.startsWith("Online")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(vm.connectionStatus, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { vm.testServer(gateway) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = gateway.isNotBlank(),
                ) { Text("TEST SECURE CONNECTION") }
                Button(
                    onClick = {
                        if (!vm.connectionStatus.startsWith("Online")) vm.useOfflineMode()
                        vm.login(name)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (vm.connectionStatus.startsWith("Online")) "ENTER SERVER ${vm.selectedServer}" else "PLAY OFFLINE BETA")
                }
                Text("Direct backend ports are never exposed", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun KingdomScreen(vm: GameViewModel) {
    var tab by remember { mutableStateOf(GameTab.CITY) }
    val snackbar = remember { SnackbarHostState() }
    val message = vm.toastMessage
    LaunchedEffect(Unit) {
        while (true) {
            vm.tick()
            delay(1_000)
        }
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color.Transparent,
        topBar = { KingdomHeader(vm.state) },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF101812)) {
                GameTab.entries.forEach {
                    NavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(it.icon, contentDescription = it.label) },
                        label = { Text(it.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF304B32), Color(0xFF101812))))
                .padding(padding)
        ) {
            AnimatedKingdomBackdrop()
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = .97f)) togetherWith
                        (fadeOut() + scaleOut(targetScale = 1.03f))
                },
                label = "kingdom_tab",
            ) { current ->
                when (current) {
                    GameTab.CITY -> CityScreen(vm)
                    GameTab.WORLD -> WorldMapScreen(vm)
                    GameTab.COMMANDERS -> CommandersScreen(vm.state)
                    GameTab.ALLIANCE -> AllianceScreen(vm)
                    GameTab.MORE -> MoreScreen(vm)
                }
            }
            vm.battle?.let { battle ->
                BattleOverlay(battle, vm::resolveBattleRound, vm::leaveBattle)
            }
        }
    }
}

@Composable
private fun KingdomHeader(state: GameState) {
    Column(Modifier.background(Color(0xFF101812)).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF26382A)) {
                Icon(Icons.Default.Person, null, Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(state.governorName, fontWeight = FontWeight.Bold)
                Text("Power ${number(state.power)}", style = MaterialTheme.typography.labelSmall)
            }
            Text("VIP 1", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiniResource("🌾", state.resources.food)
            MiniResource("🪵", state.resources.wood)
            MiniResource("🪨", state.resources.stone)
            MiniResource("🪙", state.resources.gold)
            MiniResource("💎", state.resources.gems)
        }
    }
}

@Composable
private fun MiniResource(icon: String, value: Long) {
    Surface(color = Color(0xFF26382A), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon)
            Spacer(Modifier.width(4.dp))
            AnimatedNumber(value)
        }
    }
}

@Composable
private fun CityScreen(vm: GameViewModel) {
    val state = vm.state
    var pendingUpgrade by remember { mutableStateOf<Building?>(null) }
    pendingUpgrade?.let { building ->
        val wood = building.level * 1_000L
        val stone = building.level * 500L
        AlertDialog(
            onDismissRequest = { pendingUpgrade = null },
            icon = { Text(building.icon, style = MaterialTheme.typography.headlineLarge) },
            title = { Text("Upgrade ${building.name}?") },
            text = { Text("Level ${building.level} → ${building.level + 1}\nCost: ${number(wood)} wood + ${number(stone)} stone\nTime: 30 seconds") },
            confirmButton = {
                Button(onClick = { vm.upgradeBuilding(building.id); pendingUpgrade = null }) { Text("Start Upgrade") }
            },
            dismissButton = { TextButton(onClick = { pendingUpgrade = null }) { Text("Cancel") } },
        )
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        vm.action?.let { running ->
            item {
                ActionQueueCard(running, vm::speedUp)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xDD26382A))) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏰", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Capital City", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("City Hall Level ${state.cityLevel}")
                            Text("${number(state.troops.toLong())} troops ready", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(vm::trainTroops, Modifier.weight(1f)) {
                            Icon(Icons.Default.Add, null)
                            Text("Train")
                        }
                        OutlinedButton(vm::research, Modifier.weight(1f)) {
                            Icon(Icons.Default.Science, null)
                            Text("Research")
                        }
                    }
                }
            }
        }
        item { SectionTitle("Buildings", "Tap upgrade to grow your power") }
        items(state.buildings, key = { it.id }) { building ->
            BuildingCard(building) { pendingUpgrade = building }
        }
    }
}

@Composable
private fun ActionQueueCard(action: GameAction, onSpeedUp: () -> Unit) {
    val seconds = ((action.endsAt - System.currentTimeMillis()).coerceAtLeast(0) + 999) / 1_000
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2E18))) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(action.label, fontWeight = FontWeight.Bold)
                    Text("00:${seconds.toString().padStart(2, '0')} remaining")
                }
                FilledTonalButton(onClick = onSpeedUp) { Text("⚡ 25") }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BuildingCard(building: Building, onUpgrade: () -> Unit) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color(0xEE19251C))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BouncyIcon(building.icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(building.name, fontWeight = FontWeight.Bold)
                Text(building.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Level ${building.level}", color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onUpgrade) { Text("Upgrade") }
        }
    }
}

@Composable
private fun WorldScreen(vm: GameViewModel) {
    LazyColumn(
        Modifier.padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionTitle("World Map", "Explore, gather, and defeat enemies")
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF26382A))) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingWorldIcon()
                        Text("Kingdom #1 • Peaceful Zone")
                        Text("X: 412  Y: 687", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        item { SectionTitle("Campaign Missions", "Complete battles to earn rewards") }
        items(vm.state.missions, key = { it.id }) { mission ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xEE19251C))) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    PulsingMissionIcon(mission.completed)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(mission.title, fontWeight = FontWeight.Bold)
                        Text("${mission.difficulty} • ${mission.reward}", style = MaterialTheme.typography.bodySmall)
                    }
                    FilledTonalButton(onClick = { vm.startBattle(mission.id) }, enabled = !mission.completed) {
                        Text(if (mission.completed) "Done" else "Battle")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandersScreen(state: GameState) {
    LazyColumn(
        Modifier.padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SectionTitle("Commanders", "Build the strongest army") }
        items(state.commanders, key = { it.id }) { commander ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xEE19251C))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF26382A)) {
                        Text("⚔️", Modifier.padding(14.dp), style = MaterialTheme.typography.headlineMedium)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(commander.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(commander.rarity, color = if (commander.rarity == "Legendary") Color(0xFFFFC857) else Color(0xFFC89BFF))
                        Text("Level ${commander.level} • Power ${number(commander.power.toLong())}")
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
private fun AllianceScreen(vm: GameViewModel) {
    val state = vm.state
    LazyColumn(
        Modifier.padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionTitle("Alliance [FLC]", state.alliance)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xEE19251C))) {
                Column(Modifier.padding(16.dp)) {
                    Text("🦅", style = MaterialTheme.typography.displayMedium)
                    Text("Falcons", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Members 42/60 • Power 18.4M")
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { .72f }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Button(vm::allianceHelp, Modifier.fillMaxWidth(), enabled = state.allianceHelp > 0) {
                        Icon(Icons.Default.Handshake, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Help Allies (${state.allianceHelp})")
                    }
                }
            }
        }
        item { SectionTitle("Alliance Activity", "Work together to grow") }
        items(listOf("Alliance technology donation", "Territory flags", "Barbarian rally", "Alliance gifts")) {
            ListItem(
                headlineContent = { Text(it) },
                leadingContent = { Icon(Icons.Default.Groups, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                colors = ListItemDefaults.colors(containerColor = Color(0xEE19251C)),
            )
        }
    }
}

@Composable
private fun MoreScreen(vm: GameViewModel) {
    var showMail by remember { mutableStateOf(false) }
    var showReports by remember { mutableStateOf(false) }
    if (showReports) {
        AlertDialog(
            onDismissRequest = { showReports = false },
            title = { Text("War Reports") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (vm.battleReports.isEmpty()) Text("No battle reports yet")
                    vm.battleReports.take(5).forEach { report ->
                        Column {
                            Text(if (report.victory) "Victory • ${report.enemy}" else "Defeat • ${report.enemy}", fontWeight = FontWeight.Bold)
                            Text("Rounds ${report.rounds} • Survivors ${report.survivors} • Losses ${report.losses} • Loot ${report.lootFood}")
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showReports = false }) { Text("Close") } },
        )
    }
    if (showMail) {
        AlertDialog(
            onDismissRequest = { showMail = false },
            title = { Text("Inbox") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    vm.state.mail.forEach {
                        Column {
                            Text(it.title, fontWeight = FontWeight.Bold)
                            Text(it.body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showMail = false }) { Text("Close") } },
        )
    }
    LazyColumn(
        Modifier.padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionTitle("Governor", "Account and kingdom tools") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xEE19251C))) {
                Column(Modifier.padding(16.dp)) {
                    Text(vm.state.governorName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Server ${vm.selectedServer} • Player ID 100001")
                    Text(vm.connectionStatus, color = MaterialTheme.colorScheme.secondary)
                    if (vm.serverUrl.isNotBlank()) Text(vm.serverUrl, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Text("Beta v0.9.0", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { MenuRow(Icons.Default.MilitaryTech, "War Reports", "${vm.battleReports.size} reports") { showReports = true } }
        item { MenuRow(Icons.Default.Mail, "Inbox", "${vm.state.mail.size} messages") { showMail = true } }
        item { MenuRow(Icons.Default.TaskAlt, "Daily Quests", "4 rewards available") {} }
        item { MenuRow(Icons.Default.Inventory2, "Inventory", "View your items") {} }
        item { MenuRow(Icons.Default.Leaderboard, "Rankings", "Kingdom leaderboard") {} }
        item { MenuRow(Icons.Default.Settings, "Settings", "Sound, language, account") {} }
        item {
            OutlinedButton(vm::logout, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(6.dp))
                Text("Log out")
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color(0xEE19251C))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .68f))
    }
}

private fun number(value: Long): String = NumberFormat.getIntegerInstance().format(value)
