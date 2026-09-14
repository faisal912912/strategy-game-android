package com.faisal.strategygame

import com.faisal.strategygame.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {
    @Test
    fun betaStartsWithPlayableContent() {
        val state = GameState()
        assertEquals(6, state.buildings.size)
        assertEquals(3, state.commanders.size)
        assertEquals(3, state.missions.size)
        assertTrue(state.resources.food > 0)
    }
}
