package com.ohav.pixelforge.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohav.pixelforge.editor.*
import com.ohav.pixelforge.editor.Library.Kind
import com.ohav.pixelforge.editor.Library.Thumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Where the library browser is: home (all libraries) → one library → one category. */
private data class LibNav(val lib: Int = -1, val cat: Int = -1, val query: String = "")

/**
 * The Add tab: every asset library as a card, each opening to its categories and then a grid.
 * Tapping a tile adds it to the design (icons and pictures as layers, textures and generated
 * art as a locked background).
 */
@Composable
fun LibraryPanel(state: EditorState, onStatus: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var nav by remember { mutableStateOf(LibNav()) }
    val libs = Library.all
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val (name, aspect) = withContext(Dispatchers.IO) { ImageAssets.import(ctx, uri) }
            state.addPicture(name, aspect, 220f, 24f)
        }
    }

    fun add(lib: Library.Lib, item: Library.Item) = scope.launch {
        onStatus("מוסיף ${item.title}…")
        try {
            addItem(ctx, state, lib, item)
            onStatus("")
        } catch (e: Exception) {
            onStatus("לא הצלחתי להוריד את ${item.title} — בדוק חיבור לאינטרנט")
        }
    }

    AnimatedContent(
        nav, Modifier.fillMaxSize(), label = "lib",
        transitionSpec = {
            val forward = targetState.lib >= 0 && initialState.lib < 0 || targetState.cat >= 0 && initialState.cat < 0
            (slideInHorizontally(tween(260)) { if (forward) -it / 4 else it / 4 } + fadeIn(tween(200))) togetherWith fadeOut(tween(120))
        },
    ) { n ->
        when {
            n.lib < 0 -> LibraryHome(libs, state) { i ->
                if (libs[i].kind == Kind.PHOTOS) pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                else nav = LibNav(i)
            }
            else -> LibraryPage(state, libs[n.lib], n, { nav = it }) { item -> add(libs[n.lib], item) }
        }
    }
}

private suspend fun addItem(ctx: android.content.Context, state: EditorState, lib: Library.Lib, item: Library.Item) {
    val doc = state.doc
    when (val t = item.thumb) {
        is Thumb.Element -> state.addCentered(t.layer)
        is Thumb.Icon -> {
            Iconify.fetch(ctx, t.id.substringBefore(':'), listOf(t.id.substringAfter(':')))
            state.addCentered(Layer.create(LayerKind.ICON, 0, 0xFFFFFFFF).copy(text = t.id, size = 110f, x = FACE_SIZE / 2, y = FACE_SIZE / 2, color = 0xFFFFFFFF))
        }
        is Thumb.Gen -> Unit // generators add through GeneratorsPage
        is Thumb.Url -> if (lib.kind == Kind.TEXTURES) {
            val bmp = PolyHaven.picture(item.full)
            val name = withContext(Dispatchers.IO) { LibraryAdd.store(ctx, bmp, item.title, 1000) }
            state.addBackground(name)
        } else {
            val (name, aspect, w) = withContext(Dispatchers.IO) {
                val bytes = RemoteImages.bytes(ctx, t.url)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("bad image")
                Triple(LibraryAdd.store(ctx, bmp, item.title), bmp.width.toFloat() / bmp.height, bmp.width)
            }
            // Kenney parts keep their pixel size (they are UI pieces); icons start at a third of the canvas.
            val side = if (lib.kind == Kind.KENNEY) (w / 2f).coerceIn(60f, doc.w * .8f) else minOf(doc.w, doc.h) * .34f
            state.addPicture(name, aspect, side, 0f)
        }
    }
}

@Composable
private fun LibraryHome(libs: List<Library.Lib>, state: EditorState, onOpen: (Int) -> Unit) {
    LazyVerticalGrid(
        GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(libs) { i, lib -> LibraryCard(lib, state, Modifier.enter(i)) { onOpen(i) } }
    }
}

@Composable
private fun LibraryCard(lib: Library.Lib, state: EditorState, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Column(
        modifier.pressScale(src).height(136.dp).glass(RoundedCornerShape(22.dp)).clickable(src, null, onClick = onClick).padding(12.dp),
    ) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            when (lib.kind) {
                Kind.GENERATORS -> listOf(Generator.WAVES, Generator.BLOBS, Generator.TOPO).forEach { g ->
                    GenThumb(g, Generator.palettes[4], 7, 1f, Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)))
                }
                Kind.ELEMENTS -> Elements.categories(true).take(3).map { it.items.first() }.forEach { item ->
                    (item.thumb as? Thumb.Element)?.let { ElementThumb(it.layer, Modifier.size(38.dp)) }
                }
                else -> if (lib.cover.isEmpty()) Icon(lib.kind.icon(), null, Modifier.size(34.dp), tint = Ui.Text2)
                else lib.cover.forEach { ThumbView(it, Modifier.size(34.dp)) }
            }
        }
        Text(lib.title, color = Ui.Text1, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(lib.subtitle, color = Ui.Text2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun Kind.icon(): ImageVector = when (this) {
    Kind.PHOTOS -> Icons.Outlined.PhotoLibrary
    Kind.KENNEY -> Icons.Outlined.Dashboard
    else -> Icons.Outlined.Category
}

@Composable
private fun LibraryPage(state: EditorState, lib: Library.Lib, nav: LibNav, go: (LibNav) -> Unit, onPick: (Library.Item) -> Unit) {
    val ctx = LocalContext.current
    var cats by remember(lib) { mutableStateOf<List<Library.Category>?>(null) }
    var error by remember(lib) { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(lib, attempt) {
        error = false
        cats = try { Library.categories(ctx, lib, state.studio) } catch (e: Exception) { error = true; null }
    }
    val cat = cats?.getOrNull(nav.cat)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה", size = 36.dp) {
                go(if (nav.cat >= 0 || nav.query.isNotEmpty()) LibNav(nav.lib) else LibNav())
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cat?.title ?: lib.title, color = Ui.Text1, fontSize = Ui.Title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(if (cat != null) "${cat.items.size} פריטים · ${lib.title}" else lib.credit, color = Ui.Text2, fontSize = Ui.Caption, maxLines = 1)
            }
        }
        if (lib.search && nav.cat < 0) SearchField(nav.query) { q -> go(nav.copy(query = q)) }
        when {
            lib.kind == Kind.GENERATORS -> GeneratorsPage(state)
            nav.query.isNotEmpty() -> SearchResults(lib, nav.query, onPick)
            error -> Message("הספרייה לא נטענה — בדוק חיבור לאינטרנט", "נסה שוב") { attempt++ }
            cats == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ui.Text2, strokeWidth = 2.dp) }
            cats!!.all { it.items.isEmpty() } -> Message("הספרייה הזאת עוד בהכנה")
            cat != null -> ItemGrid(lib, cat.items, onPick)
            cats!!.size == 1 -> ItemGrid(lib, cats!!.first().items, onPick)
            else -> CategoryGrid(cats!!) { go(nav.copy(cat = it)) }
        }
    }
}

@Composable
private fun Message(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = Ui.Text2, fontSize = 14.sp)
        action?.let { Spacer(Modifier.height(12.dp)); GlassPill(it, Icons.Outlined.Refresh, onClick = onAction) }
    }
}

@Composable
private fun SearchField(initial: String, onSubmit: (String) -> Unit) {
    var q by rememberSaveable(initial) { mutableStateOf(initial) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(40.dp).clip(RoundedCornerShape(12.dp)).background(Ui.Raised).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), tint = Ui.Text2)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (q.isEmpty()) Text("חיפוש באנגלית: heart, sun, cat", color = Ui.Text2, fontSize = 14.sp)
            BasicTextField(
                q, { q = it }, Modifier.fillMaxWidth(), singleLine = true,
                textStyle = TextStyle(color = Ui.Text1, fontSize = 14.sp), cursorBrush = SolidColor(Ui.Blue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSubmit(q.trim()) }),
            )
        }
        if (q.isNotEmpty()) Icon(Icons.Outlined.Cancel, "נקה", Modifier.size(18.dp).clickable { q = ""; onSubmit("") }, tint = Ui.Text2)
    }
}

@Composable
private fun SearchResults(lib: Library.Lib, query: String, onPick: (Library.Item) -> Unit) {
    var ids by remember(query) { mutableStateOf<List<String>?>(null) }
    var failed by remember(query) { mutableStateOf(false) }
    LaunchedEffect(query) {
        val prefix = when (lib.kind) { Kind.METEOCONS -> "meteocons"; Kind.TABLER -> "tabler"; else -> null }
        ids = try { Iconify.search(query, prefix) } catch (e: Exception) { failed = true; emptyList() }
    }
    when {
        failed -> Message("החיפוש צריך חיבור לאינטרנט")
        ids == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ui.Text2, strokeWidth = 2.dp) }
        ids!!.isEmpty() -> Message("לא נמצא כלום עבור \"$query\"")
        else -> ItemGrid(lib, ids!!.map { Library.Item(it.substringAfter(':'), Thumb.Icon(it)) }, onPick)
    }
}

@Composable
private fun CategoryGrid(cats: List<Library.Category>, onOpen: (Int) -> Unit) {
    val ctx = LocalContext.current
    // One batched download for every category's preview icons.
    LaunchedEffect(cats) {
        cats.flatMap { c -> c.items.take(4).mapNotNull { (it.thumb as? Thumb.Icon)?.id } }.groupBy { it.substringBefore(':') }
            .forEach { (p, l) -> runCatching { Iconify.fetch(ctx, p, l.map { it.substringAfter(':') }) } }
    }
    LazyVerticalGrid(
        GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(cats) { i, c ->
            val src = remember { MutableInteractionSource() }
            Column(Modifier.enter(i.coerceAtMost(8)).pressScale(src).clip(RoundedCornerShape(20.dp)).background(Ui.Raised).clickable(src, null) { onOpen(i) }.padding(12.dp)) {
                Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    c.items.take(4).forEach { ThumbView(it.thumb, Modifier.size(36.dp)) }
                }
                Spacer(Modifier.height(8.dp))
                Text(c.title, color = Ui.Text1, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${c.items.size}", color = Ui.Text2, fontSize = 12.sp)
            }
        }
    }
}

/** Items of one category. Iconify icons are downloaded a page (60) at a time as the grid scrolls. */
@Composable
private fun ItemGrid(lib: Library.Lib, items: List<Library.Item>, onPick: (Library.Item) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val requested = remember(items) { HashSet<Int>() }
    val cols = when (lib.kind) { Kind.ELEMENTS, Kind.TEXTURES, Kind.KENNEY -> 3; else -> 4 }
    LazyVerticalGrid(
        GridCells.Fixed(cols), Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items.size) { i ->
            val item = items[i]
            if (item.thumb is Thumb.Icon) {
                val page = i / 60
                LaunchedEffect(items, page) {
                    if (requested.add(page)) scope.launch {
                        val ok = runCatching {
                            items.subList(page * 60, minOf(items.size, page * 60 + 60)).mapNotNull { (it.thumb as? Thumb.Icon)?.id }
                                .groupBy { it.substringBefore(':') }.forEach { (p, l) -> Iconify.fetch(ctx, p, l.map { it.substringAfter(':') }) }
                        }.isSuccess
                        if (!ok) requested.remove(page)
                    }
                }
            }
            val src = remember { MutableInteractionSource() }
            Column(
                Modifier.pressScale(src).clip(RoundedCornerShape(16.dp)).background(Ui.Raised).clickable(src, null) { onPick(item) }.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ThumbView(item.thumb, Modifier.fillMaxWidth().aspectRatio(1f))
                if (lib.kind == Kind.ELEMENTS || lib.kind == Kind.TEXTURES) Text(
                    item.title, Modifier.padding(top = 4.dp), color = Ui.Text2, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Generated backgrounds: pick a palette, shuffle, tap one to set it as the background. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneratorsPage(state: EditorState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var pal by rememberSaveable { mutableIntStateOf(0) }
    var seed by rememberSaveable { mutableIntStateOf(1) }
    val aspect = state.doc.w / state.doc.h
    val shape = if (state.doc.round) CircleShape else RoundedCornerShape(18.dp)
    LazyVerticalGrid(
        GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Generator.palettes.forEachIndexed { i, p ->
                        Box(
                            Modifier.size(52.dp, 28.dp).clip(CircleShape)
                                .background(Brush.horizontalGradient(p.colors.map { Color(it) }))
                                .border(2.dp, if (i == pal) Color.White else Color.Transparent, CircleShape).clickable { pal = i },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlassPill("ערבב וריאציות", Icons.Outlined.Shuffle) { seed++ }
            }
        }
        items(Generator.entries) { g ->
            val src = remember { MutableInteractionSource() }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GenThumb(
                    g, Generator.palettes[pal], seed * 31 + g.ordinal, aspect,
                    Modifier.fillMaxWidth().aspectRatio(aspect).pressScale(src).clip(shape).clickable(src, null) {
                        scope.launch {
                            val d = state.doc
                            val name = withContext(Dispatchers.Default) {
                                val bmp = Generators.render(g, Generator.palettes[pal], seed * 31 + g.ordinal, (d.w * 2).roundToInt(), (d.h * 2).roundToInt())
                                LibraryAdd.store(ctx, bmp, g.title, 1000)
                            }
                            state.addBackground(name)
                        }
                    },
                )
                Text(g.title, Modifier.padding(top = 6.dp), color = Ui.Text2, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GenThumb(g: Generator, pal: GenPalette, seed: Int, aspect: Float, modifier: Modifier) {
    val img by produceState<ImageBitmap?>(null, g, pal, seed, aspect) {
        value = withContext(Dispatchers.Default) { Generators.render(g, pal, seed, 240, (240 / aspect).roundToInt().coerceAtLeast(1)).asImageBitmap() }
    }
    Box(modifier.background(Color(pal.bg))) { img?.let { Image(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop) } }
}

/** One tile picture, whatever its source. */
@Composable
fun ThumbView(t: Thumb, modifier: Modifier) {
    when (t) {
        is Thumb.Url -> UrlThumb(t.url, modifier)
        is Thumb.Icon -> IconThumb(t.id, modifier)
        is Thumb.Element -> ElementThumb(t.layer, modifier)
        is Thumb.Gen -> GenThumb(t.g, Generator.palettes[0], 1, 1f, modifier.clip(RoundedCornerShape(10.dp)))
    }
}

@Composable
private fun UrlThumb(url: String, modifier: Modifier) {
    val ctx = LocalContext.current
    val img by produceState(RemoteImages.peek(url), url) { if (value == null) value = RemoteImages.thumb(ctx, url) }
    Box(modifier, contentAlignment = Alignment.Center) {
        img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            ?: Box(Modifier.fillMaxSize(.5f).clip(CircleShape).background(Color.White.copy(alpha = .05f)))
    }
}

@Composable
private fun IconThumb(id: String, modifier: Modifier) {
    val ctx = LocalContext.current
    val v = Iconify.version
    val art by produceState<Iconify.Art?>(null, id, v) { value = withContext(Dispatchers.Default) { Iconify.peek(ctx, id, 96) } }
    Box(modifier.padding(4.dp), contentAlignment = Alignment.Center) {
        art?.let { a -> Image(a.image, id, Modifier.fillMaxSize(), colorFilter = if (a.mono) ColorFilter.tint(Ui.Text1) else null) }
    }
}

/** A watch part drawn alone on a small plate, cropped to its own size. */
@Composable
fun ElementThumb(layer: Layer, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val assets = rememberAssets()
    val doc = remember(layer) {
        val (w, h) = layer.box()
        val side = maxOf(w, h) * 1.15f
        FaceDoc(0xFF141416, listOf(layer.copy(x = side / 2, y = side / 2)), w = side, h = side, round = false, corner = side * .2f)
    }
    Canvas(modifier.aspectRatio(1f)) { drawDoc(doc, Color(doc.background), measurer, ThumbTime, assets) }
}
