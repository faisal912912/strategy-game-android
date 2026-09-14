package com.faisal.strategygame

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faisal.strategygame.data.GameServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.faisal.strategygame.model.GameAction
import com.faisal.strategygame.model.GameState
import com.faisal.strategygame.model.BattleState
import com.faisal.strategygame.model.MarchState
import com.faisal.strategygame.model.WorldTarget
import com.faisal.strategygame.model.ReturnMarch
import com.faisal.strategygame.model.BattleReport
import com.faisal.strategygame.model.ScoutReport

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("kingdom_beta", 0)

    var state by mutableStateOf(
        GameState(
            governorName = prefs.getString("governor", "") ?: "",
            cityLevel = prefs.getInt("cityLevel", 8),
            power = prefs.getLong("power", 24_850),
            troops = prefs.getInt("troops", 2_450),
        )
    )
        private set

    var action by mutableStateOf(loadAction())
        private set

    var toastMessage by mutableStateOf<String?>(null)
        private set

    var serverUrl by mutableStateOf(prefs.getString("serverUrl", "") ?: "")
        private set

    var selectedServer by mutableStateOf(prefs.getInt("selectedServer", 1))
        private set

    var battle by mutableStateOf<BattleState?>(null)
        private set

    val worldTargets = listOf(
        WorldTarget("t1", "Border Raiders", 438, 671, 4, "👹", "m1"),
        WorldTarget("t2", "Ancient Ruins", 401, 702, 7, "🗿", "m2"),
        WorldTarget("t3", "Trade Caravan", 466, 645, 10, "🐫", "m3"),
    )

    var marches by mutableStateOf<List<MarchState>>(emptyList())
        private set

    var returningMarches by mutableStateOf<List<ReturnMarch>>(emptyList())
        private set

    var battleReports by mutableStateOf<List<BattleReport>>(emptyList())
        private set

    var scoutReports by mutableStateOf<Map<String, ScoutReport>>(emptyMap())
        private set

    var connectionStatus by mutableStateOf(if (serverUrl.isBlank()) "Offline Beta" else "Not tested")
        private set

    fun selectServer(id: Int) {
        selectedServer = id
        prefs.edit().putInt("selectedServer", id).apply()
    }

    fun testServer(rawUrl: String) {
        val normalized = rawUrl.trim().trimEnd('/') + "/"
        if (!normalized.startsWith("https://")) {
            connectionStatus = "Use a secure HTTPS gateway"
            return
        }
        serverUrl = normalized
        connectionStatus = "Connecting…"
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { GameServer(normalized).health() }
            connectionStatus = if (result.isSuccess) "Online • Server $selectedServer" else "Unavailable"
            if (result.isSuccess) {
                prefs.edit().putString("serverUrl", normalized).apply()
                toastMessage = "Secure server connection verified"
            }
        }
    }

    fun useOfflineMode() {
        connectionStatus = "Offline Beta"
        serverUrl = ""
        prefs.edit().remove("serverUrl").apply()
    }

    fun login(name: String) {
        state = state.copy(governorName = name.trim().ifBlank { "Governor" })
        save()
    }

    fun logout() {
        prefs.edit().clear().apply()
        state = GameState()
        action = null
    }

    fun upgradeBuilding(id: String) {
        if (!queueAvailable()) return
        val building = state.buildings.first { it.id == id }
        val woodCost = building.level * 1_000L
        val stoneCost = building.level * 500L
        if (state.resources.wood < woodCost || state.resources.stone < stoneCost) {
            toastMessage = "Need ${number(woodCost)} wood and ${number(stoneCost)} stone"
            return
        }
        state = state.copy(resources = state.resources.copy(
            wood = state.resources.wood - woodCost,
            stone = state.resources.stone - stoneCost,
        ))
        startAction("upgrade", id, "Upgrading ${building.name}", 30)
    }

    fun trainTroops() {
        if (!queueAvailable()) return
        if (state.resources.food < 2_500 || state.resources.wood < 1_000) {
            toastMessage = "Need 2,500 food and 1,000 wood"
            return
        }
        state = state.copy(resources = state.resources.copy(
            food = state.resources.food - 2_500,
            wood = state.resources.wood - 1_000,
        ))
        startAction("training", "troops", "Training 100 Infantry", 20)
    }

    fun research() {
        if (!queueAvailable()) return
        if (state.resources.gold < 500) {
            toastMessage = "Need 500 gold"
            return
        }
        state = state.copy(resources = state.resources.copy(gold = state.resources.gold - 500))
        startAction("research", "economy", "Researching Construction I", 25)
    }

    fun tick() {
        resolveMarches()
        resolveReturns()
        val current = action ?: return
        if (System.currentTimeMillis() < current.endsAt) return
        when (current.type) {
            "upgrade" -> {
                val building = state.buildings.first { it.id == current.targetId }
                state = state.copy(
                    buildings = state.buildings.map {
                        if (it.id == current.targetId) it.copy(level = it.level + 1) else it
                    },
                    power = state.power + 350,
                    cityLevel = if (current.targetId == "hall") state.cityLevel + 1 else state.cityLevel,
                )
                toastMessage = "${building.name} reached level ${building.level + 1}"
            }
            "training" -> {
                state = state.copy(troops = state.troops + 100, power = state.power + 100)
                toastMessage = "100 Infantry joined your army"
            }
            "research" -> {
                state = state.copy(power = state.power + 250)
                toastMessage = "Construction I research completed"
            }
        }
        action = null
        prefs.edit().remove("actionType").remove("actionTarget").remove("actionLabel").remove("actionEnds").apply()
        save()
    }

    fun speedUp() {
        val current = action ?: return
        if (state.resources.gems < 25) {
            toastMessage = "Need 25 gems"
            return
        }
        state = state.copy(resources = state.resources.copy(gems = state.resources.gems - 25))
        action = current.copy(endsAt = System.currentTimeMillis())
        tick()
    }

    fun startMarch(target: WorldTarget) {
        if (marches.size >= 5) {
            toastMessage = "All 5 march slots are busy"
            return
        }
        if (state.troops < 250) {
            toastMessage = "You need at least 250 troops"
            return
        }
        val distance = kotlin.math.sqrt(
            ((target.x - 412) * (target.x - 412) + (target.y - 687) * (target.y - 687)).toDouble()
        )
        val travelSeconds = (8 + distance / 4).toLong().coerceAtMost(30)
        val now = System.currentTimeMillis()
        marches = marches + MarchState("march-${now}", target, 250, now, now + travelSeconds * 1_000)
        state = state.copy(troops = state.troops - 250)
        toastMessage = "March dispatched • ${travelSeconds}s"
    }

    fun recallMarch(id: String) {
        val march = marches.firstOrNull { it.id == id } ?: return
        marches = marches.filterNot { it.id == id }
        state = state.copy(troops = state.troops + march.troops)
        toastMessage = "March recalled • all troops returned"
    }

    private fun resolveMarches() {
        if (battle != null) return
        val arrived = marches.firstOrNull { System.currentTimeMillis() >= it.arrivesAt } ?: return
        marches = marches.filterNot { it.id == arrived.id }
        startBattle(arrived.target.missionId)
    }

    fun scoutTarget(target: WorldTarget) {
        val estimate = target.level * 180 + 350
        scoutReports = scoutReports + (target.id to ScoutReport(
            target.id, target.name, estimate, state.missions.first { it.id == target.missionId }.reward
        ))
        toastMessage = "Scout report ready for ${target.name}"
    }

    private fun resolveReturns() {
        val now = System.currentTimeMillis()
        val arrived = returningMarches.filter { now >= it.arrivesAt }
        if (arrived.isEmpty()) return
        state = state.copy(
            troops = state.troops + arrived.sumOf { it.troops },
            resources = state.resources.copy(food = state.resources.food + arrived.sumOf { it.lootFood }),
        )
        returningMarches = returningMarches - arrived.toSet()
        toastMessage = "Army returned with loot"
        save()
    }

    fun startBattle(missionId: String) {
        val mission = state.missions.first { it.id == missionId }
        if (mission.completed) {
            toastMessage = "This mission is already complete"
            return
        }
        battle = BattleState(missionId, mission.title)
    }

    fun resolveBattleRound() {
        val current = battle ?: return
        if (current.status != "FIGHTING") return
        val nextRound = current.round + 1
        val commanderBonus = (state.commanders.first().power / 1_500).coerceAtMost(12)
        val enemyDamage = (17 + commanderBonus + nextRound % 5).coerceAtMost(30)
        val playerDamage = (8 + nextRound * 2).coerceAtMost(22)
        val enemyHp = (current.enemyHp - enemyDamage).coerceAtLeast(0)
        val playerHp = (current.playerHp - playerDamage).coerceAtLeast(0)
        val status = when {
            enemyHp == 0 -> "VICTORY"
            playerHp == 0 -> "DEFEAT"
            else -> "FIGHTING"
        }
        battle = current.copy(
            playerHp = playerHp,
            enemyHp = enemyHp,
            round = nextRound,
            status = status,
            lastHit = "Your army dealt $enemyDamage damage • Enemy dealt $playerDamage",
        )
        if (status != "FIGHTING") {
            val mission = state.missions.first { it.id == current.missionId }
            val survivors = if (status == "VICTORY") 220 else 170
            val loot = if (status == "VICTORY") 5_000L else 0L
            battleReports = listOf(
                BattleReport(
                    "report-${System.currentTimeMillis()}", mission.title, status == "VICTORY",
                    nextRound, survivors, 250 - survivors, loot
                )
            ) + battleReports
            returningMarches = returningMarches + ReturnMarch(
                survivors, loot, System.currentTimeMillis() + 10_000
            )
            if (status == "VICTORY") {
                state = state.copy(
                    missions = state.missions.map { if (it.id == current.missionId) it.copy(completed = true) else it },
                    power = state.power + 200,
                )
            }
        }
    }

    fun leaveBattle() {
        battle = null
    }

    fun completeMission(id: String) {
        val mission = state.missions.first { it.id == id }
        if (mission.completed) return
        state = state.copy(
            missions = state.missions.map { if (it.id == id) it.copy(completed = true) else it },
            power = state.power + 200,
            resources = state.resources.copy(food = state.resources.food + 5_000),
        )
        toastMessage = "Victory! Reward: ${mission.reward}"
        save()
    }

    fun allianceHelp() {
        if (state.allianceHelp <= 0) {
            toastMessage = "All help requests completed"
            return
        }
        state = state.copy(allianceHelp = state.allianceHelp - 1)
        toastMessage = "Alliance member helped"
    }

    fun clearMessage() { toastMessage = null }

    private fun queueAvailable(): Boolean {
        tick()
        if (action != null) {
            toastMessage = "Your action queue is busy"
            return false
        }
        return true
    }

    private fun startAction(type: String, target: String, label: String, seconds: Int) {
        action = GameAction(type, target, label, System.currentTimeMillis() + seconds * 1_000L)
        prefs.edit()
            .putString("actionType", type)
            .putString("actionTarget", target)
            .putString("actionLabel", label)
            .putLong("actionEnds", action!!.endsAt)
            .apply()
        toastMessage = "$label started"
        save()
    }

    private fun loadAction(): GameAction? {
        val type = prefs.getString("actionType", null) ?: return null
        return GameAction(
            type,
            prefs.getString("actionTarget", "") ?: "",
            prefs.getString("actionLabel", "Working") ?: "Working",
            prefs.getLong("actionEnds", 0),
        )
    }

    private fun save() {
        prefs.edit()
            .putString("governor", state.governorName)
            .putInt("cityLevel", state.cityLevel)
            .putLong("power", state.power)
            .putInt("troops", state.troops)
            .apply()
    }

    private fun number(value: Long) = java.text.NumberFormat.getIntegerInstance().format(value)
}
