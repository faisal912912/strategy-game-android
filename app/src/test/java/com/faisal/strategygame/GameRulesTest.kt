package com.faisal.strategygame

import org.junit.Assert.*
import org.junit.Test

class GameRulesTest {
    @Test fun allTierUnlocksMatchServerContract() {
        assertEquals(listOf(1,10,15,20,25),(1..5).map(::tierBuildingLevel))
        assertEquals("stable",trainingBuilding("cavalry"))
        assertEquals("archery_range",trainingBuilding("archers"))
    }
    @Test fun trainingCostsAndDurationCoverSmallAndMaximumBatches() {
        assertEquals(TrainingCost(10,5,2,10),trainingCost(1,1))
        assertEquals(TrainingCost(5000000,2500000,1000000,50000),trainingCost(5,100000))
        assertEquals(TrainingCost(2000,1000,400,20),trainingCost(2,100))
        assertTrue(runCatching{trainingCost(6,100)}.isFailure)
        assertTrue(runCatching{trainingCost(1,0)}.isFailure)
    }
    @Test fun mapPanUsesWorldUnitsAndClampsEdges() {
        assertEquals(MapCamera(45f,40f),MapCamera(50f,50f).pan(10f,20f,2f,499f))
        assertEquals(MapCamera(0f,499f),MapCamera(50f,50f).pan(10000f,-10000f,2f,499f))
    }
    @Test fun zoomKeepsTheWorldPointUnderTheFingers() {
        val c=MapCamera(200f,100f)
        val z=c.zoomAt(2f,20f,40f,2f,499f)
        assertEquals(c.x+20/2f,z.x+20/(2*z.zoom),.001f)
        assertEquals(c.y+40/2f,z.y+40/(2*z.zoom),.001f)
        assertEquals(4f,z.zoomAt(100f,0f,0f,2f,499f).zoom,.001f)
    }
}
