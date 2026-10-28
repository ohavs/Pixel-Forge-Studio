package com.ohav.pixelforge.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohav.pixelforge.watch.FaceBuilder
import com.ohav.pixelforge.watch.WatchFaceXml
import com.ohav.pixelforge.watch.WatchLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Backgrounds = listOf(0xFF0E1015, 0xFF1A1033, 0xFF06231F, 0xFF2B0F16, 0xFF101C33).map { Color(it) }
private val Accents = listOf(0xFF3D7BFF, 0xFF00C2FF, 0xFF6C5CE7, 0xFF2EE59D, 0xFFFF7A59, 0xFFFF3D8B, 0xFFFFD166).map { Color(it) }

/** Stage 0b feasibility lab: design a tiny face, build it on the phone and push it to the watch. */
@Composable
fun WatchLabScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bg by remember { mutableStateOf(Backgrounds[1]) }
    var accent by remember { mutableStateOf(Accents[1]) }
    var label by remember { mutableStateOf("MY FACE") }
    var watch by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            watch = try { WatchLink.watchName(ctx) } catch (e: Exception) { null }
            delay(5000)
        }
    }

    fun send(build: Boolean) = scope.launch {
        busy = true
        status = if (build) "בונה פני שעון בטלפון…" else "מכין תבנית…"
        status = try {
            val face = withContext(Dispatchers.IO) {
                if (build) FaceBuilder.build(ctx, WatchFaceXml.simple(argb(bg), argb(accent), label)) else FaceBuilder.template(ctx)
            }
            status = "שולח לשעון…"
            WatchLink.push(ctx, face)
        } catch (e: Throwable) {
            "שגיאה: ${e.javaClass.simpleName} ${e.message ?: ""}"
        }
        busy = false
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.enter(0)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה") }
        Text("מעבדת שעון", Modifier.enter(1), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            watch?.let { "מחובר: $it" } ?: "מחפש שעון עם Pixel Forge…",
            Modifier.enter(2), color = if (watch != null) Color(0xFF2EE59D) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        FacePreview(bg, accent, label, Modifier.enter(3).align(Alignment.CenterHorizontally).size(260.dp))
        Spacer(Modifier.height(20.dp))
        Swatches("רקע", Backgrounds, bg, 4) { bg = it }
        Spacer(Modifier.height(12.dp))
        Swatches("הדגשה", Accents, accent, 5) { accent = it }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(label, { label = it.take(16) }, Modifier.enter(6).fillMaxWidth(), singleLine = true, label = { Text("טקסט") })
        Spacer(Modifier.height(18.dp))
        Button({ send(true) }, Modifier.enter(7).fillMaxWidth().height(54.dp), enabled = !busy && watch != null, shape = RoundedCornerShape(18.dp)) {
            Text("בנה ושלח לשעון", fontSize = 17.sp)
        }
        TextButton({ send(false) }, Modifier.enter(8).fillMaxWidth(), enabled = !busy && watch != null) { Text("שלח תבנית בסיס (בדיקה)") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        if (status.isNotEmpty()) Text(status, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun argb(c: Color) = "#%08x".format(c.toArgb())

@Composable
private fun Swatches(title: String, colors: List<Color>, selected: Color, index: Int, onPick: (Color) -> Unit) {
    Column(Modifier.enter(index)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colors.forEach { c ->
                val ring by animateColorAsState(if (c == selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "ring")
                Box(Modifier.size(38.dp).border(3.dp, ring, CircleShape).padding(5.dp).clip(CircleShape).background(c).clickable { onPick(c) })
            }
        }
    }
}

/** Live preview mirroring [WatchFaceXml.simple]: 450-unit canvas scaled to the composable. */
@Composable
private fun FacePreview(bg: Color, accent: Color, label: String, modifier: Modifier) {
    val bgA by animateColorAsState(bg, tween(400), label = "bg")
    val accA by animateColorAsState(accent, tween(400), label = "acc")
    val measurer = rememberTextMeasurer()
    var time by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { time = LocalTime.now(); delay(1000) } }
    val sweep by rememberInfiniteTransition(label = "sweep").animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "s")
    Canvas(modifier) {
        val u = size.minDimension / 450f
        drawCircle(bgA)
        drawCircle(accA, radius = 210f * u, style = Stroke(6f * u))
        drawArc(Color.White.copy(alpha = .35f), sweep, 40f, false, topLeft = center.copy(center.x - 210f * u, center.y - 210f * u),
            size = androidx.compose.ui.geometry.Size(420f * u, 420f * u), style = Stroke(6f * u))
        val t = measurer.measure(time.format(DateTimeFormatter.ofPattern("hh:mm")), TextStyle(color = Color.White, fontSize = (110f * u).toSp(), fontWeight = FontWeight.Bold))
        drawText(t, topLeft = center.copy(center.x - t.size.width / 2f, 215f * u - t.size.height / 2f))
        val l = measurer.measure(label, TextStyle(color = accA, fontSize = (26f * u).toSp(), fontWeight = FontWeight.Bold))
        drawText(l, topLeft = center.copy(center.x - l.size.width / 2f, 315f * u - l.size.height / 2f))
    }
}
