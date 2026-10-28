package com.ohav.pixelforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.ohav.pixelforge.ui.HomeScreen
import com.ohav.pixelforge.ui.SettingsScreen
import com.ohav.pixelforge.ui.StudioTheme
import com.ohav.pixelforge.ui.EditorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudioTheme {
                var screen by rememberSaveable { mutableStateOf("home") }
                BackHandler(screen != "home") { screen = "home" }
                AnimatedContent(screen, transitionSpec = {
                    val dir = if (targetState == "home") -1 else 1
                    (slideInHorizontally(tween(380)) { -dir * it / 4 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(380)) { dir * it / 4 } + fadeOut(tween(200)))
                }, label = "screen") { s ->
                    when (s) {
                        "settings" -> SettingsScreen(onBack = { screen = "home" })
                        "watch" -> EditorScreen(onBack = { screen = "home" })
                        else -> HomeScreen(onSettings = { screen = "settings" }, onWatchFaces = { screen = "watch" })
                    }
                }
            }
        }
    }
}
