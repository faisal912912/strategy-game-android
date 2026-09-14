package com.faisal.strategygame

import com.faisal.strategygame.model.CityState
import org.junit.Assert.assertEquals
import org.junit.Test

class CityStateTest {
    @Test
    fun initialCityHasFourBuildings() {
        assertEquals(4, CityState().buildings.size)
    }
}
