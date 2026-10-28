package com.ohav.pixelforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.ohav.pixelforge.ui.HomeScreen
import com.ohav.pixelforge.ui.SettingsScreen
import com.ohav.pixelforge.ui.StudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudioTheme {
                var screen by rememberSaveable { mutableStateOf("home") }
                BackHandler(screen != "home") { screen = "home" }
                when (screen) {
                    "settings" -> SettingsScreen(onBack = { screen = "home" })
                    else -> HomeScreen(onSettings = { screen = "settings" })
                }
            }
        }
    }
}
