package com.ohav.pixelforge.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF3D7BFF)
val Violet = Color(0xFF6C5CE7)
val Cyan = Color(0xFF00C2FF)

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(primary = Accent, background = Color(0xFF0E1015), surface = Color(0xFF171A21), surfaceVariant = Color(0xFF22262F))
    else lightColorScheme(primary = Accent, background = Color(0xFFF4F5F9), surface = Color.White, surfaceVariant = Color(0xFFE9ECF3))
    // Hebrew UI: always right-to-left, whatever the device language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
