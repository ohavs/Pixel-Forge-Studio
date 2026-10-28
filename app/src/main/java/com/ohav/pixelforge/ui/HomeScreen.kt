package com.ohav.pixelforge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PIXEL FORGE", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "הגדרות") }
        }
        Text("סטודיו", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("עצב מאפס, מקצה לקצה", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        StudioCard("Watch Faces", "פני שעון ל-Galaxy Watch", listOf(Violet, Cyan), enabled = true) {}
        Spacer(Modifier.height(14.dp))
        StudioCard("Widgets", "ווידג'טים לטלפון · בקרוב", listOf(Color(0xFFFF7A59), Color(0xFFFF3D8B)), enabled = false) {}
    }
}

@Composable
private fun StudioCard(title: String, subtitle: String, colors: List<Color>, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(if (enabled) colors else colors.map { it.copy(alpha = .35f) }))
            .clickable(enabled = enabled, onClick = onClick).padding(22.dp),
    ) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = .85f))
        }
    }
}
