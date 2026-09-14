package com.faisal.strategygame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.faisal.strategygame.ui.FrontierApp
import com.faisal.strategygame.ui.theme.StrategyGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StrategyGameTheme {
                FrontierApp()
            }
        }
    }
}
