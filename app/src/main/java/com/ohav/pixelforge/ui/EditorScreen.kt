package com.ohav.pixelforge.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
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
            val doc = state.doc
            val face = withContext(Dispatchers.IO) {
                FaceBuilder.build(ctx, FaceXml.build(doc), FaceXml.imageLayers(doc).map { ImageAssets.render(ctx, it) })
            }
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
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000L - now.nano / 1_000_000) } }
    val pulse by rememberInfiniteTransition(label = "sel").animateFloat(.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "p")
    val bg by animateColorAsState(Color(state.doc.background), tween(350), label = "bg")
    val images = rememberImages()
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
        drawFace(state.doc, bg, measurer, now, images)
        val guide = Color(0xFFFF3D8B)
        if (guideX) drawLine(guide, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1.dp.toPx())
        if (guideY) drawLine(guide, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
        state.selected?.takeIf { it.visible }?.let { l ->
            val dash = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            val sel = Color(0xFF00C2FF).copy(alpha = pulse)
            if (l.kind in listOf(LayerKind.RING, LayerKind.ARC, LayerKind.TICKS, LayerKind.HANDS, LayerKind.CIRCLE)) {
                drawCircle(sel, (l.size + l.thickness / 2 + 6) * u, Offset(l.x * u, l.y * u), style = dash)
            } else {
                val (w, h) = l.box()
                drawRect(sel, Offset((l.x - w / 2 - 4) * u, (l.y - h / 2 - 4) * u), Size((w + 8) * u, (h + 8) * u), style = dash)
            }
        }
    }
}

private val Weights = listOf(FontWeight.Thin, FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.Bold)

/** Draws [doc] scaled into this (square) draw area, clipped to the watch circle; mirrors [FaceXml]. */
private fun DrawScope.drawFace(doc: FaceDoc, bg: Color, measurer: TextMeasurer, now: LocalDateTime, images: (String) -> ImageBitmap?) {
    val u = size.minDimension / FACE_SIZE
    val date = now.format(DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH))
    clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset.Zero, size)) }) {
        drawRect(bg)
        doc.layers.filter { it.visible }.forEach { l ->
            val c = Color(l.color)
            val center = Offset(l.x * u, l.y * u)
            val r = l.size * u
            val brush = if (l.color2 == 0L) SolidColor(c) else Brush.linearGradient(
                listOf(c, Color(l.color2)), Offset(center.x - l.box().first * u / 2, center.y - l.box().second * u / 2),
                Offset(center.x + l.box().first * u / 2, center.y + l.box().second * u / 2),
            )
            when (l.kind) {
                LayerKind.RING -> drawCircle(brush, r, center, style = Stroke(l.thickness * u))
                LayerKind.ARC -> drawArc(
                    brush, l.angle - 90, l.height, false, Offset(center.x - r, center.y - r),
                    Size(2 * r, 2 * r), style = Stroke(l.thickness * u, cap = StrokeCap.Round),
                )
                LayerKind.TICKS -> l.ticks().forEach { (a, b) ->
                    drawLine(c, Offset(a.first * u, a.second * u), Offset(b.first * u, b.second * u), l.thickness * u, StrokeCap.Round)
                }
                LayerKind.HANDS -> {
                    val t = l.thickness * u
                    val sec = now.second + now.nano / 1e9f
                    val min = now.minute + sec / 60
                    fun hand(deg: Float, len: Float, w: Float, col: Color) = rotate(deg, center) {
                        drawLine(col, Offset(center.x, center.y + w * 1.6f), Offset(center.x, center.y - len), w, StrokeCap.Round)
                    }
                    val accent = if (l.color2 == 0L) c else Color(l.color2)
                    hand((now.hour % 12 + min / 60) * 30, r * .62f, t * 1.15f, c)
                    hand(min * 6, r, t, c)
                    if (l.extra) hand(now.second * 6f, r * 1.08f, maxOf(t * .35f, 2f * u), accent)
                    drawCircle(accent, t * .9f, center)
                }
                LayerKind.ICON -> rotate(l.rotation, center) {
                    val s = l.size * u / IconLibrary.VIEWPORT
                    withTransform({ translate(center.x - l.size * u / 2, center.y - l.size * u / 2); scale(s, s, Offset.Zero) }) {
                        IconLibrary.paths(l.text).forEach { drawPath(it, c) }
                    }
                }
                LayerKind.IMAGE -> rotate(l.rotation, center) {
                    val w = l.size * u; val h = l.height * u
                    val tl = Offset(center.x - w / 2, center.y - h / 2)
                    val img = images(l.text)
                    val clip = Path().apply { addRoundRect(RoundRect(tl.x, tl.y, tl.x + w, tl.y + h, CornerRadius(l.thickness * u))) }
                    clipPath(clip) {
                        if (img == null) drawRect(Color.Gray.copy(alpha = .3f), tl, Size(w, h))
                        else {
                            val src = ImageAssets.centerCrop(img.width, img.height, w, h)
                            drawImage(
                                img, IntOffset(src.left, src.top), IntSize(src.width(), src.height()),
                                IntOffset(tl.x.toInt(), tl.y.toInt()), IntSize(w.toInt(), h.toInt()), alpha = c.alpha,
                            )
                        }
                    }
                }
                LayerKind.CIRCLE -> drawCircle(brush, r, center)
                LayerKind.RECT -> rotate(l.rotation, center) {
                    drawRoundRect(brush, Offset(center.x - r / 2, center.y - l.height * u / 2), Size(r, l.height * u), CornerRadius(l.thickness * u))
                }
                else -> {
                    val text = when (l.kind) {
                        LayerKind.TIME -> now.format(DateTimeFormatter.ofPattern(if (l.extra) "hh:mm:ss" else "hh:mm"))
                        LayerKind.DATE -> date
                        else -> l.text
                    }
                    val m = measurer.measure(text, TextStyle(color = c, fontSize = (l.size * u).toSp(), fontWeight = Weights[l.weight.coerceIn(Weights.indices)]))
                    rotate(l.rotation, center) { drawText(m, topLeft = Offset(center.x - m.size.width / 2f, center.y - m.size.height / 2f)) }
                }
            }
        }
    }
}

/** Static thumbnail of a face (presets). */
@Composable
private fun FaceThumb(doc: FaceDoc, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) { drawFace(doc, Color(doc.background), measurer, ThumbTime) { null } }
}

private val ThumbTime = LocalDateTime.of(2026, 10, 28, 10, 8, 32)

/** Imported pictures for the live preview, decoded once per file name. */
@Composable
private fun rememberImages(): (String) -> ImageBitmap? {
    val ctx = LocalContext.current
    val cache = remember { HashMap<String, ImageBitmap?>() }
    return { name -> cache.getOrPut(name) { ImageAssets.load(ctx, name)?.asImageBitmap() } }
}

private fun LayerKind.icon(): ImageVector = when (this) {
    LayerKind.TIME -> Icons.Outlined.Schedule
    LayerKind.DATE -> Icons.Outlined.CalendarToday
    LayerKind.TEXT -> Icons.Outlined.TextFields
    LayerKind.RING -> Icons.Outlined.RadioButtonUnchecked
    LayerKind.ARC -> Icons.Outlined.DataUsage
    LayerKind.TICKS -> Icons.Outlined.Timer
    LayerKind.HANDS -> Icons.Outlined.AccessTime
    LayerKind.ICON -> Icons.Outlined.EmojiEmotions
    LayerKind.IMAGE -> Icons.Outlined.Image
    LayerKind.CIRCLE -> Icons.Filled.Circle
    LayerKind.RECT -> Icons.Outlined.CropSquare
}

@Composable
private fun AddPanel(state: EditorState, onAdded: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val (name, aspect) = withContext(Dispatchers.IO) { ImageAssets.import(ctx, uri) }
            val w = if (aspect >= 1) 220f else 220f * aspect
            state.add(Layer.create(LayerKind.IMAGE, 0, 0).copy(text = name, size = w, height = w / aspect, thickness = 24f, color = 0xFFFFFFFF))
            onAdded()
        }
    }
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
                        .clickable(src, null) {
                            if (k == LayerKind.IMAGE) pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            else { state.add(k, 0xFF8B7BFF); onAdded() }
                        }.padding(vertical = 18.dp),
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
        val alpha = (l.color ushr 24 and 0xFF) / 255f
        if (l.kind == LayerKind.ICON) IconPicker(l.text) { n -> state.editLayer(l.id) { it.copy(text = n) } }
        if (l.kind != LayerKind.IMAGE) Swatches("צבע", Palette, l.color or 0xFF000000, 2) { c -> state.editLayer(l.id) { it.copy(color = withAlpha(c, alpha)) } }
        LabeledSlider("אטימות", alpha * 100, 5f..100f, state, 2) { v -> state.updateLayer(l.id) { it.copy(color = withAlpha(it.color, v / 100)) } }
        if (l.kind != LayerKind.TICKS && !l.kind.isText && !l.kind.isImage) {
            val secondLabel = if (l.kind == LayerKind.HANDS) "מחוג שניות" else "גרדיאנט (צבע שני)"
            Swatches(secondLabel, listOf(0L) + Palette, l.color2, 2) { c -> state.editLayer(l.id) { it.copy(color2 = c) } }
        }
        val (sizeLabel, range) = when (l.kind) {
            LayerKind.TIME -> "גודל" to 30f..160f
            LayerKind.DATE, LayerKind.TEXT -> "גודל" to 14f..80f
            LayerKind.RING, LayerKind.ARC, LayerKind.TICKS -> "רדיוס" to 20f..225f
            LayerKind.HANDS -> "אורך" to 40f..225f
            LayerKind.CIRCLE -> "רדיוס" to 4f..225f
            LayerKind.RECT -> "רוחב" to 10f..450f
            LayerKind.ICON -> "גודל" to 12f..300f
            LayerKind.IMAGE -> "גודל" to 30f..450f
        }
        LabeledSlider(sizeLabel, l.size, range, state, 3) { v ->
            // Images keep their aspect ratio.
            state.updateLayer(l.id) { if (it.kind == LayerKind.IMAGE) it.copy(size = v, height = v * it.height / it.size) else it.copy(size = v) }
        }
        if (l.kind == LayerKind.IMAGE) LabeledSlider("פינות", l.thickness, 0f..(minOf(l.size, l.height) / 2), state, 4) { v -> state.updateLayer(l.id) { it.copy(thickness = v) } }
        if (l.kind == LayerKind.RECT) LabeledSlider("גובה", l.height, 6f..450f, state, 4) { v -> state.updateLayer(l.id) { it.copy(height = v) } }
        if (l.kind in listOf(LayerKind.RING, LayerKind.ARC, LayerKind.TICKS, LayerKind.HANDS)) {
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
            Text("משקל", Modifier.enter(5), fontWeight = FontWeight.SemiBold)
            Row(Modifier.enter(5).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("דק", "קל", "רגיל", "בינוני", "עבה").forEachIndexed { i, t ->
                    FilterChip(l.weight == i, { state.editLayer(l.id) { it.copy(weight = i) } }, { Text(t, fontWeight = Weights[i]) })
                }
            }
        }
        if (l.kind == LayerKind.TEXT || l.kind == LayerKind.RECT || l.kind.isImage) {
            LabeledSlider("סיבוב", l.rotation, -180f..180f, state, 5) { v -> state.updateLayer(l.id) { it.copy(rotation = v) } }
        }
        if (l.kind == LayerKind.TIME || l.kind == LayerKind.HANDS) {
            Row(Modifier.enter(5), verticalAlignment = Alignment.CenterVertically) {
                Text(if (l.kind == LayerKind.TIME) "הצג שניות" else "מחוג שניות", Modifier.weight(1f))
                Switch(l.extra, { b -> state.editLayer(l.id) { it.copy(extra = b) } })
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

@Composable
private fun IconPicker(selected: String, onPick: (String) -> Unit) {
    var cat by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.enter(1).padding(bottom = 10.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconLibrary.categories.forEachIndexed { i, (name, _) -> FilterChip(cat == i, { cat = i }, { Text(name) }) }
        }
        Spacer(Modifier.height(6.dp))
        IconLibrary.categories[cat].second.chunked(6).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (name, v) ->
                    val bgc by animateColorAsState(if (name == selected) Violet.copy(alpha = .35f) else MaterialTheme.colorScheme.surfaceVariant, label = "ic")
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(bgc).clickable { onPick(name) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(v, name) }
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun withAlpha(c: Long, a: Float): Long = ((a * 255).toLong().coerceIn(0, 255) shl 24) or (c and 0xFFFFFF)

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
                        .background(if (c == 0L) Color.Transparent else Color(c)).border(1.dp, Color.White.copy(alpha = .25f), CircleShape)
                        .clickable { onPick(c) },
                    contentAlignment = Alignment.Center,
                ) { if (c == 0L) Icon(Icons.Outlined.Block, "ללא", Modifier.size(18.dp)) }
            }
        }
    }
}
