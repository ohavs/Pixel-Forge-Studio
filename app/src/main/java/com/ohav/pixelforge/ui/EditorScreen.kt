package com.ohav.pixelforge.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohav.pixelforge.editor.*
import com.ohav.pixelforge.watch.FaceBuilder
import com.ohav.pixelforge.watch.WatchLink
import com.ohav.pixelforge.widget.StudioWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val Palette = listOf(
    0xFFFFFFFF, 0xFFD1D1D6, 0xFF8E8E93, 0xFF000000, 0xFFFF453A, 0xFFFF9F0A, 0xFFFFD60A, 0xFFE5C07B,
    0xFF32D74B, 0xFF66D4CF, 0xFF64D2FF, 0xFF0A84FF, 0xFF5E5CE6, 0xFFBF5AF2, 0xFFFF375F, 0xFFC69C6D,
)
private val Backgrounds = listOf(0xFF000000, 0xFF0E1015, 0xFF15132B, 0xFF1A1033, 0xFF06231F, 0xFF2B0F16, 0xFF101C33, 0xFF2A2A2E)

private enum class Tab(val title: String, val icon: ImageVector) {
    ADD("ספריות", Icons.Outlined.AutoAwesomeMosaic), LAYERS("שכבות", Icons.Outlined.Layers),
    STYLE("עיצוב", Icons.Outlined.Tune), CANVAS("קנבס", Icons.Outlined.Wallpaper),
}

/**
 * The editor both studios share. The design sits alone in the middle; four tabs in a floating
 * dock open one panel beneath it (tap the open tab again to fold it and see the design bigger).
 */
@Composable
fun EditorScreen(studio: Studio, onBack: () -> Unit) = StudioDark {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { EditorState(ctx, studio) }
    val isFace = studio == Studio.FACE
    var tab by rememberSaveable { mutableStateOf(Tab.ADD) }
    var open by rememberSaveable { mutableStateOf(true) }
    var watch by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    if (isFace) LaunchedEffect(Unit) {
        while (true) {
            watch = try { WatchLink.watchName(ctx) } catch (e: Exception) { null }
            delay(5000)
        }
    }
    // Status lines fade on their own after a while (errors stay a little longer).
    LaunchedEffect(status) { if (status.isNotEmpty() && !busy) { delay(if (status.startsWith("שגיאה")) 8000 else 3500); status = "" } }

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

    Box(Modifier.fillMaxSize().background(Ui.Ink)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIcon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה", onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(studio.title, color = Ui.Text1, fontSize = Ui.Title, fontWeight = FontWeight.SemiBold)
                    if (isFace) Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (watch != null) Ui.Good else Ui.Text2))
                        Spacer(Modifier.width(5.dp))
                        Text(watch ?: "שעון לא מחובר", color = Ui.Text2, fontSize = Ui.Caption, maxLines = 1)
                    } else Text("מסך הבית", color = Ui.Text2, fontSize = Ui.Caption)
                }
                Row(Modifier.height(40.dp).glass(CircleShape), verticalAlignment = Alignment.CenterVertically) {
                    UndoButton(Icons.AutoMirrored.Filled.Undo, "בטל", state.canUndo, state::undo)
                    Box(Modifier.size(1.dp, 18.dp).background(Ui.Hairline))
                    UndoButton(Icons.AutoMirrored.Filled.Redo, "בצע שוב", state.canRedo, state::redo)
                }
                Spacer(Modifier.width(8.dp))
                if (isFace) PrimaryPill("שלח", Icons.AutoMirrored.Filled.Send, enabled = !busy && watch != null) { send() }
                else PrimaryPill("הוסף", Icons.Outlined.AddToHomeScreen) { scope.launch { status = StudioWidgets.pin(ctx, state.doc) } }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = Color.White, trackColor = Ui.Raised)

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Stage(state, Modifier.fillMaxSize())
                androidx.compose.animation.AnimatedVisibility(
                    status.isNotEmpty(), Modifier.align(Alignment.TopCenter).padding(top = 6.dp, start = 24.dp, end = 24.dp),
                    enter = fadeIn() + scaleIn(initialScale = .9f), exit = fadeOut(),
                ) {
                    Text(
                        status, Modifier.glass(RoundedCornerShape(16.dp), strength = 1.6f).background(Color.Black.copy(alpha = .4f)).padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Ui.Text1, fontSize = 13.sp, maxLines = 3,
                    )
                }
                SelectionBar(state, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)) { tab = Tab.STYLE; open = true }
            }

            AnimatedVisibility(open, enter = expandVertically(spring(dampingRatio = .85f, stiffness = 500f)) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(
                    Modifier.fillMaxWidth().fillMaxHeight(.47f).clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A1A1D), Ui.Sheet))),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Grabber(Modifier.clickable { open = false })
                    AnimatedContent(tab, Modifier.fillMaxSize(), transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(100)) }, label = "tab") { t ->
                        when (t) {
                            Tab.ADD -> LibraryPanel(state) { status = it }
                            Tab.LAYERS -> LayersPanel(state)
                            Tab.STYLE -> Scroll { StylePanel(state) }
                            Tab.CANVAS -> Scroll { CanvasPanel(state) { status = it } }
                        }
                    }
                }
            }
            Dock(tab, open) { t -> if (t == tab) open = !open else { tab = t; open = true } }
        }
    }
}

@Composable
private fun UndoButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(44.dp, 40.dp).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, Modifier.size(20.dp), tint = if (enabled) Ui.Text1 else Ui.Text1.copy(alpha = .25f))
    }
}

@Composable
private fun Scroll(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), content = content)
}

/** The floating tab dock: icons with labels; the active one lifts onto a lit plate. */
@Composable
private fun Dock(selected: Tab, open: Boolean, onSelect: (Tab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Ui.Sheet).padding(horizontal = 16.dp, vertical = 8.dp).height(58.dp)
            .glass(RoundedCornerShape(22.dp)).padding(5.dp),
    ) {
        Tab.entries.forEach { t ->
            val on = t == selected && open
            val bg by animateColorAsState(if (on) Color.White.copy(alpha = .14f) else Color.Transparent, label = "dock")
            val fg by animateColorAsState(if (on) Ui.Text1 else Ui.Text2, label = "dockfg")
            val src = remember { MutableInteractionSource() }
            Column(
                Modifier.weight(1f).fillMaxHeight().pressScale(src).clip(RoundedCornerShape(17.dp)).background(bg).clickable(src, null) { onSelect(t) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Icon(t.icon, null, Modifier.size(21.dp), tint = fg)
                Text(t.title, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** The design on a soft halo, as large as the stage allows. */
@Composable
private fun Stage(state: EditorState, modifier: Modifier) {
    BoxWithConstraints(modifier.padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        val aspect = state.doc.w / state.doc.h
        val w: Dp = minOf(maxWidth, maxHeight * aspect)
        val accent = Color(state.doc.background2.takeIf { it != 0L } ?: state.doc.layers.lastOrNull()?.color ?: 0xFF0A84FF)
        val halo by animateColorAsState(accent.copy(alpha = .22f), tween(600), label = "halo")
        Canvas(Modifier.size(w * 1.25f, w / aspect * 1.25f)) {
            drawCircle(Brush.radialGradient(listOf(halo, Color.Transparent), center, size.maxDimension / 2), size.maxDimension / 2)
        }
        FaceCanvas(state, Modifier.size(w, w / aspect))
    }
}

/** A floating capsule of quick actions for the selected layer. */
@Composable
private fun SelectionBar(state: EditorState, modifier: Modifier, onStyle: () -> Unit) {
    val l = state.selected
    AnimatedVisibility(l != null, modifier, enter = fadeIn() + scaleIn(initialScale = .85f), exit = fadeOut() + scaleOut(targetScale = .85f)) {
        val layer = l ?: return@AnimatedVisibility
        Row(
            Modifier.height(44.dp).glass(CircleShape, strength = 1.4f).background(Color.Black.copy(alpha = .35f)).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                layer.name, Modifier.widthIn(max = 120.dp).clickable(onClick = onStyle).padding(horizontal = 10.dp),
                color = Ui.Text1, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            BarAction(Icons.Outlined.ContentCopy, "שכפל") { state.duplicate(layer.id) }
            BarAction(if (layer.locked) Icons.Filled.Lock else Icons.Outlined.LockOpen, "נעל") { state.editLayer(layer.id) { it.copy(locked = !it.locked) } }
            BarAction(Icons.Outlined.FlipToFront, "קדימה") { state.move(layer.id, 1) }
            BarAction(Icons.Outlined.Delete, "מחק", Ui.Danger) { state.delete(layer.id) }
        }
    }
}

@Composable
private fun BarAction(icon: ImageVector, label: String, tint: Color = Ui.Text1, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, Modifier.size(19.dp), tint = tint)
    }
}

@Composable
private fun FaceCanvas(state: EditorState, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000L - now.nano / 1_000_000) } }
    val pulse by rememberInfiniteTransition(label = "sel").animateFloat(.5f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
    val bg by animateColorAsState(Color(state.doc.background), tween(350), label = "bg")
    val assets = rememberAssets()
    var guideX by remember { mutableStateOf(false) }
    var guideY by remember { mutableStateOf(false) }

    Canvas(
        modifier
            .pointerInput(Unit) {
                // One gesture handler: tap selects (empty spot clears), one finger moves, two fingers pinch-resize.
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
                    if (moving) state.commit() else state.selectedId = hit?.id
                    guideX = false; guideY = false
                }
            },
    ) {
        val doc = state.doc
        val u = size.width / doc.w
        val rim = 3.dp.toPx()
        val rimBrush = Brush.linearGradient(listOf(Color(0xFF55555C), Color(0xFF1C1C1F), Color(0xFF3A3A40)), Offset.Zero, Offset(size.width, size.height))
        if (doc.round) drawCircle(rimBrush, radius = size.minDimension / 2 + rim)
        else drawRoundRect(rimBrush, Offset(-rim, -rim), Size(size.width + 2 * rim, size.height + 2 * rim), CornerRadius(doc.corner * u + rim))
        drawDoc(doc, bg, measurer, now, assets)
        val guide = Color(0xFFFF375F)
        if (guideX) drawLine(guide, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1.dp.toPx())
        if (guideY) drawLine(guide, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
        state.selected?.takeIf { it.visible }?.let { l ->
            val dash = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            val sel = Ui.Blue.copy(alpha = pulse)
            if (l.kind in listOf(LayerKind.RING, LayerKind.ARC, LayerKind.TICKS, LayerKind.HANDS, LayerKind.CIRCLE, LayerKind.GAUGE)) {
                drawCircle(sel, (l.size + l.thickness / 2 + 6) * u, Offset(l.x * u, l.y * u), style = dash)
            } else {
                val (w, h) = l.box()
                drawRoundRect(sel, Offset((l.x - w / 2 - 4) * u, (l.y - h / 2 - 4) * u), Size((w + 8) * u, (h + 8) * u), CornerRadius(8.dp.toPx()), style = dash)
            }
        }
    }
}

/** Static thumbnail of a design (saved designs): [height] tall, as wide as its aspect needs. */
@Composable
internal fun FaceThumb(doc: FaceDoc, assets: DocAssets, height: Dp = 92.dp) {
    val measurer = rememberTextMeasurer()
    val aspect = doc.w / doc.h
    val mod = if (aspect > 1.6f) Modifier.width(height * 1.63f).aspectRatio(aspect) else Modifier.height(height).aspectRatio(aspect)
    Canvas(mod) { drawDoc(doc, Color(doc.background), measurer, ThumbTime, assets) }
}

private val SavedTime = DateTimeFormatter.ofPattern("d/M HH:mm")

internal val ThumbTime: LocalDateTime = LocalDateTime.of(2026, 10, 28, 10, 8, 32)

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

@Composable
private fun LayersPanel(state: EditorState) {
    val layers = state.doc.layers
    if (layers.isEmpty()) {
        Text("העיצוב ריק — פתח את הספריות והוסף משהו", Modifier.padding(24.dp), color = Ui.Text2)
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val list = layers.asReversed()
        itemsIndexed(list, key = { _, l -> l.id }) { i, l ->
            val selected = l.id == state.selectedId
            val bgc by animateColorAsState(if (selected) Ui.Blue.copy(alpha = .22f) else Ui.Raised, label = "row")
            Row(
                Modifier.animateItem().fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(bgc)
                    .clickable { state.selectedId = l.id }.padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Ui.Raised2), contentAlignment = Alignment.Center) {
                    Icon(l.kind.icon(), null, Modifier.size(18.dp), tint = Color(l.color or 0xFF000000).takeIf { l.color and 0xFFFFFF > 0x202020 } ?: Ui.Text2)
                }
                Text(
                    l.name, Modifier.weight(1f).padding(horizontal = 12.dp), color = if (l.visible) Ui.Text1 else Ui.Text2,
                    fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                RowIcon(if (l.visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, "הסתר") { state.editLayer(l.id) { it.copy(visible = !it.visible) } }
                RowIcon(if (l.locked) Icons.Filled.Lock else Icons.Outlined.LockOpen, "נעל") { state.editLayer(l.id) { it.copy(locked = !it.locked) } }
                RowIcon(Icons.Filled.KeyboardArrowUp, "למעלה", i > 0) { state.move(l.id, 1) }
                RowIcon(Icons.Filled.KeyboardArrowDown, "למטה", i < list.lastIndex) { state.move(l.id, -1) }
            }
        }
    }
}

@Composable
private fun RowIcon(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, Modifier.size(19.dp), tint = if (enabled) Ui.Text2 else Ui.Text2.copy(alpha = .25f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StylePanel(state: EditorState) {
    val l = state.selected
    if (l == null) {
        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.TouchApp, null, Modifier.size(32.dp), tint = Ui.Text2)
            Spacer(Modifier.height(8.dp))
            Text("גע בשכבה בעיצוב כדי לעצב אותה", color = Ui.Text2)
        }
        return
    }
    val alpha = (l.color ushr 24 and 0xFF) / 255f
    val k = maxOf(state.doc.w, state.doc.h) / FACE_SIZE

    if (l.kind == LayerKind.TEXT) Section("טקסט") {
        BasicTextField(
            l.text, { v -> state.updateLayer(l.id) { it.copy(text = v.take(24)) } },
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(Ui.Raised).padding(horizontal = 14.dp, vertical = 12.dp)
                .onFocusChanged { if (!it.isFocused) state.commit() },
            singleLine = true, textStyle = TextStyle(color = Ui.Text1, fontSize = 15.sp), cursorBrush = SolidColor(Ui.Blue),
        )
    }
    if (l.kind == LayerKind.DATA || l.kind == LayerKind.GAUGE) Section("מקור נתון") {
        Segmented(DataSource.entries.map { it.title }, DataSource.entries.indexOf(DataSource.of(l.text))) { i -> state.editLayer(l.id) { it.copy(text = DataSource.entries[i].name) } }
    }
    if (l.kind == LayerKind.HANDS) Section("סגנון מחוגים") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4) {
            (listOf("") + HandStyles).forEach { s ->
                val on = l.text == s
                Box(
                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (on) Ui.Blue.copy(alpha = .3f) else Color.Transparent).clickable { state.editLayer(l.id) { it.copy(text = s) } }.padding(3.dp),
                ) { ElementThumb(l.copy(text = s, x = FACE_SIZE / 2, y = FACE_SIZE / 2), Modifier.fillMaxSize()) }
            }
        }
    }
    if (l.kind != LayerKind.IMAGE) Section("צבע") {
        ColorDots(Palette, l.color or 0xFF000000) { c -> state.editLayer(l.id) { it.copy(color = withAlpha(c, alpha)) } }
    }
    Section(if (l.kind == LayerKind.IMAGE) "תמונה" else null) {
        BarSlider("אטימות", alpha * 100, 5f..100f, format = { "${it.toInt()}%" }, onDone = state::commit) { v -> state.updateLayer(l.id) { it.copy(color = withAlpha(it.color, v / 100)) } }
        val (sizeLabel, base) = when (l.kind) {
            LayerKind.TIME -> "גודל" to 30f..160f
            LayerKind.DATE, LayerKind.TEXT, LayerKind.DATA -> "גודל" to 12f..80f
            LayerKind.RING, LayerKind.ARC, LayerKind.TICKS -> "רדיוס" to 20f..225f
            LayerKind.HANDS -> "אורך" to 40f..225f
            LayerKind.CIRCLE -> "רדיוס" to 4f..225f
            LayerKind.RECT -> "רוחב" to 10f..450f
            LayerKind.ICON -> "גודל" to 12f..300f
            LayerKind.IMAGE -> "גודל" to 20f..450f
            LayerKind.GAUGE -> (if (l.extra) "אורך" else "רדיוס") to 10f..225f
        }
        // Images keep their aspect ratio.
        LayerSlider(state, l.id, sizeLabel, l.size, base.start..base.endInclusive * k) { it, v -> if (it.kind == LayerKind.IMAGE) it.copy(size = v, height = v * it.height / it.size) else it.copy(size = v) }
        if (l.kind == LayerKind.RECT) LayerSlider(state, l.id, "גובה", l.height, 6f..450f * k) { it, v -> it.copy(height = v) }
        if (l.kind == LayerKind.RECT || l.kind == LayerKind.IMAGE) LayerSlider(state, l.id, "פינות", l.thickness, 0f..(minOf(l.size, l.height) / 2).coerceAtLeast(1f)) { it, v -> it.copy(thickness = v) }
        if (l.kind in listOf(LayerKind.RING, LayerKind.ARC, LayerKind.TICKS, LayerKind.HANDS, LayerKind.GAUGE)) LayerSlider(state, l.id, "עובי", l.thickness, 1f..60f) { it, v -> it.copy(thickness = v) }
        if (l.kind == LayerKind.ARC || l.kind == LayerKind.GAUGE) {
            LayerSlider(state, l.id, "התחלה", l.angle, 0f..360f) { it, v -> it.copy(angle = v) }
            LayerSlider(state, l.id, "זווית", l.height, 5f..360f) { it, v -> it.copy(height = v) }
        }
        if (l.kind == LayerKind.TICKS) {
            LayerSlider(state, l.id, "אורך סימון", l.height, 2f..80f) { it, v -> it.copy(height = v) }
            LayerSlider(state, l.id, "כמות", l.count.toFloat(), 4f..120f) { it, v -> it.copy(count = v.toInt()) }
        }
        if (l.kind == LayerKind.TEXT || l.kind == LayerKind.RECT || l.kind.isImage) LayerSlider(state, l.id, "סיבוב", l.rotation, -180f..180f) { it, v -> it.copy(rotation = v) }
    }
    if (l.kind.isText) Section("משקל") {
        Segmented(listOf("דק", "קל", "רגיל", "בינוני", "עבה"), l.weight) { i -> state.editLayer(l.id) { it.copy(weight = i) } }
    }
    if (l.kind == LayerKind.TIME || l.kind == LayerKind.HANDS || l.kind == LayerKind.GAUGE) Section(null) {
        val opts = when (l.kind) { LayerKind.TIME -> listOf("בלי שניות", "עם שניות"); LayerKind.GAUGE -> listOf("קשת", "מחט"); else -> listOf("בלי מחוג שניות", "מחוג שניות") }
        Segmented(opts, if (l.extra) 1 else 0) { i -> state.editLayer(l.id) { it.copy(extra = i == 1) } }
    }
    if (l.kind != LayerKind.TICKS && !l.kind.isText && !l.kind.isImage) Section(if (l.kind == LayerKind.HANDS) "מחוג שניות ומרכז" else "צבע שני (גרדיאנט)") {
        ColorDots(listOf(0L) + Palette, l.color2) { c -> state.editLayer(l.id) { it.copy(color2 = c) } }
    }
    Section("מיקום") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassPill("מרכז אופקי", Icons.Outlined.AlignHorizontalCenter, Modifier.weight(1f)) { state.editLayer(l.id) { it.copy(x = state.doc.w / 2) } }
            GlassPill("מרכז אנכי", Icons.Outlined.AlignVerticalCenter, Modifier.weight(1f)) { state.editLayer(l.id) { it.copy(y = state.doc.h / 2) } }
        }
    }
}

/** Canvas tab: size (widgets), background, saving and the saved designs. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CanvasPanel(state: EditorState, onStatus: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFace = state.studio == Studio.FACE
    if (!isFace) Section("גודל") {
        val sizes = WidgetPresets.sizes
        Segmented(sizes.map { it.title }, sizes.indexOfFirst { it.w == state.doc.w && it.h == state.doc.h }) { i ->
            val sz = sizes[i]; state.edit { it.resized(sz.w, sz.h).copy(corner = sz.corner) }
        }
        GlassPill("עדכן את כל הווידג'טים במסך", Icons.Outlined.Sync, Modifier.fillMaxWidth()) { scope.launch { onStatus(StudioWidgets.applyToAll(ctx, state.doc)) } }
    }
    Section("רקע") { ColorDots(Backgrounds + Palette, state.doc.background) { c -> state.edit { it.copy(background = c) } } }
    Section("גרדיאנט לרקע") { ColorDots(listOf(0L) + Backgrounds + Palette, state.doc.background2) { c -> state.edit { it.copy(background2 = c) } } }
    Section(if (isFace) "הפנים שלי" else "השמורים שלי") {
        PrimaryPill("שמור עותק", Icons.Outlined.BookmarkAdd, Modifier.fillMaxWidth()) { state.saveCopy(); onStatus("נשמר") }
        if (state.saved.isNotEmpty()) {
            val assets = rememberAssets()
            Text("לחיצה פותחת · לחיצה ארוכה מוחקת", color = Ui.Text2, fontSize = Ui.Caption)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.saved.forEach { f ->
                    Column(
                        Modifier.clip(RoundedCornerShape(18.dp)).background(Ui.Raised).combinedClickable(onLongClick = { state.deleteSaved(f) }) { state.open(f) }.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FaceThumb(f.doc, assets, 76.dp)
                        Text(SavedTime.format(Instant.ofEpochMilli(f.time).atZone(ZoneId.systemDefault())), Modifier.padding(top = 4.dp), color = Ui.Text2, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerSlider(state: EditorState, id: Long, label: String, value: Float, range: ClosedFloatingPointRange<Float>, f: (Layer, Float) -> Layer) {
    BarSlider(label, value, range, onDone = state::commit) { v -> state.updateLayer(id) { f(it, v) } }
}

private fun withAlpha(c: Long, a: Float): Long = ((a * 255).toLong().coerceIn(0, 255) shl 24) or (c and 0xFFFFFF)
