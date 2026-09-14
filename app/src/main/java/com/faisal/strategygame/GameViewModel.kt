package com.faisal.strategygame

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.faisal.strategygame.model.GameAction
import com.faisal.strategygame.model.GameState

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
