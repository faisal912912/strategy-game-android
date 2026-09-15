package com.faisal.strategygame

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ExpansionRulesTest {
    @Test fun speedupLimitIncludesFinalPartialItemButNeverOverspends() {
        assertEquals(2L,speedupLimit(61000,60,20))
        assertEquals(1L,speedupLimit(60000,60,20))
        assertEquals(1L,speedupLimit(61000,60,1))
        assertEquals(0L,speedupLimit(0,60,20))
        assertEquals(0L,speedupLimit(1000,0,20))
        assertEquals(0L,speedupLimit(1000,60,0))
        assertEquals(10000L,speedupLimit(Long.MAX_VALUE,60,Long.MAX_VALUE))
    }
    @Test fun speedupTypesRespectTheJob() {
        assertTrue(speedupCompatible("speedup","train"))
        assertTrue(speedupCompatible("speedup_build","build"))
        assertFalse(speedupCompatible("speedup_train","build"))
        assertFalse(speedupCompatible("food","research"))
    }
    @Test fun warehouseSeparatesStoredPacksAndConsumables() {
        assertEquals("resources",inventoryCategory(JSONObject("{\"type\":\"gold\"}")))
        assertEquals("speedups",inventoryCategory(JSONObject("{\"type\":\"speedup_train\"}")))
        assertEquals("other",inventoryCategory(JSONObject("{\"type\":\"vip\"}")))
    }
    @Test fun commandTargetsTheSpecificServerJob() {
        val command=speedupCommand(CityJob("train","تدريب",5000,jobId=73),"frontier_speed_60",3)
        assertEquals("/expansion/v1/speedup",command.path)
        assertEquals(73L,command.body.getLong("job_id"))
        assertEquals(3L,command.body.getLong("count"))
        assertEquals("train",command.body.getString("kind"))
    }
}
