package com.faisal.strategygame

import android.app.Application
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.faisal.strategygame.ui.*
import com.faisal.strategygame.ui.theme.StrategyGameTheme
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** Render the actual signed-in UI using isolated test documents. No credentials or network calls. */
class RealmExperienceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Suppress("UNCHECKED_CAST")
    private fun fixture():FrontierViewModel {
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val vm=FrontierViewModel(app)
        val docs=mutableMapOf(
            "/me" to JSONObject("""{"username":"Faisal","player_id":1,"power":884370,"city":{"level":8},"resources":{"food":3290000,"wood":1760000,"stone":584000,"gold":96800},"army":{"infantry":3500,"cavalry":1250,"archers":2700}}"""),
            "/game/buildings" to JSONObject("""{"buildings":[${townSpots.joinToString(","){"{\"type\":\"${it.key}\",\"level\":8}"}}]}"""),
            "/city/progression" to JSONObject("""{"xp":3200,"xp_for_next_level":5000}"""),
            "/commanders" to JSONObject("""{"commanders":[{"key":"iron_guard","owned":true,"level":4,"attack_bonus_percent":10,"defense_bonus_percent":15},{"key":"eagle_eye","owned":true,"level":3,"attack_bonus_percent":18,"defense_bonus_percent":5},{"key":"storm_rider","owned":false,"level":1,"recruit_cost_gold":10000},{"key":"royal_marshal","owned":false,"level":1,"recruit_cost_gold":25000}]}"""),
            "/commanders/skills" to JSONObject("""{"skills":[{"commander_key":"eagle_eye","skill_key":"deadly_focus","level":2,"max_level":5,"upgrade_gold":1500,"bonus_per_level":3,"effect_type":"attack"}]}"""),
            "/world/v2/state" to JSONObject("""{"x":250,"y":250}"""),
            "/world/v4/pve/status" to JSONObject("""{"energy":83,"max_energy":100}"""),
            "/commerce/v2/wallet" to JSONObject("""{"total_gems":1600,"bonus_gems":100}"""),
            "/shop/v3/catalog" to JSONObject("""{"products":[{"product_key":"starter_resource_crate","product_type":"bundle","price_gems":100,"grants":{"food":10000,"wood":10000}},{"product_key":"growth_bundle","product_type":"bundle","price_gems":500,"grants":{"food":50000,"wood":50000,"stone":10000}}]}""")
        )
        docs[vm.cityPath]=JSONObject("""{"cities":[{"player_id":1,"username":"Faisal","x":250,"y":250},{"player_id":2,"username":"حارس الوادي","x":229,"y":260}]}""")
        docs[vm.nodePath]=JSONObject("""{"nodes":[{"id":1,"node_type":"food","status":"active","level":3,"x":270,"y":274,"remaining_amount":40000},{"id":2,"node_type":"wood","status":"active","level":5,"x":231,"y":228,"remaining_amount":96000},{"id":3,"node_type":"gold","status":"active","level":4,"x":268,"y":234,"remaining_amount":56000}]}""")
        docs[vm.monsterPath]=JSONObject("""{"monsters":[{"id":1,"name":"ذئب الجبال","status":"active","level":6,"x":248,"y":277,"power":25000,"current_hp":18000,"max_hp":18000}]}""")
        val field=FrontierViewModel::class.java.getDeclaredField("documents\$delegate").apply{isAccessible=true}
        (field.get(vm) as MutableState<Map<String,JSONObject>>).value=docs
        vm.setMotion(true)
        // signedIn stays false: view-model commands and refreshes cannot reach a server.
        return vm
    }
    private fun launch(vm:FrontierViewModel) {
        compose.runOnUiThread {compose.activity.setContent{StrategyGameTheme{CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){Kingdom(vm)}}}}
    }
    @Test fun cityHudBuildingDetailsAndDockWorkTogether() {
        val vm=fixture();launch(vm)
        captureTestScreenshot(compose,"beta4-city-before-tap")
        compose.onNodeWithTag("building-castle").assertIsDisplayed().performClick()
        captureTestScreenshot(compose,"beta4-building-after-tap")
        compose.waitUntil(5000) {compose.onAllNodesWithText("متطلبات المستوى 9").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("متطلبات المستوى 9").assertExists()
        captureTestScreenshot(compose,"beta4-building")
        compose.onNodeWithText("إغلاق").performClick()
        captureTestScreenshot(compose,"beta4-city")
        compose.onNodeWithTag("tab-world").performClick()
        compose.onNodeWithTag("kingdom-minimap").assertExists()
        compose.onNodeWithTag("monster-1").assertIsDisplayed()
        captureTestScreenshot(compose,"beta4-world")
        compose.onNodeWithText("الموارد",useUnmergedTree=true).performClick()
        compose.onNodeWithTag("monster-1").assertDoesNotExist()
        compose.onNodeWithTag("node-1").assertExists()
        compose.onNodeWithContentDescription("الأهداف القريبة").performClick()
        compose.onNodeWithText("أهداف حول موقع العرض").assertExists()
    }
    @Test fun heroSelectionArmyTierAndTreasuryRemainAccessible() {
        val vm=fixture();vm.selectTab("heroes");launch(vm)
        compose.onNodeWithText("عين الصقر").performScrollTo().performClick()
        captureTestScreenshot(compose,"beta4-heroes")
        compose.onNodeWithTag("tab-army").performClick()
        captureTestScreenshot(compose,"beta4-army")
        compose.onNodeWithText("T5").performScrollTo().performClick()
        compose.onNodeWithText("T5 • ثكنة المشاة مستوى 25").assertExists()
        compose.onNodeWithTag("tab-shop").performClick()
        captureTestScreenshot(compose,"beta4-shop")
        compose.onNodeWithText("الشحن").performClick()
        compose.onNodeWithText("الشحن المالي غير مفعّل في هذه البيتا").assertExists()
    }
    @Test fun selectionsAndTrainingAmountSurviveNavigation() {
        val vm=fixture();vm.selectTab("heroes");launch(vm)
        compose.onNodeWithTag("hero-choice-eagle_eye").performClick().assertIsSelected()
        compose.onNodeWithTag("tab-army").performClick()
        compose.onNodeWithTag("training-amount").performScrollTo().performTextReplacement("321")
        compose.onNodeWithContentDescription("زيادة عدد الجنود").performClick()
        compose.onNodeWithTag("training-amount").assertTextContains("421")
        compose.onNodeWithTag("training-amount").performImeAction()
        captureTestScreenshot(compose,"beta5-training")
        compose.onNodeWithTag("tab-heroes").performClick()
        compose.onNodeWithTag("hero-choice-eagle_eye").assertIsSelected()
        compose.onNodeWithTag("tab-army").performClick()
        compose.onNodeWithTag("training-amount").assertIsDisplayed().assertTextContains("421")
    }
    @Test fun profileInventoryCouncilAndSceneToolsStayReachable() {
        val vm=fixture();launch(vm)
        compose.onNodeWithContentDescription("ملف الحاكم").performClick()
        compose.onNodeWithText("القوات المتاحة").assertIsDisplayed()
        captureTestScreenshot(compose,"beta5-profile")
        compose.onNodeWithContentDescription("إغلاق").performClick()
        compose.onNodeWithTag("resource-inventory").performClick()
        compose.onNodeWithText("مخزون المدينة").assertIsDisplayed()
        compose.onNodeWithContentDescription("إغلاق").performClick()
        compose.onNodeWithTag("tab-more").performClick()
        captureTestScreenshot(compose,"beta5-council")
        compose.onNodeWithText("الحقيبة").performClick()
        compose.onNodeWithText("حقيبتك فارغة").assertIsDisplayed()
        compose.onNodeWithText("العودة إلى الديوان").performClick()
        compose.onNodeWithText("الإعدادات").performClick()
        compose.onNodeWithText("تقليل حركة الخلفية").assertIsDisplayed()
        compose.onNodeWithTag("tab-city").performClick()
        compose.onNodeWithContentDescription("قائمة المدينة").performClick()
        compose.onNodeWithText("إخفاء أسماء المباني").performClick()
        compose.onNodeWithTag("building-castle").assertIsDisplayed().performClick()
        compose.onNodeWithText("متطلبات المستوى 9").assertExists()
        compose.onNodeWithText("إغلاق").performClick()
        compose.onNodeWithTag("tab-world").performClick()
        compose.onNodeWithContentDescription("أدوات الخريطة").performClick()
        compose.onNodeWithText("تكبير الخريطة").performClick()
        compose.onNodeWithTag("monster-1").performClick()
        compose.onNodeWithText("تجهيز الحملة").assertIsDisplayed().assertIsNotEnabled()
        captureTestScreenshot(compose,"beta5-target")
    }
}
