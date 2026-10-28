package com.ohav.pixelforge.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Collections

/**
 * Icons from Iconify (api.iconify.design): 200k+ open-source icons from 200+ sets, free, no key.
 * A layer refers to one as "prefix:name". Fetched SVGs are cached forever in filesDir/icons, so
 * a face keeps working offline. Single-color icons use currentColor and are tinted with the
 * layer color; multicolor ones (emoji, weather...) keep their own colors.
 */
object Iconify {
    private const val API = "https://api.iconify.design"
    /** Side of the bitmaps drawn on the editor canvas and in widgets. */
    const val ART_PX = 384

    class Pack(val prefix: String, val title: String)

    /** Curated sets shown as tabs in the picker. Multicolor first: they dress a design up most. */
    val packs = listOf(
        Pack("fluent-emoji", "אימוג'י 3D"),
        Pack("fluent-emoji-flat", "אימוג'י שטוח"),
        Pack("noto", "Noto"),
        Pack("fluent-color", "צבעוני"),
        Pack("meteocons", "מזג אוויר"),
        Pack("streamline-emojis", "Streamline"),
        Pack("flat-color-icons", "Flat"),
        Pack("circle-flags", "דגלים"),
        Pack("ph", "Phosphor"),
        Pack("solar", "Solar"),
        Pack("material-symbols", "Material"),
        Pack("tabler", "Tabler"),
        Pack("game-icons", "משחקים"),
    )

    class Art(val bitmap: Bitmap, val mono: Boolean) {
        val image: ImageBitmap by lazy { bitmap.asImageBitmap() }
    }

    /** Bumped whenever icons land in the cache, so canvases that drew a placeholder redraw. */
    var version by mutableIntStateOf(0)
        private set

    private val arts = LruCache<String, Art>(160)
    private val pending = Collections.synchronizedSet(HashSet<String>())
    private val names = HashMap<String, List<String>>()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isIconify(id: String) = ':' in id

    private fun file(ctx: Context, prefix: String, name: String) = File(ctx.filesDir, "icons/$prefix/$name.svg")
    private fun file(ctx: Context, id: String) = file(ctx, id.substringBefore(':'), id.substringAfter(':'))

    fun cached(ctx: Context, id: String): String? = file(ctx, id).takeIf { it.exists() }?.readText()

    /** The icon rendered at [px] if it is cached on disk (no network). */
    fun peek(ctx: Context, id: String, px: Int = ART_PX): Art? {
        val key = "$id@$px"
        arts.get(key)?.let { return it }
        val svg = cached(ctx, id) ?: return null
        return runCatching { Art(render(svg, px, px), "currentColor" in svg) }.getOrNull()?.also { arts.put(key, it) }
    }

    /** Like [peek], but starts a download when the icon is missing ([version] changes when it lands). */
    fun art(ctx: Context, id: String, px: Int = ART_PX): Art? = peek(ctx, id, px) ?: run { request(ctx, id); null }

    private fun request(ctx: Context, id: String) {
        if (!pending.add(id)) return
        io.launch {
            val ok = runCatching { fetch(ctx, id.substringBefore(':'), listOf(id.substringAfter(':'))) }.isSuccess
            if (!ok) delay(30_000) // offline: don't retry on every redraw
            pending.remove(id)
        }
    }

    /** Downloads every listed icon not yet cached (all of them before sending a face to the watch). */
    suspend fun ensure(ctx: Context, ids: Collection<String>) {
        ids.filter { isIconify(it) && !file(ctx, it).exists() }.groupBy { it.substringBefore(':') }
            .forEach { (prefix, list) -> runCatching { fetch(ctx, prefix, list.map { it.substringAfter(':') }) } }
    }

    /** Downloads [names] of one set in batches (one request per 60 icons) and caches them as SVG files. */
    suspend fun fetch(ctx: Context, prefix: String, names: List<String>) = withContext(Dispatchers.IO) {
        val need = names.filter { !file(ctx, prefix, it).exists() }
        if (need.isEmpty()) return@withContext
        need.chunked(60).forEach { chunk ->
            val j = JSONObject(get("$API/$prefix.json?icons=${chunk.joinToString(",") { enc(it) }}"))
            val icons = j.getJSONObject("icons")
            val aliases = j.optJSONObject("aliases")
            for (n in chunk) {
                var key = n
                var hops = 0
                while (!icons.has(key) && aliases?.optJSONObject(key) != null && hops++ < 5) key = aliases.getJSONObject(key).getString("parent")
                val o = icons.optJSONObject(key) ?: continue
                fun dim(k: String, def: Double) = o.optDouble(k, j.optDouble(k, def))
                val vb = "${num(dim("left", 0.0))} ${num(dim("top", 0.0))} ${num(dim("width", 16.0))} ${num(dim("height", 16.0))}"
                val svg = """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="$vb">${o.getString("body")}</svg>"""
                file(ctx, prefix, n).apply { parentFile?.mkdirs() }.writeText(svg)
            }
        }
        withContext(Dispatchers.Main) { version++ }
    }

    /** Free-text search (English) across all sets, or one set when [prefix] is given. */
    suspend fun search(query: String, prefix: String? = null): List<String> = withContext(Dispatchers.IO) {
        val p = prefix?.let { "&prefix=${enc(it)}" } ?: ""
        val arr = JSONObject(get("$API/search?query=${enc(query)}&limit=120$p")).getJSONArray("icons")
        List(arr.length()) { arr.getString(it) }
    }

    /** All icon names of a set (cached in memory). */
    suspend fun collection(prefix: String): List<String> = names[prefix] ?: withContext(Dispatchers.IO) {
        val o = JSONObject(get("$API/collection?prefix=${enc(prefix)}"))
        val out = LinkedHashSet<String>()
        o.optJSONObject("categories")?.let { c -> c.keys().forEach { k -> c.getJSONArray(k).strings(out) } }
        o.optJSONArray("uncategorized")?.strings(out)
        o.optJSONArray("hidden")?.let { h -> List(h.length()) { h.getString(it) }.forEach(out::remove) }
        out.toList().also { names[prefix] = it }
    }

    /** Draws the icon into a [w] x [h] bitmap canvas (watch PNG export): tinted if single-color. */
    fun drawInto(ctx: Context, canvas: Canvas, id: String, w: Int, h: Int, color: Long) {
        val svg = cached(ctx, id) ?: return
        val bmp = runCatching { render(svg, w, h) }.getOrNull() ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if ("currentColor" in svg) paint.colorFilter = PorterDuffColorFilter(color.toInt(), PorterDuff.Mode.SRC_IN)
        else paint.alpha = (color ushr 24 and 0xFF).toInt()
        canvas.drawBitmap(bmp, 0f, 0f, paint)
    }

    private fun render(svgText: String, w: Int, h: Int): Bitmap {
        val svg = SVG.getFromString(svgText)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        svg.renderToCanvas(Canvas(bmp), RenderOptions().viewPort(0f, 0f, w.toFloat(), h.toFloat()))
        return bmp
    }

    private fun get(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 15_000 }
        try {
            if (conn.responseCode != 200) error("Iconify ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally { conn.disconnect() }
    }

    private fun JSONArray.strings(into: MutableCollection<String>) { for (i in 0 until length()) into += getString(i) }
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun num(d: Double) = if (d == Math.floor(d)) d.toLong().toString() else d.toString()
}
