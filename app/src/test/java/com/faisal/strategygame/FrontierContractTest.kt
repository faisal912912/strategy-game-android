package com.faisal.strategygame

import com.faisal.strategygame.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FrontierContractTest {
    @Test fun gatewayCannotSmuggleCredentialsOrApiPaths() {
        assertEquals("https://game.example.com",secureOrigin(" https://game.example.com/ "))
        listOf("http://game.example.com","https://name:password@game.example.com","https://game.example.com/api/v1","https://game.example.com?token=a").forEach {
            assertTrue(runCatching {secureOrigin(it)}.isFailure)
        }
    }
    @Test fun replaySurvivesSerializationWithoutChangingTheIdempotencyKeyOrBody() {
        val command=Command("train","/game/training/start",json("type" to "infantry","amount" to 100,"tier" to 1),"train")
        val recovered=Command.decode(JSONObject(command.encode().toString()))
        assertEquals(command.key,recovered.key)
        assertEquals(command.path,recovered.path)
        assertEquals(100,recovered.body.getInt("amount"))
        assertEquals(command.body.toString(),recovered.body.toString())
    }
    @Test fun serverNullCollectionsAreEmpty() {
        assertTrue(JSONObject("{\"marches\":null}").rows("marches").isEmpty())
        assertTrue(JSONObject().rows("marches").isEmpty())
    }
    @Test fun versionGateUsesNumericComponents() {
        assertTrue(versionSupported("0.10.0","0.2.0"))
        assertTrue(versionSupported("0.2.0","0.2"))
        assertFalse(versionSupported("0.2.0","1.0.0"))
    }
    @Test fun timerUnderstandsServerOffsetAndFractionalTimestamp() {
        assertEquals(instantMillis("2026-09-14T12:00:00Z"),instantMillis("2026-09-14T15:00:00+03:00"))
        assertTrue(instantMillis("2026-09-14T15:00:00.123456+03:00")>0)
    }
    @Test fun campaignClaimIdentitySurvivesRestart() {
        val job=CityJob("hunt","hunt",1000,"/world/monsters/claim",42)
        val stored=JSONObject(job.encode().toString())
        assertEquals(42L,stored.getLong("claimId"))
        assertEquals("/world/monsters/claim",stored.getString("claimPath"))
    }
}
