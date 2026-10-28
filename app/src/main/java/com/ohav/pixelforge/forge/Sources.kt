package com.ohav.pixelforge.forge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

private const val UA = "OrbitStudio/1.0 (Android; github.com/ohavs/pixel-forge-studio)"

private fun http(url: String, method: String = "GET", body: String? = null, headers: Map<String, String> = emptyMap(), timeout: Int = 30_000): ByteArray {
    val c = URL(url).openConnection() as HttpURLConnection
    c.requestMethod = method
    c.connectTimeout = 15_000; c.readTimeout = timeout
    c.setRequestProperty("User-Agent", headers["User-Agent"] ?: UA)
    headers.forEach { (k, v) -> if (k != "User-Agent") c.setRequestProperty(k, v) }
    if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray()) } }
    val code = c.responseCode
    if (code !in 200..299) {
        val err = c.errorStream?.readBytes()?.decodeToString()?.take(300) ?: ""
        throw IllegalStateException("HTTP $code $err")
    }
    return c.inputStream.use { it.readBytes() }
}

/**
 * Google Fonts: the whole catalogue (~1,700 families), free. The CSS API hands an Android user
 * agent plain TTF links; files are cached in filesDir/fonts, so a font downloads once.
 */
object GoogleFonts {
    private val loaded = HashMap<String, Typeface>()
    private var catalogue: List<String>? = null

    fun cachedFile(ctx: Context, family: String) = File(ctx.filesDir, "fonts/${family.replace(' ', '_')}.ttf")

    suspend fun typeface(ctx: Context, family: String): Typeface? = withContext(Dispatchers.IO) {
        loaded[family]?.let { return@withContext it }
        val f = cachedFile(ctx, family)
        if (!f.exists()) runCatching {
            val q = URLEncoder.encode(family, "UTF-8").replace("+", "%20")
            val css = http("https://fonts.googleapis.com/css2?family=$q", headers = mapOf("User-Agent" to "Dalvik/2.1.0 (Linux; U; Android 14)")).decodeToString()
            val url = Regex("""url\((https://[^)]+)\)""").find(css)?.groupValues?.get(1) ?: return@runCatching
            f.parentFile?.mkdirs()
            f.writeBytes(http(url))
        }
        if (!f.exists()) return@withContext null
        runCatching { Typeface.createFromFile(f) }.getOrNull()?.also { loaded[family] = it }
    }

    /** Every family name (fetched once, cached in filesDir), or the curated lists when offline. */
    suspend fun all(ctx: Context): List<String> = withContext(Dispatchers.IO) {
        catalogue?.let { return@withContext it }
        val file = File(ctx.filesDir, "fonts/catalogue.json")
        val text = runCatching { if (file.exists()) file.readText() else null }.getOrNull()
            ?: runCatching { http("https://fonts.google.com/metadata/fonts").decodeToString().substringAfter(")]}'").also { file.parentFile?.mkdirs(); file.writeText(it) } }.getOrNull()
        val names = text?.let { t ->
            runCatching {
                val arr = JSONObject(t).getJSONArray("familyMetadataList")
                List(arr.length()) { arr.getJSONObject(it) }.sortedBy { it.optInt("popularity", 99999) }.map { it.getString("family") }
            }.getOrNull()
        } ?: Style.entries.flatMap { it.fonts }.distinct()
        names.also { catalogue = it }
    }
}

/**
 * AI images for exactly one use and size. Pollinations (Flux, free, no key) by default; Gemini
 * image when a key is set (paid, better). Every call counts towards a daily cap from settings.
 */
object AiImages {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun cap(ctx: Context) = prefs(ctx).getInt("ai_cap", 30)
    fun usedToday(ctx: Context): Int = prefs(ctx).let { if (it.getString("ai_day", "") == LocalDate.now().toString()) it.getInt("ai_used", 0) else 0 }
    fun remaining(ctx: Context) = (cap(ctx) - usedToday(ctx)).coerceAtLeast(0)

    private fun count(ctx: Context) {
        val n = usedToday(ctx) + 1
        prefs(ctx).edit().putString("ai_day", LocalDate.now().toString()).putInt("ai_used", n).apply()
    }

    /** "auto" = Gemini when a key is set, else Pollinations. */
    fun provider(ctx: Context): String = prefs(ctx).getString("ai_provider", "auto")!!.let {
        if (it == "auto") (if (prefs(ctx).getString("ai_key", "").isNullOrBlank()) "pollinations" else "gemini") else it
    }

    /** An image [w] x [h] pixels (center-cropped to exactly that), or an exception with the reason. */
    suspend fun generate(ctx: Context, prompt: String, w: Int, h: Int, seed: Int): Bitmap = withContext(Dispatchers.IO) {
        check(remaining(ctx) > 0) { "המכסה היומית של AI נוצלה (${cap(ctx)}). אפשר לשנות בהגדרות." }
        val cacheFile = File(ctx.cacheDir, "ai/${(prompt + w + "x" + h + seed + provider(ctx)).hashCode()}.png")
        val raw = if (cacheFile.exists()) BitmapFactory.decodeFile(cacheFile.path) else {
            val bytes = when (provider(ctx)) {
                "gemini" -> runCatching { gemini(ctx, prompt, w, h) }.getOrElse { pollinations(prompt, w, h, seed) }
                else -> pollinations(prompt, w, h, seed)
            }
            count(ctx)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("התמונה לא התקבלה")
        }
        if (!cacheFile.exists()) { cacheFile.parentFile?.mkdirs(); cacheFile.outputStream().use { raw.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        fit(raw, w, h)
    }

    private fun pollinations(prompt: String, w: Int, h: Int, seed: Int): ByteArray {
        val s = 1024f / max(w, h)
        val rw = ((w * s) / 64).roundToInt() * 64; val rh = ((h * s) / 64).roundToInt() * 64
        val q = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
        return http("https://image.pollinations.ai/prompt/$q?width=$rw&height=$rh&model=flux&nologo=true&private=true&seed=$seed", timeout = 120_000)
    }

    private val Ratios = listOf("1:1" to 1f, "4:3" to 4 / 3f, "3:2" to 1.5f, "16:9" to 16 / 9f, "21:9" to 21 / 9f, "3:4" to .75f, "9:16" to 9 / 16f)

    private fun gemini(ctx: Context, prompt: String, w: Int, h: Int): ByteArray {
        val key = prefs(ctx).getString("ai_key", "")!!
        val ratio = Ratios.minBy { kotlin.math.abs(it.second - w.toFloat() / h) }.first
        val body = JSONObject()
            .put("contents", org.json.JSONArray().put(JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject().put("responseModalities", org.json.JSONArray().put("IMAGE")).put("imageConfig", JSONObject().put("aspectRatio", ratio)))
        val res = JSONObject(http(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent", "POST", body.toString(),
            mapOf("x-goog-api-key" to key, "Content-Type" to "application/json"), 120_000,
        ).decodeToString())
        val parts = res.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        for (i in 0 until parts.length()) {
            val d = parts.getJSONObject(i).optJSONObject("inlineData") ?: continue
            return Base64.decode(d.getString("data"), Base64.DEFAULT)
        }
        error("Gemini לא החזיר תמונה")
    }

    fun fit(src: Bitmap, w: Int, h: Int): Bitmap {
        val r = DialArt.centerCrop(src.width, src.height, w.toFloat(), h.toFloat())
        return Bitmap.createScaledBitmap(Bitmap.createBitmap(src, r.left, r.top, r.width(), r.height()), w, h, true)
    }
}

/** CC0 material photos from Poly Haven (polyhaven.com): metal, fabric, wood, stone… free, no key. */
object Textures {
    private val lists = HashMap<String, List<String>>()

    private fun dir(ctx: Context) = File(ctx.filesDir, "textures").apply { mkdirs() }

    private fun ids(ctx: Context, category: String): List<String> = lists.getOrPut(category) {
        val f = File(dir(ctx), "list_$category.json")
        val text = if (f.exists() && System.currentTimeMillis() - f.lastModified() < 7 * 86_400_000L) f.readText()
        else http("https://api.polyhaven.com/assets?t=textures&c=${URLEncoder.encode(category, "UTF-8")}").decodeToString().also { f.writeText(it) }
        JSONObject(text).keys().asSequence().toList().sorted()
    }

    /** A random texture photo from one of [categories] (about 1024px), or null when offline. */
    suspend fun pick(ctx: Context, categories: List<String>, seed: Int): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val rnd = kotlin.random.Random(seed)
            val all = categories.flatMap { c -> runCatching { ids(ctx, c) }.getOrDefault(emptyList()) }.distinct()
            if (all.isEmpty()) return@runCatching null
            val id = all[rnd.nextInt(all.size)]
            val f = File(dir(ctx), "$id.jpg")
            if (!f.exists()) {
                val files = JSONObject(http("https://api.polyhaven.com/files/$id").decodeToString())
                val key = files.keys().asSequence().firstOrNull { it.contains("diff", true) || it.contains("color", true) }
                val url = key?.let { files.getJSONObject(it).optJSONObject("1k")?.optJSONObject("jpg")?.optString("url") }
                    ?: "https://dl.polyhaven.org/file/ph-assets/Textures/jpg/1k/$id/${id}_diff_1k.jpg"
                f.writeBytes(http(url, timeout = 60_000))
            }
            BitmapFactory.decodeFile(f.path)
        }.getOrNull()
    }
}
