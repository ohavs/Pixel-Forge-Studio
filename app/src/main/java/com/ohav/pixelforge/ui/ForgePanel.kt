package com.ohav.pixelforge.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohav.pixelforge.editor.EditorState
import com.ohav.pixelforge.editor.FaceDoc
import com.ohav.pixelforge.editor.Studio
import com.ohav.pixelforge.forge.AiImages
import com.ohav.pixelforge.forge.BgSource
import com.ohav.pixelforge.forge.ClockType
import com.ohav.pixelforge.forge.Forge
import com.ohav.pixelforge.forge.Forged
import com.ohav.pixelforge.forge.GoogleFonts
import com.ohav.pixelforge.forge.Recipe
import com.ohav.pixelforge.forge.Style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private const val VARIANTS = 6

/**
 * The generator: pick a style and what the design is made of, get six genuinely different
 * finished designs, open one in the editor. Same panel for faces and widgets.
 */
@Composable
fun ForgePanel(state: EditorState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFace = state.studio == Studio.FACE
    var recipe by remember { mutableStateOf(Recipe()) }
    val results = remember { mutableStateListOf<Forged>() }
    var job by remember { mutableStateOf<Job?>(null) }
    var picked by remember { mutableIntStateOf(-1) }
    val assets = rememberAssets()
    var busy by remember { mutableStateOf(false) }

    fun generate() {
        job?.cancel()
        results.clear(); picked = -1
        val canvas = if (isFace) FaceDoc() else state.doc.copy(layers = emptyList())
        val seed = Random.nextInt(1, 1_000_000)
        val r = recipe
        job = scope.launch {
            busy = true
            try {
                repeat(VARIANTS) { v -> results += withContext(Dispatchers.Default) { Forge.generate(ctx, r, canvas, seed, v) } }
            } finally { busy = false }
        }
    }

    Column(Modifier.animateContentSize()) {
        Text("סגנון", fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Style.entries.forEach { s ->
                FilterChip(recipe.style == s, { recipe = recipe.copy(style = s, palette = -1, font = "") }, { Text(s.title) })
            }
        }

        Text("חומר הרקע", fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BgSource.entries.forEach { b -> FilterChip(recipe.bg == b, { recipe = recipe.copy(bg = b) }, { Text(b.title) }) }
        }
        if (recipe.bg == BgSource.AI || (recipe.bg == BgSource.AUTO && recipe.style == Style.ART)) {
            OutlinedTextField(
                recipe.prompt, { recipe = recipe.copy(prompt = it) }, Modifier.fillMaxWidth(),
                label = { Text("תאר את התמונה (באנגלית עדיף)") }, placeholder = { Text("e.g. koi fish in ink, gold leaf") }, maxLines = 3,
            )
            val provider = if (AiImages.provider(ctx) == "gemini") "Gemini" else "Pollinations (חינמי)"
            Text("$provider · נשארו ${AiImages.remaining(ctx)} תמונות היום · כל וריאציה = תמונה", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
        }

        if (isFace) {
            Text("שעון", fontWeight = FontWeight.SemiBold)
            Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClockType.entries.forEach { t -> FilterChip(recipe.type == t, { recipe = recipe.copy(type = t) }, { Text(t.title) }) }
            }
            Text("מורכבות: " + listOf("נקי", "עשיר", "מקסימלי")[recipe.complexity - 1], fontWeight = FontWeight.SemiBold)
            Slider(recipe.complexity.toFloat(), { recipe = recipe.copy(complexity = it.toInt().coerceIn(1, 3)) }, valueRange = 1f..3f, steps = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("נתונים חיים (צעדים, דופק, סוללה)", Modifier.weight(1f))
                Switch(recipe.data, { recipe = recipe.copy(data = it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("מחוג / ספרות שניות", Modifier.weight(1f))
                Switch(recipe.seconds, { recipe = recipe.copy(seconds = it) })
            }
        }

        Text("ערכת צבע", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(recipe.palette < 0, { recipe = recipe.copy(palette = -1) }, { Text("מגוון") })
            recipe.style.palettes.forEachIndexed { i, p ->
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(p.base), Color(p.base2), Color(p.accent))))
                        .border(if (recipe.palette == i) 3.dp else 1.dp, if (recipe.palette == i) Color.White else Color.White.copy(alpha = .25f), CircleShape)
                        .clickable { recipe = recipe.copy(palette = i) },
                )
            }
        }

        OutlinedTextField(recipe.brand, { recipe = recipe.copy(brand = it.take(18)) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("שם על הדיאל") })

        FontPicker(recipe.style, recipe.font) { recipe = recipe.copy(font = it) }

        Spacer(Modifier.height(10.dp))
        Button({ generate() }, Modifier.fillMaxWidth().height(52.dp), enabled = !busy) {
            Icon(Icons.Outlined.AutoAwesome, null)
            Spacer(Modifier.width(8.dp))
            Text(if (busy) "יוצר ${results.size + 1}/$VARIANTS…" else "צור $VARIANTS וריאציות", fontSize = 16.sp)
        }
        if (busy) LinearProgressIndicator({ results.size / VARIANTS.toFloat() }, Modifier.fillMaxWidth().padding(top = 6.dp))

        results.mapNotNull { it.note.ifEmpty { null } }.distinct().forEach {
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
        }
        if (results.isNotEmpty()) {
            Text("לחיצה פותחת בעורך (אפשר לבטל)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
            val perRow = if (isFace || state.doc.w / state.doc.h < 1.3f) 3 else 2
            results.chunked(perRow).forEachIndexed { row, items ->
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEachIndexed { i, f ->
                        val idx = row * perRow + i
                        Box(
                            Modifier.weight(1f).enter(i).clip(RoundedCornerShape(18.dp))
                                .border(2.dp, if (picked == idx) Violet else Color.Transparent, RoundedCornerShape(18.dp))
                                .clickable { picked = idx; state.replaceWith(f.doc) }.padding(6.dp),
                            contentAlignment = Alignment.Center,
                        ) { FaceThumb(f.doc, assets, if (perRow == 3) 100.dp else 80.dp) }
                    }
                    repeat(perRow - items.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Curated fonts for the style, plus search across the whole Google Fonts catalogue. */
@Composable
private fun FontPicker(style: Style, selected: String, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var all by remember { mutableStateOf<List<String>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(searching) { if (searching && all.isEmpty()) all = GoogleFonts.all(ctx) }
    Text("פונט", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected.isEmpty(), { onPick("") }, { Text("מגוון") })
        (listOfNotNull(selected.takeIf { it.isNotEmpty() && it !in style.fonts }) + style.fonts).forEach { f ->
            FilterChip(selected == f, { onPick(f) }, { Text(f) })
        }
        AssistChip({ searching = !searching }, { Text(if (searching) "סגור חיפוש" else "כל Google Fonts…") })
    }
    if (searching) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("חיפוש ב-${all.size.takeIf { it > 0 } ?: "…"} משפחות") })
        val hits = all.filter { it.contains(query.trim(), ignoreCase = true) }.take(24)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            hits.forEach { f -> FilterChip(selected == f, { onPick(f); searching = false }, { Text(f) }) }
        }
    }
}
