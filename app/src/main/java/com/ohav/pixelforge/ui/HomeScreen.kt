package com.ohav.pixelforge.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(onSettings: () -> Unit, onWatchFaces: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Row(Modifier.enter(0), verticalAlignment = Alignment.CenterVertically) {
            Text("ORBIT STUDIO", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "הגדרות") }
        }
        Text("סטודיו", Modifier.enter(1), fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text("עצב מאפס, מקצה לקצה", Modifier.enter(2), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        StudioCard("Watch Faces", "עורך פני שעון ל-Galaxy Watch", listOf(Violet, Cyan), true, Modifier.enter(3), onWatchFaces)
        Spacer(Modifier.height(14.dp))
        StudioCard("Widgets", "ווידג'טים לטלפון · בקרוב", listOf(Color(0xFFFF7A59), Color(0xFFFF3D8B)), false, Modifier.enter(4)) {}
    }
}

@Composable
private fun StudioCard(title: String, subtitle: String, colors: List<Color>, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val t by rememberInfiniteTransition(label = "card").animateFloat(0f, 6.2832f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t")
    val c = if (enabled) colors else colors.map { it.copy(alpha = .4f) }
    Box(
        modifier.pressScale(src).fillMaxWidth().height(170.dp).clip(RoundedCornerShape(30.dp))
            .clickable(src, null, enabled = enabled, onClick = onClick),
    ) {
        // Living gradient: two soft light blobs orbit over the base gradient.
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(c, Offset(size.width * (.5f + .5f * cos(t)), 0f), Offset(size.width * (.5f - .5f * cos(t)), size.height)))
            val r = size.minDimension
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .35f), Color.Transparent), Offset(size.width * (.5f + .4f * cos(t)), size.height * (.5f + .4f * sin(t * 2))), r * .7f), r * .7f, Offset(size.width * (.5f + .4f * cos(t)), size.height * (.5f + .4f * sin(t * 2))))
            drawCircle(Brush.radialGradient(listOf(c.last().copy(alpha = .6f), Color.Transparent), Offset(size.width * (.5f - .35f * sin(t)), size.height * (.5f + .3f * cos(t))), r * .8f), r * .8f, Offset(size.width * (.5f - .35f * sin(t)), size.height * (.5f + .3f * cos(t))))
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(22.dp)) {
            Text(title, Modifier.fillMaxWidth(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, Modifier.fillMaxWidth(), color = Color.White.copy(alpha = .88f))
        }
    }
}
