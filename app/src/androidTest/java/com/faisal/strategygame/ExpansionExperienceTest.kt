package com.faisal.strategygame

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.faisal.strategygame.ui.*
import com.faisal.strategygame.ui.theme.StrategyGameTheme
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpansionExperienceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun speedupChooserFiltersAndSubmitsSelectedCount() {
        var submitted:Command?=null
        compose.runOnUiThread {compose.activity.setContent {StrategyGameTheme {
            SpeedupPicker(CityJob("train","تدريب المشاة",300000,jobId=77),listOf(
                JSONObject("""{"item_key":"frontier_speed_60","name":"تسريع دقيقة","type":"speedup","quantity":4,"effect_value":60}"""),
                JSONObject("""{"item_key":"frontier_build_300","name":"تسريع البناء فقط","type":"speedup_build","quantity":8,"effect_value":300}""")
            ),0,true,{}){submitted=it}
        }}}
        compose.onNodeWithText("تسريع البناء فقط").assertDoesNotExist()
        compose.onNodeWithContentDescription("زيادة التسريعات").performClick()
        compose.onNodeWithTag("speedup-count").assertTextEquals("2")
        captureTestScreenshot(compose,"beta6-speedup")
        compose.onNodeWithText("استخدام 2 تسريع").performClick()
        compose.runOnIdle{assertEquals(77L,submitted!!.body.getLong("job_id"));assertEquals(2L,submitted!!.body.getLong("count"))}
    }
    @Test fun warehouseSearchShowsOwnedItemsAndEquipment() {
        val vm=FrontierViewModel(compose.activity.application)
        val docs=mapOf("/me" to JSONObject("""{"username":"Faisal","resources":{"food":80000,"wood":60000,"stone":40000,"gold":20000}}"""),
            "/inventory" to JSONObject("""{"items":[{"item_key":"frontier_speed_60","name":"تسريع دقيقة","type":"speedup","quantity":20,"effect_value":60},{"item_key":"food_pack","name":"حزمة غذاء","type":"food","quantity":5,"effect_value":1000}]}"""),
            "/equipment" to JSONObject("""{"equipment":[{"key":"royal_sword","owned":true,"equipped":true}]}"""))
        @Suppress("UNCHECKED_CAST")
        (FrontierViewModel::class.java.getDeclaredField("documents\$delegate").apply{isAccessible=true}.get(vm) as MutableState<Map<String,JSONObject>>).value=docs
        vm.selectTab("warehouse")
        compose.runOnUiThread{compose.activity.setContent{StrategyGameTheme{CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){Kingdom(vm)}}}}
        compose.onNodeWithTag("warehouse-search").performScrollTo().performTextInput("غذاء")
        compose.onNodeWithText("حزمة غذاء").assertExists()
        compose.onNodeWithText("تسريع دقيقة").assertDoesNotExist()
        compose.onNodeWithTag("warehouse-search").performTextClearance()
        compose.onNodeWithTag("warehouse-search").performImeAction()
        captureTestScreenshot(compose,"beta6-warehouse-search")
        compose.onNodeWithText("التسريعات",useUnmergedTree=true).performClick()
        compose.onNodeWithText("حزمة غذاء").assertDoesNotExist()
        compose.onNodeWithText("تسريع دقيقة").assertExists()
        captureTestScreenshot(compose,"beta6-warehouse")
    }
    @Test fun thousandCityWorldCullsOffscreenAndRemainsInteractive() {
        var camera by mutableStateOf(MapCamera(248f,236f,1.4f))
        var selected=""
        val pins=(0 until 1000).map {i->WorldPin("npc-$i","city",8f+(i%40)*12f,8f+(i/40)*19f,"حصن $i",i%28+3,JSONObject().put("power",10000L+i*7000))}
        compose.runOnUiThread{compose.activity.setContent{MaterialTheme{WorldBoard(camera,{camera=it},pins,true){selected=it.key}}}}
        compose.onNodeWithTag("npc-500").assertIsDisplayed().performClick()
        compose.runOnIdle{assertEquals("npc-500",selected)}
        compose.onNodeWithTag("npc-0").assertDoesNotExist()
        captureTestScreenshot(compose,"beta6-populated-world")
        compose.onNodeWithTag("world-board").performTouchInput{swipeLeft()}
        compose.runOnIdle{assertTrue(camera.x>248f)}
    }
}
