package com.ohav.pixelforge.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The editor's asset libraries. Nothing heavy ships in the APK: catalogs come from the repo's
 * `assets` branch (built by tools/assets on CI), pictures from each library's own host, and
 * everything fetched is cached on the device.
 */
object Library {
    const val ASSETS = "https://raw.githubusercontent.com/ohavs/Pixel-Forge-Studio/assets"
    private const val FLUENT = "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main"

    /** What a grid tile shows: a remote picture, an Iconify icon, a generator or a ready layer. */
    sealed interface Thumb {
        data class Url(val url: String) : Thumb
        data class Icon(val id: String) : Thumb
        data class Gen(val g: Generator) : Thumb
        data class Element(val layer: Layer) : Thumb
    }

    class Item(val title: String, val thumb: Thumb, val full: String = "")
    class Category(val title: String, val items: List<Item>)

    enum class Kind { ELEMENTS, GENERATORS, ICONS3D, FLUENT3D, METEOCONS, TABLER, KENNEY, TEXTURES, VECTORS, PHOTOS }

    /** [cover] previews the library on its card; [faceOnly] libraries make no sense in widgets. */
    class Lib(val kind: Kind, val title: String, val subtitle: String, val credit: String, val cover: List<Thumb>, val search: Boolean = false)

    val all = listOf(
        Lib(Kind.ELEMENTS, "רכיבי שעון", "שעה, מחוגים, חוגות, נתונים", "Pixel Forge", emptyList()),
        Lib(Kind.GENERATORS, "רקעים מחוללים", "גלים, כתמים, פסגות, טופוגרפיה", "בהשראת Haikei", emptyList()),
        Lib(Kind.ICONS3D, "3D Icons", "אייקוני תלת-ממד ב-4 סגנונות", "3dicons · CC0", listOf("rocket", "fire", "heart", "star").map { Thumb.Url("$ASSETS/3dicons/color/$it.png") }),
        Lib(Kind.FLUENT3D, "Fluent Emoji 3D", "אימוג'י תלת-ממדי של Microsoft", "Microsoft · MIT", listOf("Sparkles/3D/sparkles_3d.png", "Fire/3D/fire_3d.png", "Rocket/3D/rocket_3d.png", "Red heart/3D/red_heart_3d.png").map { Thumb.Url(fluentUrl("assets/$it")) }),
        Lib(Kind.METEOCONS, "Meteocons", "מזג אוויר מאויר", "Bas Milius · MIT", listOf("clear-day-fill", "partly-cloudy-night-fill", "thunderstorms-rain-fill", "snowflake-fill").map { Thumb.Icon("meteocons:$it") }, search = true),
        Lib(Kind.TABLER, "Tabler Icons", "5,000+ אייקוני קו נקיים", "Tabler · MIT", listOf("heart", "bolt", "moon", "run").map { Thumb.Icon("tabler:$it") }, search = true),
        Lib(Kind.KENNEY, "Sci-Fi UI", "פאנלים, מסגרות ופסים עתידניים", "Kenney · CC0", emptyList()),
        Lib(Kind.TEXTURES, "טקסטורות", "צילומי חומר כרקע: עץ, מתכת, אבן", "Poly Haven · CC0", listOf("wood_table_001", "metal_plate", "rock_wall_08", "marble_01").map { Thumb.Url(PolyHaven.thumb(it)) }),
        Lib(Kind.VECTORS, "וקטורים", "Phosphor, Solar, Noto, דגלים ועוד", "Iconify · קוד פתוח", listOf("ph:planet-duotone", "noto:sun-with-face", "game-icons:crystal-ball", "circle-flags:il").map { Thumb.Icon(it) }, search = true),
        Lib(Kind.PHOTOS, "התמונות שלי", "מהגלריה של הטלפון", "", emptyList()),
    )

    fun fluentUrl(path: String) = "$FLUENT/${path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }}"

    private val catalogs = HashMap<String, List<Category>>()

    /** The library's categories (cached in memory; catalogs on disk for a week). */
    suspend fun categories(ctx: Context, lib: Lib, studio: Studio): List<Category> = when (lib.kind) {
        Kind.ELEMENTS -> Elements.categories(studio == Studio.FACE)
        Kind.GENERATORS -> listOf(Category("רקעים", Generator.entries.map { Item(it.title, Thumb.Gen(it)) }))
        Kind.TEXTURES -> catalogs.getOrPut("textures") { PolyHaven.categories(ctx) }
        Kind.ICONS3D -> catalogs.getOrPut("3dicons") {
            val o = JSONObject(cachedText(ctx, "3dicons.json", "$ASSETS/catalog/3dicons.json"))
            val names = o.getJSONArray("items").objects().map { it.getString("n") }
            val titles = mapOf("color" to "צבעוני", "clay" to "חימר", "gradient" to "גרדיאנט", "premium" to "פרימיום")
            o.getJSONArray("styles").strings().map { s ->
                Category(titles[s] ?: s, names.map { n -> Item(n, Thumb.Url("$ASSETS/3dicons/$s/$n.png")) })
            }
        }
        Kind.FLUENT3D -> catalogs.getOrPut("fluent3d") {
            groups(ctx, "fluent3d") { o -> Item(o.getString("n"), Thumb.Url(fluentUrl(o.getString("f")))) }
        }
        Kind.KENNEY -> catalogs.getOrPut("kenney") {
            groups(ctx, "kenney-scifi") { o -> Item(o.getString("n"), Thumb.Url("$ASSETS/${o.getString("f")}")) }
        }
        Kind.METEOCONS, Kind.TABLER, Kind.VECTORS -> {
            val name = when (lib.kind) { Kind.METEOCONS -> "meteocons"; Kind.TABLER -> "tabler"; else -> "vectors" }
            catalogs.getOrPut(name) { groups(ctx, name) { o -> o.getString("n").let { Item(it.substringAfter(':'), Thumb.Icon(it)) } } }
        }
        Kind.PHOTOS -> emptyList()
    }

    private suspend fun groups(ctx: Context, name: String, item: (JSONObject) -> Item): List<Category> {
        val arr = JSONArray(cachedText(ctx, "$name.json", "$ASSETS/catalog/$name.json"))
        return arr.objects().map { g -> Category(GroupTitles[g.getString("g")] ?: g.getString("g"), g.getJSONArray("items").objects().map(item)) }
    }

    /** Group names in the catalogs are English; the common ones get Hebrew titles. */
    private val GroupTitles = mapOf(
        "Smileys & Emotion" to "פרצופים ורגשות", "People & Body" to "אנשים", "Animals & Nature" to "חיות וטבע", "Food & Drink" to "אוכל ושתייה",
        "Travel & Places" to "מסע ומקומות", "Activities" to "פעילויות", "Objects" to "חפצים", "Symbols" to "סמלים", "Flags" to "דגלים",
        "Sun" to "שמש", "Night" to "לילה", "Clouds" to "עננים", "Rain" to "גשם", "Snow" to "שלג", "Storm" to "סערה", "Wind" to "רוח",
        "Temperature" to "טמפרטורה", "More" to "עוד", "Line" to "קווי", "Filled" to "מלאים", "Weather" to "מזג אוויר", "Arrows" to "חצים",
        "Shapes" to "צורות", "Sport" to "ספורט", "Health" to "בריאות", "Nature" to "טבע", "Food" to "אוכל", "Devices" to "מכשירים",
        "Media" to "מדיה", "Map" to "מפה", "Vehicles" to "כלי רכב", "Animals" to "חיות", "Mood" to "מצב רוח", "Communication" to "תקשורת",
        "Zodiac" to "מזלות", "Games" to "משחקים", "Buildings" to "מבנים", "Design" to "עיצוב", "System" to "מערכת", "Brand" to "מותגים",
        "Numbers" to "מספרים", "Letters" to "אותיות", "Charts" to "גרפים", "Currencies" to "מטבעות", "Flags" to "דגלים",
        "panel" to "פאנלים", "bar" to "פסים", "button" to "כפתורים", "glass" to "זכוכית", "metal" to "מתכת", "square" to "ריבועים",
    )

    /** A catalog file: fresh for a week, the stale copy when offline. */
    private suspend fun cachedText(ctx: Context, name: String, url: String): String = withContext(Dispatchers.IO) {
        val f = File(ctx.filesDir, "library/$name")
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < 7 * 86_400_000L) return@withContext f.readText()
        try {
            Net.get(url).decodeToString().also { f.parentFile?.mkdirs(); f.writeText(it) }
        } catch (e: Exception) {
            if (f.exists()) f.readText() else throw e
        }
    }

    private fun JSONArray.objects() = List(length()) { getJSONObject(it) }
    private fun JSONArray.strings() = List(length()) { getString(it) }
}

/** Poly Haven textures (CC0): categories from its API, a 1k colour map as a background. */
object PolyHaven {
    private const val API = "https://api.polyhaven.com"

    fun thumb(id: String) = "https://cdn.polyhaven.com/asset_img/thumbs/$id.png?width=256&height=256"

    private val Titles = mapOf(
        "wood" to "עץ", "metal" to "מתכת", "rock" to "סלע", "brick" to "לבנים", "fabric" to "בד", "concrete" to "בטון",
        "plaster" to "טיח", "floor" to "רצפה", "terrain" to "קרקע", "natural" to "טבעי", "man made" to "מעשה ידי אדם",
        "stone" to "אבן", "wall" to "קיר", "sand" to "חול", "roofing" to "גגות", "tiles" to "אריחים", "ground" to "אדמה",
        "outdoor" to "חוץ", "indoor" to "פנים", "clean" to "נקי", "dirty" to "מלוכלך", "rough" to "מחוספס", "painted" to "צבוע",
    )

    suspend fun categories(ctx: Context): List<Library.Category> = withContext(Dispatchers.IO) {
        val all = JSONObject(Net.get("$API/assets?t=textures").decodeToString())
        val byCat = LinkedHashMap<String, MutableList<String>>()
        all.keys().forEach { id ->
            val cats = all.getJSONObject(id).optJSONArray("categories") ?: return@forEach
            for (i in 0 until cats.length()) byCat.getOrPut(cats.getString(i)) { mutableListOf() } += id
        }
        byCat.entries.filter { it.value.size >= 8 }.sortedByDescending { it.value.size }.take(16).map { (c, ids) ->
            Library.Category(Titles[c] ?: c, ids.sorted().map { id -> Library.Item(all.getJSONObject(id).optString("name", id), Library.Thumb.Url(thumb(id)), id) })
        }
    }

    /** Downloads the texture's 1k colour map. */
    suspend fun picture(id: String): Bitmap = withContext(Dispatchers.IO) {
        val files = JSONObject(Net.get("$API/files/$id").decodeToString())
        val key = files.keys().asSequence().firstOrNull { it.equals("Diffuse", true) } ?: files.keys().asSequence().firstOrNull { it.contains("diff", true) || it.contains("color", true) }
        val url = key?.let { files.getJSONObject(it).optJSONObject("1k")?.optJSONObject("jpg")?.optString("url") }
            ?: "https://dl.polyhaven.org/file/ph-assets/Textures/jpg/1k/$id/${id}_diff_1k.jpg"
        val bytes = Net.get(url, 60_000)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("bad image")
    }
}

object Net {
    fun get(url: String, timeout: Int = 20_000): ByteArray {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = timeout; setRequestProperty("User-Agent", "PixelForge") }
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            return conn.inputStream.use { it.readBytes() }
        } finally { conn.disconnect() }
    }
}

/** Grid thumbnails from the web: decoded small, kept in memory and in the cache dir. */
object RemoteImages {
    private val mem = LruCache<String, ImageBitmap>(300)
    private val gate = Semaphore(6)

    fun peek(url: String): ImageBitmap? = mem.get(url)

    suspend fun thumb(ctx: Context, url: String, px: Int = 192): ImageBitmap? = mem.get(url) ?: withContext(Dispatchers.IO) {
        runCatching {
            val f = File(ctx.cacheDir, "thumbs/${url.hashCode().toUInt()}_${url.length}")
            if (!f.exists()) gate.withPermit { f.parentFile?.mkdirs(); f.writeBytes(Net.get(url)) }
            decode(f.path, px)?.asImageBitmap()
        }.getOrNull()?.also { mem.put(url, it) }
    }

    /** The full file (cached), for adding to a design. */
    suspend fun bytes(ctx: Context, url: String): ByteArray = withContext(Dispatchers.IO) {
        val f = File(ctx.cacheDir, "thumbs/${url.hashCode().toUInt()}_${url.length}")
        if (f.exists()) f.readBytes() else Net.get(url, 60_000).also { f.parentFile?.mkdirs(); f.writeBytes(it) }
    }

    private fun decode(path: String, px: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        var s = 1
        while (max(o.outWidth, o.outHeight) / (s * 2) >= px) s *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
    }
}

/** Ready-made watch parts, laid out on the 450 face; the editor centres them on its canvas. */
object Elements {
    private const val ACCENT = 0xFF5AC8FA
    private fun l(kind: LayerKind, f: (Layer) -> Layer = { it }) = f(Layer.create(kind, 0, ACCENT).copy(x = FACE_SIZE / 2, y = FACE_SIZE / 2))

    fun categories(face: Boolean): List<Library.Category> = buildList {
        add(Library.Category("שעה ותאריך", listOf(
            Library.Item("שעה", Library.Thumb.Element(l(LayerKind.TIME))),
            Library.Item("שעה דקה", Library.Thumb.Element(l(LayerKind.TIME) { it.copy(weight = 0, size = 110f) })),
            Library.Item("שעה ענקית", Library.Thumb.Element(l(LayerKind.TIME) { it.copy(weight = 4, size = 150f) })),
            Library.Item("עם שניות", Library.Thumb.Element(l(LayerKind.TIME) { it.copy(extra = true, size = 64f, weight = 2) })),
            Library.Item("תאריך", Library.Thumb.Element(l(LayerKind.DATE))),
            Library.Item("תאריך לבן", Library.Thumb.Element(l(LayerKind.DATE) { it.copy(color = 0xFFFFFFFF, weight = 1) })),
            Library.Item("טקסט", Library.Thumb.Element(l(LayerKind.TEXT))),
        )))
        if (face) add(Library.Category("מחוגים", (listOf("") + HandStyles).map { s ->
            Library.Item(s.ifEmpty { "פשוט" }, Library.Thumb.Element(l(LayerKind.HANDS) { it.copy(text = s) }))
        }))
        add(Library.Category("חוגות וטבעות", listOf(
            Library.Item("12 סימונים", Library.Thumb.Element(l(LayerKind.TICKS))),
            Library.Item("60 סימונים", Library.Thumb.Element(l(LayerKind.TICKS) { it.copy(count = 60, height = 8f, thickness = 2f, color = 0xAAFFFFFF) })),
            Library.Item("טבעת", Library.Thumb.Element(l(LayerKind.RING))),
            Library.Item("טבעת דקה", Library.Thumb.Element(l(LayerKind.RING) { it.copy(thickness = 2f, color = 0x66FFFFFF) })),
            Library.Item("קשת", Library.Thumb.Element(l(LayerKind.ARC))),
        )))
        if (face) add(Library.Category("נתונים חיים", DataSource.entries.map { d ->
            Library.Item(d.title, Library.Thumb.Element(l(LayerKind.DATA) { it.copy(text = d.name) }))
        } + listOf(
            Library.Item("מד סוללה", Library.Thumb.Element(l(LayerKind.GAUGE))),
            Library.Item("מחוון צעדים", Library.Thumb.Element(l(LayerKind.GAUGE) { it.copy(text = DataSource.STEPS.name, extra = true, size = 90f, thickness = 6f) })),
        )))
        add(Library.Category("צורות", listOf(
            Library.Item("עיגול", Library.Thumb.Element(l(LayerKind.CIRCLE) { it.copy(size = 60f) })),
            Library.Item("מלבן", Library.Thumb.Element(l(LayerKind.RECT))),
            Library.Item("קפסולה", Library.Thumb.Element(l(LayerKind.RECT) { it.copy(size = 200f, height = 60f, thickness = 30f, color = 0x33FFFFFF) })),
        )))
    }
}

/** Saves library pictures into the design's image folder so they work offline and on the watch. */
object LibraryAdd {
    /** Stores [bmp] (max [maxPx] side) and returns its file name; the layer shows [title]. */
    fun store(ctx: Context, bmp: Bitmap, title: String, maxPx: Int = 900): String {
        val s = maxPx.toFloat() / max(bmp.width, bmp.height)
        val b = if (s < 1) Bitmap.createScaledBitmap(bmp, (bmp.width * s).roundToInt(), (bmp.height * s).roundToInt(), true) else bmp
        val name = "lib~${title.replace(Regex("[/~\\\\]"), " ").take(40)}~${System.currentTimeMillis()}.png"
        File(ImageAssets.dir(ctx), name).outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return name
    }
}
