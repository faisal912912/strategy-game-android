package com.faisal.strategygame

import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.faisal.strategygame.ui.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SceneInteractionTest {
    @get:Rule val compose=createComposeRule()
    private fun screenshot(name:String)=captureTestScreenshot(compose,name)
    @Test fun cityBuildingOpensAndCameraControlsRemainReachable() {
        var selected=""
        compose.setContent {MaterialTheme {TownBoard(townSpots.associate{it.key to 3},null,true){selected=it}}}
        compose.onNodeWithTag("building-castle").performClick()
        compose.runOnIdle {assertEquals("castle",selected)}
        screenshot("city")
        compose.onNodeWithContentDescription("تصغير المدينة").performClick()
        compose.onNodeWithTag("town-board").performTouchInput{swipeLeft()}
        compose.onNodeWithContentDescription("مركز المدينة").performClick()
        compose.onNodeWithTag("building-castle").assertExists()
    }
    @Test fun worldPanAndZoomKeepTargetsInteractive() {
        var camera by mutableStateOf(MapCamera(250f,250f))
        var selected=""
        val pins=listOf(WorldPin("qa-city","city",250f,250f,"مدينة الاختبار",0,JSONObject()),WorldPin("qa-forest","gather",270f,270f,"خشب",3,JSONObject("{\"node_type\":\"wood\"}")))
        compose.setContent {MaterialTheme {WorldBoard(camera,{camera=it},pins,true){selected=it.key}}}
        compose.onNodeWithTag("qa-city").performClick()
        compose.runOnIdle{assertEquals("qa-city",selected)}
        screenshot("world")
        compose.onNodeWithTag("world-board").performTouchInput{swipeLeft()}
        compose.runOnIdle{assertTrue(camera.x>250f);camera=MapCamera(250f,250f,2f)}
        compose.onNodeWithTag("qa-city").performClick()
    }
    @Test fun heroAndTroopArtworkAndLockedTierRender() {
        compose.setContent {MaterialTheme {Column {ArtworkBanner(R.drawable.hero_eagle,"عين الصقر","قاعة الأبطال");ArtworkBanner(R.drawable.unit_infantry,"المشاة","الفئة الخامسة");TierRequirements(5,1,"barracks")}}}
        compose.onNodeWithText("T5 • ثكنة المشاة مستوى 25").assertExists()
        screenshot("hero-army")
    }
}

internal fun captureTestScreenshot(compose:ComposeContentTestRule,name:String) {
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        val resolver=InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val uri=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,ContentValues().apply{
            put(MediaStore.Images.Media.DISPLAY_NAME,"$name.png")
            put(MediaStore.Images.Media.MIME_TYPE,"image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/FrontierQA")
        })!!
        resolver.openOutputStream(uri)!!.use{assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
    }
