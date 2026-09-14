package com.faisal.strategygame

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
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

    var toastMessage by mutableStateOf<String?>(null)
        private set

    fun login(name: String) {
        val cleanName = name.trim().ifBlank { "Governor" }
        state = state.copy(governorName = cleanName)
        save()
    }

    fun logout() {
        prefs.edit().clear().apply()
        state = GameState()
    }

    fun upgradeBuilding(id: String) {
        val building = state.buildings.first { it.id == id }
        val cost = building.level * 1_000L
        if (state.resources.wood < cost || state.resources.stone < cost / 2) {
            toastMessage = "Not enough resources"
            return
        }
        val upgraded = state.buildings.map {
            if (it.id == id) it.copy(level = it.level + 1) else it
        }
        state = state.copy(
            buildings = upgraded,
            power = state.power + 350,
            cityLevel = if (id == "hall") state.cityLevel + 1 else state.cityLevel,
            resources = state.resources.copy(
                wood = state.resources.wood - cost,
                stone = state.resources.stone - cost / 2,
            ),
        )
        toastMessage = "${building.name} upgraded"
        save()
    }

    fun trainTroops() {
        if (state.resources.food < 2_500 || state.resources.wood < 1_000) {
            toastMessage = "Not enough resources"
            return
        }
        state = state.copy(
            troops = state.troops + 100,
            power = state.power + 100,
            resources = state.resources.copy(
                food = state.resources.food - 2_500,
                wood = state.resources.wood - 1_000,
            ),
        )
        toastMessage = "100 troops trained"
        save()
    }

    fun research() {
        if (state.resources.gold < 500) {
            toastMessage = "Not enough gold"
            return
        }
        state = state.copy(
            power = state.power + 250,
            resources = state.resources.copy(gold = state.resources.gold - 500),
        )
        toastMessage = "Research completed"
        save()
    }

    fun completeMission(id: String) {
        val mission = state.missions.first { it.id == id }
        if (mission.completed) return
        state = state.copy(
            missions = state.missions.map { if (it.id == id) it.copy(completed = true) else it },
            power = state.power + 200,
            resources = state.resources.copy(food = state.resources.food + 5_000),
        )
        toastMessage = "Mission completed: ${mission.reward}"
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

    fun clearMessage() {
        toastMessage = null
    }

    private fun save() {
        prefs.edit()
            .putString("governor", state.governorName)
            .putInt("cityLevel", state.cityLevel)
            .putLong("power", state.power)
            .putInt("troops", state.troops)
            .apply()
    }
}
