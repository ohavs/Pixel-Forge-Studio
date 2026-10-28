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
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.input.ImeAction
import com.ohav.pixelforge.widget.StudioWidgets
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val Palette = listOf(
    0xFFFFFFFF, 0xFFB8BCC8, 0xFF8B7BFF, 0xFF6C5CE7, 0xFF3D7BFF, 0xFF00C2FF, 0xFF2EE59D,
    0xFFFFD166, 0xFFFF7A59, 0xFFFF6A4D, 0xFFFF3D8B, 0xFF000000,
)
private val Backgrounds = listOf(0xFF000000, 0xFF0E1015, 0xFF120E2E, 0xFF1A1033, 0xFF06231F, 0xFF2B0F16, 0xFF101C33, 0xFF2A2A2E)

/**
 * The editor both studios share: live canvas, layers, styling, undo/redo. Watch faces are round
 * and go to the watch; widgets are rounded rects in four sizes and go to the home screen.
 */
@Composable
fun EditorScreen(studio: Studio, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { EditorState(ctx, studio) }
    val isFace = studio == Studio.FACE
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var watch by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    if (isFace) LaunchedEffect(Unit) {
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
            val slots = FaceXml.slots(doc)
            Iconify.ensure(ctx, slots.map { it.layer }.filter { it.kind == LayerKind.ICON }.map { it.text })
            val face = withContext(Dispatchers.IO) {
                FaceBuilder.build(ctx, FaceXml.build(doc), slots.map { ImageAssets.render(ctx, it) })
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
                Text(studio.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (isFace) Text(
                    watch?.let { "מחובר: $it" } ?: "שעון לא מחובר",
                    fontSize = 12.sp, color = if (watch != null) Color(0xFF2EE59D) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else Text("למסך הבית של הטלפון", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = state::undo, enabled = state.canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, "בטל") }
            IconButton(onClick = state::redo, enabled = state.canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, "בצע שוב") }
            IconButton(onClick = { state.saveCopy(); status = "נשמר בפנים שלי" }) { Icon(Icons.Outlined.Save, "שמור") }
            if (isFace) FilledIconButton(onClick = { send() }, enabled = !busy && watch != null) { Icon(Icons.AutoMirrored.Filled.Send, "שלח לשעון") }
            else {
                IconButton(onClick = { scope.launch { status = StudioWidgets.applyToAll(ctx, state.doc) } }) { Icon(Icons.Outlined.Sync, "עדכן ווידג'טים במסך") }
                FilledIconButton(onClick = { scope.launch { status = StudioWidgets.pin(ctx, state.doc) } }) { Icon(Icons.Outlined.AddToHomeScreen, "הוסף למסך הבית") }
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (status.isNotEmpty()) {
            Text(status, Modifier.fillMaxWidth().padding(horizontal = 20.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
        }
        val aspect = state.doc.w / state.doc.h
        FaceCanvas(
            state,
            Modifier.enter(1).padding(vertical = 14.dp).fillMaxWidth(if (aspect > 1.2f) .92f else .78f)
                .aspectRatio(aspect).align(Alignment.CenterHorizontally),
        )
        TabRow(tab, Modifier.enter(2), containerColor = Color.Transparent) {
            listOf("מחולל", "הוסף", "שכבות", "עיצוב").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
        }
        AnimatedContent(tab, Modifier.weight(1f), transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "tab") { t ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (t) {
                    0 -> ForgePanel(state)
                    1 -> AddPanel(state) { tab = 3 }
                    2 -> LayersPanel(state)
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
    val assets = rememberAssets()
    var guideX by remember { mutableStateOf(false) }
    var guideY by remember { mutableStateOf(false) }

    Canvas(
        modifier
            .pointerInput(Unit) {
                // One gesture handler: tap selects, one finger moves, two fingers pinch-resize.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val doc = state.doc
                    val u = size.width / doc.w
                    val hit = state.layerAt(down.position.x / u, down.position.y / u)
                    val id = hit?.id ?: state.selected?.takeIf { !it.locked && it.visible }?.id
                    var travel = 0f
                    var moving = false
                    do {
                        val event = awaitPointerEvent()
                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()
                        travel += pan.getDistance()
                        if (!moving && id != null && (travel > viewConfiguration.touchSlop || zoom != 1f)) {
                            moving = true; state.selectedId = id
                        }
                        if (moving && id != null) {
                            state.updateLayer(id) { l ->
                                val nx = (l.x + pan.x / u).coerceIn(0f, doc.w)
                                val ny = (l.y + pan.y / u).coerceIn(0f, doc.h)
                                guideX = abs(nx - doc.w / 2) < 6
                                guideY = abs(ny - doc.h / 2) < 6
                                (if (zoom != 1f) l.scaled(zoom) else l).copy(x = nx, y = ny)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (moving) state.commit() else hit?.let { state.selectedId = it.id }
                    guideX = false; guideY = false
                }
            },
    ) {
        val doc = state.doc
        val u = size.width / doc.w
        val rim = 6.dp.toPx()
        if (doc.round) drawCircle(Color(0xFF2A2D36), radius = size.minDimension / 2 + rim)
        else drawRoundRect(Color(0xFF2A2D36), Offset(-rim, -rim), Size(size.width + 2 * rim, size.height + 2 * rim), CornerRadius(doc.corner * u + rim))
        drawDoc(doc, bg, measurer, now, assets)
        val guide = Color(0xFFFF3D8B)
        if (guideX) drawLine(guide, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1.dp.toPx())
        if (guideY) drawLine(guide, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
        state.selected?.takeIf { it.visible }?.let { l ->
            val dash = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            val sel = Color(0xFF00C2FF).copy(alpha = pulse)
            if (l.kind in listOf(LayerKind.RING, LayerKind.ARC, LayerKind.TICKS, LayerKind.HANDS, LayerKind.CIRCLE, LayerKind.GAUGE)) {
                drawCircle(sel, (l.size + l.thickness / 2 + 6) * u, Offset(l.x * u, l.y * u), style = dash)
            } else {
                val (w, h) = l.box()
                drawRect(sel, Offset((l.x - w / 2 - 4) * u, (l.y - h / 2 - 4) * u), Size((w + 8) * u, (h + 8) * u), style = dash)
            }
        }
    }
}

/** Static thumbnail of a design (presets, saved): 92dp tall, as wide as its aspect needs (max 150dp). */
@Composable
internal fun FaceThumb(doc: FaceDoc, assets: DocAssets, height: androidx.compose.ui.unit.Dp = 92.dp) {
    val measurer = rememberTextMeasurer()
    val aspect = doc.w / doc.h
    val mod = if (aspect > 1.6f) Modifier.width(height * 1.63f).aspectRatio(aspect) else Modifier.height(height).aspectRatio(aspect)
    Canvas(mod) { drawDoc(doc, Color(doc.background), measurer, ThumbTime, assets) }
}

private val SavedTime = DateTimeFormatter.ofPattern("d/M HH:mm")

private val ThumbTime = LocalDateTime.of(2026, 10, 28, 10, 8, 32)

/**
 * Pictures for drawing: imported images decoded once per file name; Iconify icons from the
 * cache, downloaded on first use (reading [Iconify.version] redraws when they arrive).
 */
@Composable
internal fun rememberAssets(): DocAssets {
    val ctx = LocalContext.current
    return remember {
        val cache = HashMap<String, ImageBitmap?>()
        DocAssets(
            image = { name -> cache.getOrPut(name) { ImageAssets.load(ctx, name)?.asImageBitmap() } },
            icon = { id -> Iconify.version; Iconify.art(ctx, id) },
            hand = { l, part -> com.ohav.pixelforge.forge.HandArt.bitmap(l, part, 2f).asImageBitmap() },
        )
    }
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
    LayerKind.DATA -> Icons.Outlined.MonitorHeart
    LayerKind.GAUGE -> Icons.Outlined.Speed
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddPanel(state: EditorState, onAdded: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val (name, aspect) = withContext(Dispatchers.IO) { ImageAssets.import(ctx, uri) }
            val w = if (aspect >= 1) 220f else 220f * aspect
            state.add(Layer.create(LayerKind.IMAGE, 0, 0).copy(x = state.doc.w / 2, y = state.doc.h / 2, text = name, size = w, height = w / aspect, thickness = 24f, color = 0xFFFFFFFF))
            onAdded()
        }
    }
    val assets = rememberAssets()
    val isFace = state.studio == Studio.FACE
    if (!isFace) {
        Text("גודל", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WidgetPresets.sizes.forEach { sz ->
                FilterChip(state.doc.w == sz.w && state.doc.h == sz.h, { state.edit { it.resized(sz.w, sz.h).copy(corner = sz.corner) } }, { Text(sz.title) })
            }
        }
    }
    if (state.saved.isNotEmpty()) {
        Text(if (isFace) "הפנים שלי · לחיצה ארוכה למחיקה" else "השמורים שלי · לחיצה ארוכה למחיקה", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            state.saved.forEachIndexed { i, f ->
                Column(
                    Modifier.enter(i).clip(RoundedCornerShape(18.dp))
                        .combinedClickable(onLongClick = { state.deleteSaved(f) }) { state.open(f) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FaceThumb(f.doc, assets)
                    Text(SavedTime.format(Instant.ofEpochMilli(f.time).atZone(ZoneId.systemDefault())), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
    val groups = if (isFace) Presets.groups else WidgetPresets.groups
    var group by rememberSaveable { mutableIntStateOf(0) }
    Text("תבניות · ${groups.sumOf { it.second.size }}", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        groups.forEachIndexed { i, (name, _) -> FilterChip(group == i, { group = i }, { Text(name) }) }
    }
    Spacer(Modifier.height(8.dp))
    AnimatedContent(group.coerceIn(groups.indices), transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "presets") { g ->
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            groups[g].second.forEachIndexed { i, (name, doc) ->
                Column(Modifier.enter(i).clip(RoundedCornerShape(18.dp)).clickable { state.replaceWith(doc) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    FaceThumb(doc, assets)
                    Text(name, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
    Text("שכבה חדשה", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    // Widgets bake everything but time/date into a picture, so moving hands make no sense there.
    val kinds = LayerKind.entries.filter { isFace || (it != LayerKind.HANDS && it != LayerKind.DATA && it != LayerKind.GAUGE) }
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
            repeat(3 - items.size) { Spacer(Modifier.weight(1f)) }
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
            IconButton({ state.duplicate(l.id) }) { Icon(Icons.Outlined.ContentCopy, "שכפל") }
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
        if (l.kind == LayerKind.DATA || l.kind == LayerKind.GAUGE) {
            Text("מקור נתון", Modifier.enter(1), fontWeight = FontWeight.SemiBold)
            Row(Modifier.enter(1).horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DataSource.entries.forEach { d -> FilterChip(l.text == d.name, { state.editLayer(l.id) { it.copy(text = d.name) } }, { Text(d.title) }) }
            }
        }
        if (l.kind == LayerKind.HANDS) {
            Text("סגנון מחוגים", Modifier.enter(1), fontWeight = FontWeight.SemiBold)
            Row(Modifier.enter(1).horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("") + HandStyles).forEach { h -> FilterChip(l.text == h, { state.editLayer(l.id) { it.copy(text = h) } }, { Text(h.ifEmpty { "פשוט" }) }) }
            }
        }
        if (l.kind == LayerKind.ICON) IconPicker(l.text, Color(l.color or 0xFF000000)) { n -> state.editLayer(l.id) { it.copy(text = n) } }
        if (l.kind != LayerKind.IMAGE) Swatches("צבע", Palette, l.color or 0xFF000000, 2) { c -> state.editLayer(l.id) { it.copy(color = withAlpha(c, alpha)) } }
        LabeledSlider("אטימות", alpha * 100, 5f..100f, state, 2) { v -> state.updateLayer(l.id) { it.copy(color = withAlpha(it.color, v / 100)) } }
        if (l.kind != LayerKind.TICKS && !l.kind.isText && !l.kind.isImage) {
            val secondLabel = if (l.kind == LayerKind.HANDS) "מחוג שניות" else "גרדיאנט (צבע שני)"
            Swatches(secondLabel, listOf(0L) + Palette, l.color2, 2) { c -> state.editLayer(l.id) { it.copy(color2 = c) } }
        }
        // Ranges are tuned for the 450 face; bigger canvases (wide widgets) stretch them.
        val k = maxOf(state.doc.w, state.doc.h) / FACE_SIZE
        val (sizeLabel, baseRange) = when (l.kind) {
            LayerKind.TIME -> "גודל" to 30f..160f
            LayerKind.DATE, LayerKind.TEXT -> "גודל" to 14f..80f
            LayerKind.RING, LayerKind.ARC, LayerKind.TICKS -> "רדיוס" to 20f..225f
            LayerKind.HANDS -> "אורך" to 40f..225f
            LayerKind.CIRCLE -> "רדיוס" to 4f..225f
            LayerKind.RECT -> "רוחב" to 10f..450f
            LayerKind.ICON -> "גודל" to 12f..300f
            LayerKind.IMAGE -> "גודל" to 30f..450f
            LayerKind.DATA -> "גודל" to 12f..80f
            LayerKind.GAUGE -> (if (l.extra) "אורך" else "רדיוס") to 10f..225f
        }
        val range = baseRange.start..baseRange.endInclusive * k
        LabeledSlider(sizeLabel, l.size, range, state, 3) { v ->
            // Images keep their aspect ratio.
            state.updateLayer(l.id) { if (it.kind == LayerKind.IMAGE) it.copy(size = v, height = v * it.height / it.size) else it.copy(size = v) }
        }
        if (l.kind == LayerKind.IMAGE) LabeledSlider("פינות", l.thickness, 0f..(minOf(l.size, l.height) / 2), state, 4) { v -> state.updateLayer(l.id) { it.copy(thickness = v) } }
        if (l.kind == LayerKind.RECT) LabeledSlider("גובה", l.height, 6f..450f * k, state, 4) { v -> state.updateLayer(l.id) { it.copy(height = v) } }
        if (l.kind in listOf(LayerKind.RING, LayerKind.ARC, LayerKind.TICKS, LayerKind.HANDS, LayerKind.GAUGE)) {
            LabeledSlider("עובי", l.thickness, 1f..60f, state, 4) { v -> state.updateLayer(l.id) { it.copy(thickness = v) } }
        }
        if (l.kind == LayerKind.ARC || l.kind == LayerKind.GAUGE) {
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
                    FilterChip(l.weight == i, { state.editLayer(l.id) { it.copy(weight = i) } }, { Text(t, fontWeight = FontWeights[i]) })
                }
            }
        }
        if (l.kind == LayerKind.TEXT || l.kind == LayerKind.RECT || l.kind.isImage) {
            LabeledSlider("סיבוב", l.rotation, -180f..180f, state, 5) { v -> state.updateLayer(l.id) { it.copy(rotation = v) } }
        }
        if (l.kind == LayerKind.TIME || l.kind == LayerKind.HANDS || l.kind == LayerKind.GAUGE) {
            Row(Modifier.enter(5), verticalAlignment = Alignment.CenterVertically) {
                Text(when (l.kind) { LayerKind.TIME -> "הצג שניות"; LayerKind.GAUGE -> "מחט (במקום קשת)"; else -> "מחוג שניות" }, Modifier.weight(1f))
                Switch(l.extra, { b -> state.editLayer(l.id) { it.copy(extra = b) } })
            }
        }
        Row(Modifier.enter(6).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip({ state.editLayer(l.id) { it.copy(x = state.doc.w / 2) } }, { Text("מרכז אופקי") })
            AssistChip({ state.editLayer(l.id) { it.copy(y = state.doc.h / 2) } }, { Text("מרכז אנכי") })
        }
    }
    Spacer(Modifier.height(18.dp))
    HorizontalDivider()
    Spacer(Modifier.height(14.dp))
    Swatches("רקע", Backgrounds + Palette, state.doc.background, 7) { c -> state.edit { it.copy(background = c) } }
    Swatches("רקע — צבע שני (גרדיאנט)", listOf(0L) + Backgrounds + Palette, state.doc.background2, 7) { c -> state.edit { it.copy(background2 = c) } }
}

/** Built-in Material icons (offline) or the whole Iconify catalogue. */
@Composable
private fun IconPicker(selected: String, tint: Color, onPick: (String) -> Unit) {
    var online by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.enter(1).padding(bottom = 10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(online, { online = true }, { Text("ספריית Iconify") }, leadingIcon = { Icon(Icons.Outlined.Public, null, Modifier.size(18.dp)) })
            FilterChip(!online, { online = false }, { Text("בסיסי") })
        }
        Spacer(Modifier.height(6.dp))
        if (online) IconifyBrowser(selected, tint, onPick) else BuiltinIcons(selected, onPick)
    }
}

@Composable
private fun BuiltinIcons(selected: String, onPick: (String) -> Unit) {
    var cat by rememberSaveable { mutableIntStateOf(0) }
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

/**
 * Browses a curated Iconify set, or searches one set / all of them. Icon bodies are fetched a
 * page (60) at a time as the grid scrolls, then cached on the device.
 */
@Composable
private fun IconifyBrowser(selected: String, tint: Color, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // Tab 0 = search across every set; the rest are Iconify.packs.
    var tab by rememberSaveable { mutableIntStateOf(1) }
    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    var ids by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(tab, submitted) {
        error = null
        val prefix = Iconify.packs.getOrNull(tab - 1)?.prefix
        if (prefix == null && submitted.isBlank()) { ids = emptyList(); return@LaunchedEffect }
        loading = true
        ids = try {
            if (submitted.isBlank()) Iconify.collection(prefix!!).map { "$prefix:$it" } else Iconify.search(submitted, prefix)
        } catch (e: Exception) {
            error = "אין חיבור ל-Iconify — אייקונים שכבר נטענו ממשיכים לעבוד"; emptyList()
        }
        loading = false
    }
    OutlinedTextField(
        query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
        placeholder = { Text("חיפוש באנגלית: cat, heart, coffee…") },
        trailingIcon = { IconButton({ submitted = query.trim() }) { Icon(Icons.Outlined.Search, "חפש") } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submitted = query.trim() }),
    )
    Spacer(Modifier.height(6.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(tab == 0, { tab = 0 }, { Text("כל הספריות") })
        Iconify.packs.forEachIndexed { i, p -> FilterChip(tab == i + 1, { tab = i + 1 }, { Text(p.title) }) }
    }
    Spacer(Modifier.height(6.dp))
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    val hint = error ?: when {
        tab == 0 && submitted.isBlank() -> "הקלד מילה לחיפוש בכל 200+ הספריות"
        !loading && ids.isEmpty() -> "לא נמצאו אייקונים"
        else -> null
    }
    hint?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp)) }
    if (ids.isEmpty()) return
    Text("${ids.size} אייקונים", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val requested = remember(ids) { HashSet<Int>() }
    LazyVerticalGrid(GridCells.Fixed(6), Modifier.fillMaxWidth().height(330.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(ids.size) { i ->
            val id = ids[i]
            val page = i / 60
            LaunchedEffect(ids, page) {
                if (requested.add(page)) scope.launch {
                    val ok = runCatching {
                        ids.subList(page * 60, minOf(ids.size, page * 60 + 60)).groupBy { it.substringBefore(':') }
                            .forEach { (p, list) -> Iconify.fetch(ctx, p, list.map { it.substringAfter(':') }) }
                    }.isSuccess
                    if (!ok) requested.remove(page)
                }
            }
            val bgc by animateColorAsState(if (id == selected) Violet.copy(alpha = .35f) else MaterialTheme.colorScheme.surfaceVariant, label = "ic")
            Box(
                Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(bgc).clickable { onPick(id) },
                contentAlignment = Alignment.Center,
            ) { IconifyThumb(id, tint, Modifier.fillMaxSize(.74f)) }
        }
    }
}

@Composable
private fun IconifyThumb(id: String, tint: Color, modifier: Modifier) {
    val ctx = LocalContext.current
    val v = Iconify.version
    val art by produceState<Iconify.Art?>(null, id, v) { value = withContext(Dispatchers.Default) { Iconify.peek(ctx, id, 96) } }
    art?.let { a -> Image(a.image, id, modifier, colorFilter = if (a.mono) ColorFilter.tint(tint) else null) }
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
