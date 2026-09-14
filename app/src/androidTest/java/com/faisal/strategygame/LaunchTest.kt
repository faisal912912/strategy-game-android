package com.faisal.strategygame

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun portraitRegistrationRequiresCredentials() {
        compose.onNodeWithText("حُدود المملكة").assertExists()
        val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val directory=File(compose.activity.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
        File(directory,"portrait-login.png").outputStream().use {screenshot.compress(Bitmap.CompressFormat.PNG,100,it)}
        compose.onNodeWithText("حساب جديد").performClick()
        compose.onNodeWithText("أنشئ مملكتي").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("اسم الحساب").performTextInput("governor")
        compose.onNodeWithText("كلمة المرور").performTextInput("short")
        compose.onNodeWithText("أنشئ مملكتي").assertIsNotEnabled()
        check(compose.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT)
    }
}
