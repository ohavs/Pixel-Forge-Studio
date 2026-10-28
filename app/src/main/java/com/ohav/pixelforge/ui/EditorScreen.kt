package com.ohav.pixelforge.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohav.pixelforge.editor.*
import com.ohav.pixelforge.watch.FaceBuilder
import com.ohav.pixelforge.watch.WatchLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val Palette = listOf(
    0xFFFFFFFF, 0xFFB8BCC8, 0xFF8B7BFF, 0xFF6C5CE7, 0xFF3D7BFF, 0xFF00C2FF, 0xFF2EE59D,
    0xFFFFD166, 0xFFFF7A59, 0xFFFF6A4D, 0xFFFF3D8B, 0xFF000000,
)
private val Backgrounds = listOf(0xFF000000, 0xFF0E1015, 0xFF120E2E, 0xFF1A1033, 0xFF06231F, 0xFF2B0F16, 0xFF101C33, 0xFF2A2A2E)

/** Stage 1 watch face editor: round live canvas, layers, styling, undo/redo, send to watch. */
@Composable
fun EditorScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { EditorState(ctx) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var watch by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            watch = try { WatchLink.watchName(ctx) } catch (e: Exception) { null }
            delay(5000)
        }
    }

    fun send() = scope.launch {
        busy = true
        status = "בונה פני שעון…"
        status = try {
            val face = withContext(Dispatchers.IO) { FaceBuilder.build(ctx, FaceXml.build(state.doc)) }
            status = "שולח לשעון…"
            WatchLink.push(ctx, face)
        } catch (e: Throwable) {
            "שגיאה: ${e.javaClass.simpleName} ${e.message ?: ""}"
        }
        busy = false
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).enter(0), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה") }
            Column(Modifier.weight(1f)) {
                Text("פני שעון", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    watch?.let { "מחובר: $it" } ?: "שעון לא מחובר",
                    fontSize = 12.sp, color = if (watch != null) Color(0xFF2EE59D) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = state::undo, enabled = state.canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, "בטל") }
            IconButton(onClick = state::redo, enabled = state.canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, "בצע שוב") }
            FilledIconButton(onClick = { send() }, enabled = !busy && watch != null) { Icon(Icons.AutoMirrored.Filled.Send, "שלח לשעון") }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (status.isNotEmpty()) {
            Text(status, Modifier.fillMaxWidth().padding(horizontal = 20.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
        }
        FaceCanvas(state, Modifier.enter(1).padding(vertical = 14.dp).fillMaxWidth(.8f).aspectRatio(1f).align(Alignment.CenterHorizontally))
        TabRow(tab, Modifier.enter(2), containerColor = Color.Transparent) {
            listOf("הוסף", "שכבות", "עיצוב").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
        }
        AnimatedContent(tab, Modifier.weight(1f), transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "tab") { t ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (t) {
                    0 -> AddPanel(state) { tab = 2 }
                    1 -> LayersPanel(state)
                    else -> StylePanel(state)
                }
            }
        }
    }
}

@Composable
private fun FaceCanvas(state: EditorState, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }
    val time = now.format(DateTimeFormatter.ofPattern("hh:mm"))
    val date = now.format(DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH))
    val pulse by rememberInfiniteTransition(label = "sel").animateFloat(.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "p")
    val bg by animateColorAsState(Color(state.doc.background), tween(350), label = "bg")
    var guideX by remember { mutableStateOf(false) }
    var guideY by remember { mutableStateOf(false) }
    var dragId by remember { mutableStateOf<Long?>(null) }

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { o ->
                    val u = size.width / FACE_SIZE
                    state.selectedId = state.layerAt(o.x / u, o.y / u)?.id ?: state.selectedId
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { o ->
                        val u = size.width / FACE_SIZE
                        val hit = state.layerAt(o.x / u, o.y / u) ?: state.selected?.takeIf { !it.locked && it.visible }
                        dragId = hit?.id
                        hit?.let { state.selectedId = it.id }
                    },
                    onDragEnd = { dragId = null; guideX = false; guideY = false; state.commit() },
                    onDragCancel = { dragId = null; guideX = false; guideY = false; state.commit() },
                ) { change, d ->
                    val id = dragId ?: return@detectDragGestures
                    change.consume()
                    val u = size.width / FACE_SIZE
                    state.updateLayer(id) { l ->
                        // Raw position is kept off-model; snap to the centre lines within 6px.
                        val nx = (l.x + d.x / u).coerceIn(0f, FACE_SIZE)
                        val ny = (l.y + d.y / u).coerceIn(0f, FACE_SIZE)
                        guideX = abs(nx - FACE_SIZE / 2) < 6
                        guideY = abs(ny - FACE_SIZE / 2) < 6
                        l.copy(x = nx, y = ny)
                    }
                }
            },
    ) {
        val u = size.minDimension / FACE_SIZE
        drawCircle(Color(0xFF2A2D36), radius = size.minDimension / 2 + 6.dp.toPx())
        drawFace(state.doc, bg, measurer, time, date)
        val guide = Color(0xFFFF3D8B)
        if (guideX) drawLine(guide, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1.dp.toPx())
        if (guideY) drawLine(guide, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
        state.selected?.takeIf { it.visible }?.let { l ->
            val dash = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            val sel = Color(0xFF00C2FF).copy(alpha = pulse)
            if (l.kind in listOf(LayerKind.RING, LayerKind.ARC, LayerKind.TICKS, LayerKind.CIRCLE)) {
                drawCircle(sel, (l.size + l.thickness / 2 + 6) * u, Offset(l.x * u, l.y * u), style = dash)
            } else {
                val (w, h) = l.box()
                drawRect(sel, Offset((l.x - w / 2 - 4) * u, (l.y - h / 2 - 4) * u), Size((w + 8) * u, (h + 8) * u), style = dash)
            }
        }
    }
}

/** Draws [doc] scaled into this (square) draw area, clipped to the watch circle. */
private fun DrawScope.drawFace(doc: FaceDoc, bg: Color, measurer: TextMeasurer, time: String, date: String) {
    val u = size.minDimension / FACE_SIZE
    clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset.Zero, size)) }) {
        drawRect(bg)
        doc.layers.filter { it.visible }.forEach { l ->
            val c = Color(l.color)
            val center = Offset(l.x * u, l.y * u)
            when (l.kind) {
                LayerKind.RING -> drawCircle(c, l.size * u, center, style = Stroke(l.thickness * u))
                LayerKind.ARC -> drawArc(
                    c, l.angle - 90, l.height, false, Offset(center.x - l.size * u, center.y - l.size * u),
                    Size(2 * l.size * u, 2 * l.size * u), style = Stroke(l.thickness * u, cap = StrokeCap.Round),
                )
                LayerKind.TICKS -> l.ticks().forEach { (a, b) ->
                    drawLine(c, Offset(a.first * u, a.second * u), Offset(b.first * u, b.second * u), l.thickness * u, StrokeCap.Round)
                }
                LayerKind.CIRCLE -> drawCircle(c, l.size * u, center)
                LayerKind.RECT -> drawRoundRect(
                    c, Offset(center.x - l.size * u / 2, center.y - l.height * u / 2), Size(l.size * u, l.height * u),
                    CornerRadius(l.thickness * u),
                )
                else -> {
                    val t = measurer.measure(
                        l.sample(time, date),
                        TextStyle(color = c, fontSize = (l.size * u).toSp(), fontWeight = if (l.bold) FontWeight.Bold else FontWeight.Normal),
                    )
                    drawText(t, topLeft = Offset(center.x - t.size.width / 2f, center.y - t.size.height / 2f))
                }
            }
        }
    }
}

/** Static thumbnail of a face (presets). */
@Composable
private fun FaceThumb(doc: FaceDoc, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) { drawFace(doc, Color(doc.background), measurer, "10:08", "WED 28") }
}

private fun LayerKind.icon(): ImageVector = when (this) {
    LayerKind.TIME -> Icons.Outlined.Schedule
    LayerKind.DATE -> Icons.Outlined.CalendarToday
    LayerKind.TEXT -> Icons.Outlined.TextFields
    LayerKind.RING -> Icons.Outlined.RadioButtonUnchecked
    LayerKind.ARC -> Icons.Outlined.DataUsage
    LayerKind.TICKS -> Icons.Outlined.Timer
    LayerKind.CIRCLE -> Icons.Filled.Circle
    LayerKind.RECT -> Icons.Filled.Rectangle
}

@Composable
private fun AddPanel(state: EditorState, onAdded: () -> Unit) {
    Text("תבניות", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Presets.all.forEachIndexed { i, (name, doc) ->
            Column(Modifier.enter(i).clip(RoundedCornerShape(18.dp)).clickable { state.apply(doc) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FaceThumb(doc, Modifier.size(92.dp))
                Text(name, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    Text("שכבה חדשה", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    val kinds = LayerKind.entries
    kinds.chunked(3).forEachIndexed { row, items ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEachIndexed { i, k ->
                val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                Column(
                    Modifier.weight(1f).enter(row * 3 + i).pressScale(src).clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(src, null) { state.add(k, 0xFF8B7BFF); onAdded() }.padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(k.icon(), null, tint = Violet)
                    Spacer(Modifier.height(6.dp))
                    Text(k.title, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LayersPanel(state: EditorState) {
    val layers = state.doc.layers
    if (layers.isEmpty()) Text("אין שכבות עדיין — הוסף מהלשונית הראשונה", color = MaterialTheme.colorScheme.onSurfaceVariant)
    layers.asReversed().forEachIndexed { i, l ->
        val selected = l.id == state.selectedId
        val bgc by animateColorAsState(if (selected) Violet.copy(alpha = .22f) else MaterialTheme.colorScheme.surfaceVariant, label = "row")
        Row(
            Modifier.enter(i).fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(16.dp)).background(bgc)
                .clickable { state.selectedId = l.id }.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(l.kind.icon(), null, Modifier.size(20.dp), tint = Color(l.color).takeIf { l.color != 0xFF000000 } ?: Color.Gray)
            Text(l.name, Modifier.weight(1f).padding(horizontal = 10.dp), maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            IconButton({ state.editLayer(l.id) { it.copy(visible = !it.visible) } }) {
                Icon(if (l.visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, "הסתר")
            }
            IconButton({ state.editLayer(l.id) { it.copy(locked = !it.locked) } }) {
                Icon(if (l.locked) Icons.Filled.Lock else Icons.Outlined.LockOpen, "נעל")
            }
            IconButton({ state.move(l.id, 1) }, enabled = i > 0) { Icon(Icons.Filled.KeyboardArrowUp, "למעלה") }
            IconButton({ state.move(l.id, -1) }, enabled = i < layers.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, "למטה") }
            IconButton({ state.delete(l.id) }) { Icon(Icons.Outlined.Delete, "מחק") }
        }
    }
}

@Composable
private fun StylePanel(state: EditorState) {
    val l = state.selected
    if (l == null) {
        Text("בחר שכבה בקנבס או ברשימה", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Text(l.name, Modifier.enter(0), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        if (l.kind == LayerKind.TEXT) {
            OutlinedTextField(
                l.text, { v -> state.updateLayer(l.id) { it.copy(text = v.take(24)) } },
                Modifier.enter(1).fillMaxWidth().onDone(state), singleLine = true, label = { Text("טקסט") },
            )
            Spacer(Modifier.height(8.dp))
        }
        Swatches("צבע", Palette, l.color, 2) { c -> state.editLayer(l.id) { it.copy(color = c) } }
        val (sizeLabel, range) = when (l.kind) {
            LayerKind.TIME -> "גודל" to 30f..160f
            LayerKind.DATE, LayerKind.TEXT -> "גודל" to 14f..80f
            LayerKind.RING, LayerKind.ARC, LayerKind.TICKS -> "רדיוס" to 20f..225f
            LayerKind.CIRCLE -> "רדיוס" to 4f..225f
            LayerKind.RECT -> "רוחב" to 10f..450f
        }
        LabeledSlider(sizeLabel, l.size, range, state, 3) { v -> state.updateLayer(l.id) { it.copy(size = v) } }
        if (l.kind == LayerKind.RECT) LabeledSlider("גובה", l.height, 6f..450f, state, 4) { v -> state.updateLayer(l.id) { it.copy(height = v) } }
        if (l.kind in listOf(LayerKind.RING, LayerKind.ARC, LayerKind.TICKS)) {
            LabeledSlider("עובי", l.thickness, 1f..60f, state, 4) { v -> state.updateLayer(l.id) { it.copy(thickness = v) } }
        }
        if (l.kind == LayerKind.ARC) {
            LabeledSlider("התחלה", l.angle, 0f..360f, state, 4) { v -> state.updateLayer(l.id) { it.copy(angle = v) } }
            LabeledSlider("אורך", l.height, 5f..360f, state, 4) { v -> state.updateLayer(l.id) { it.copy(height = v) } }
        }
        if (l.kind == LayerKind.TICKS) {
            LabeledSlider("אורך סימון", l.height, 2f..80f, state, 4) { v -> state.updateLayer(l.id) { it.copy(height = v) } }
            LabeledSlider("כמות", l.count.toFloat(), 4f..120f, state, 4) { v -> state.updateLayer(l.id) { it.copy(count = v.toInt()) } }
        }
        if (l.kind == LayerKind.RECT) LabeledSlider("פינות", l.thickness, 0f..(minOf(l.size, l.height) / 2), state, 5) { v -> state.updateLayer(l.id) { it.copy(thickness = v) } }
        if (l.kind.isText) {
            Row(Modifier.enter(5), verticalAlignment = Alignment.CenterVertically) {
                Text("מודגש", Modifier.weight(1f))
                Switch(l.bold, { b -> state.editLayer(l.id) { it.copy(bold = b) } })
            }
        }
        Row(Modifier.enter(6).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip({ state.editLayer(l.id) { it.copy(x = FACE_SIZE / 2) } }, { Text("מרכז אופקי") })
            AssistChip({ state.editLayer(l.id) { it.copy(y = FACE_SIZE / 2) } }, { Text("מרכז אנכי") })
        }
    }
    Spacer(Modifier.height(18.dp))
    HorizontalDivider()
    Spacer(Modifier.height(14.dp))
    Swatches("רקע", Backgrounds, state.doc.background, 7) { c -> state.edit { it.copy(background = c) } }
}

/** Commits the text field's edits as one undo step when it loses focus. */
private fun Modifier.onDone(state: EditorState) = this.then(
    Modifier.onFocusChanged { if (!it.isFocused) state.commit() },
)

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, state: EditorState, index: Int, onChange: (Float) -> Unit) {
    Column(Modifier.enter(index)) {
        Row {
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(value.toInt().toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value.coerceIn(range), onChange, valueRange = range, onValueChangeFinished = state::commit)
    }
}

@Composable
private fun Swatches(title: String, colors: List<Long>, selected: Long, index: Int, onPick: (Long) -> Unit) {
    Column(Modifier.enter(index).padding(bottom = 8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { c ->
                val ring by animateColorAsState(if (c == selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "ring")
                Box(
                    Modifier.size(38.dp).border(3.dp, ring, CircleShape).padding(5.dp).clip(CircleShape)
                        .background(Color(c)).border(1.dp, Color.White.copy(alpha = .15f), CircleShape).clickable { onPick(c) },
                )
            }
        }
    }
}
