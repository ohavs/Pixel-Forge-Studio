package com.ohav.pixelforge.forge

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import androidx.compose.ui.graphics.asAndroidPath
import com.ohav.pixelforge.editor.DataSource
import com.ohav.pixelforge.editor.IconLibrary
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class Texture { SUNBURST, GUILLOCHE, CLOUS, CARBON, BRUSHED, HEX, GRID, WAVES, AURORA, PHOTO }
enum class Indices { NONE, BATON, ARABIC, ROMAN, DOTS, DIVER, HUD }
enum class Track { NONE, RAILWAY, MINUTES, FINE }
enum class Bezel { NONE, DIVER, TACHY, COMPASS, HOURS24, SEGMENT }

/** A recessed small dial; its live needle is a GAUGE layer with the same [start]/[sweep]. */
class Subdial(val x: Float, val y: Float, val r: Float, val source: DataSource, val start: Float, val sweep: Float)

/** Track drawn behind an arc GAUGE, with a label and an optional icon at its start. */
class Meter(val x: Float, val y: Float, val r: Float, val thick: Float, val start: Float, val sweep: Float, val label: String, val icon: String?)

/** Everything one piece of art is made of. Coordinates are design units (450 face, widget w x h). */
class ArtSpec(
    val w: Float, val h: Float, val round: Boolean, val corner: Float,
    val pal: Pal, val texture: Texture, val photo: Bitmap?, val photoTint: Float,
    val indices: Indices = Indices.NONE, val track: Track = Track.NONE, val bezel: Bezel = Bezel.NONE,
    val subdials: List<Subdial> = emptyList(), val meters: List<Meter> = emptyList(),
    val windows: List<RectF> = emptyList(), val skipHours: Set<Int> = emptySet(),
    val brand: String = "", val sub: String = "", val font: Typeface = Typeface.SERIF, val font2: Typeface = Typeface.DEFAULT,
    val frame: String = "", val labels: List<Triple<String, Float, Float>> = emptyList(), val seed: Int = 0,
    /** Widgets: brand line left-aligned at (brandX, brandY) baseline, [brandSize] tall. */
    val brandX: Float = 0f, val brandY: Float = 0f, val brandSize: Float = 0f,
)

/**
 * Procedural dial and panel art: real watch-making finishes (sunburst, guilloché, clous de Paris,
 * carbon, brushed metal), applied indices with shadows and lume, bezels with scales, recessed
 * sub-dials, HUD details. All static detail goes into one picture; live parts stay layers.
 */
object DialArt {
    fun render(s: ArtSpec, scale: Float): Bitmap {
        val bmp = Bitmap.createBitmap((s.w * scale).roundToInt(), (s.h * scale).roundToInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(scale, scale)
        val rnd = Random(s.seed)
        val clip = Path().apply {
            if (s.round) addCircle(s.w / 2, s.h / 2, min(s.w, s.h) / 2, Path.Direction.CW)
            else addRoundRect(RectF(0f, 0f, s.w, s.h), s.corner, s.corner, Path.Direction.CW)
        }
        c.clipPath(clip)
        background(c, s, rnd)
        if (s.round) face(c, s, rnd) else panel(c, s, rnd)
        grain(c, s, rnd)
        return bmp
    }

    // ---------- surfaces ----------

    private fun background(c: Canvas, s: ArtSpec, rnd: Random) {
        val cx = s.w / 2; val cy = s.h / 2; val R = max(s.w, s.h) * .75f
        val p = s.pal
        c.drawRect(0f, 0f, s.w, s.h, Paint().apply {
            shader = RadialGradient(cx, cy * .8f, R, intArrayOf(Colors.mix(p.base, 0xFFFFFFFF.toInt(), .08f), p.base, p.base2), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
        })
        val light = Colors.mix(p.base, 0xFFFFFFFF.toInt(), .5f)
        val dark = Colors.mix(p.base, 0xFF000000.toInt(), .6f)
        when (s.texture) {
            Texture.SUNBURST -> {
                val cols = IntArray(17) { i -> when (i % 4) { 0 -> Colors.alpha(light, .55f); 2 -> Colors.alpha(dark, .55f); else -> 0 } }
                c.drawRect(0f, 0f, s.w, s.h, Paint().apply { shader = SweepGradient(cx, cy, cols, null).also { sh -> sh.setLocalMatrix(Matrix().apply { setRotate(-35f, cx, cy) }) } })
                val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = .45f }
                for (i in 0 until 720) {
                    val a = Math.toRadians(i / 2.0)
                    line.color = Colors.alpha(if (i % 2 == 0) light else dark, .09f)
                    c.drawLine(cx, cy, cx + cos(a).toFloat() * R, cy + sin(a).toFloat() * R, line)
                }
            }
            Texture.GUILLOCHE -> {
                val k = 6 + rnd.nextInt(10); val amp = 2f + rnd.nextFloat() * 4f
                val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = .7f }
                var r = 10f; var i = 0
                while (r < R) {
                    val path = Path()
                    for (d in 0..360 step 2) {
                        val a = Math.toRadians(d.toDouble())
                        val rr = r + amp * sin(k * a + i * .35).toFloat()
                        val x = cx + cos(a).toFloat() * rr; val y = cy + sin(a).toFloat() * rr
                        if (d == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    st.color = Colors.alpha(if (i % 2 == 0) light else dark, .22f)
                    c.drawPath(path, st); r += 3.2f; i++
                }
            }
            Texture.CLOUS -> {
                val step = 9f
                val hi = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.1f; color = Colors.alpha(light, .28f) }
                val lo = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.1f; color = Colors.alpha(dark, .5f) }
                var x = -s.h
                while (x < s.w + s.h) {
                    c.drawLine(x, 0f, x + s.h, s.h, hi); c.drawLine(x + 1.2f, 0f, x + 1.2f + s.h, s.h, lo)
                    c.drawLine(x, s.h, x + s.h, 0f, hi); c.drawLine(x + 1.2f, s.h, x + 1.2f + s.h, 0f, lo)
                    x += step
                }
            }
            Texture.CARBON -> {
                val t = 6
                val tile = Bitmap.createBitmap(t * 2, t * 2, Bitmap.Config.ARGB_8888)
                val tc = Canvas(tile)
                val a = Colors.mix(p.base, 0xFF000000.toInt(), .3f); val b = Colors.mix(p.base, 0xFFFFFFFF.toInt(), .12f)
                fun cell(x: Int, y: Int, vertical: Boolean) = tc.drawRect(x.toFloat(), y.toFloat(), (x + t).toFloat(), (y + t).toFloat(), Paint().apply {
                    shader = if (vertical) LinearGradient(0f, y.toFloat(), 0f, (y + t).toFloat(), a, b, Shader.TileMode.MIRROR)
                    else LinearGradient(x.toFloat(), 0f, (x + t).toFloat(), 0f, a, b, Shader.TileMode.MIRROR)
                })
                cell(0, 0, true); cell(t, 0, false); cell(0, t, false); cell(t, t, true)
                c.drawRect(0f, 0f, s.w, s.h, Paint().apply {
                    shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).also { sh -> sh.setLocalMatrix(Matrix().apply { setRotate(45f) }) }
                })
                c.drawRect(0f, 0f, s.w, s.h, Paint().apply { shader = LinearGradient(0f, 0f, s.w, s.h, Colors.alpha(0xFFFFFFFF.toInt(), .1f), 0, Shader.TileMode.CLAMP) })
            }
            Texture.BRUSHED -> {
                val ln = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = .6f }
                repeat(2600) {
                    val y = rnd.nextFloat() * s.h; val x = rnd.nextFloat() * s.w; val len = 40f + rnd.nextFloat() * s.w * .6f
                    ln.color = Colors.alpha(if (rnd.nextBoolean()) light else dark, .05f + rnd.nextFloat() * .08f)
                    c.drawLine(x - len / 2, y, x + len / 2, y, ln)
                }
                c.drawRect(0f, 0f, s.w, s.h, Paint().apply {
                    shader = LinearGradient(0f, 0f, s.w, s.h, intArrayOf(0, Colors.alpha(light, .22f), 0, Colors.alpha(light, .12f), 0), null, Shader.TileMode.CLAMP)
                })
            }
            Texture.HEX -> {
                val r = 13f; val hw = r * 1.732f
                val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Colors.alpha(light, .16f) }
                val lit = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Colors.alpha(p.accent, .12f) }
                var row = 0; var y = 0f
                while (y < s.h + r) {
                    var x = if (row % 2 == 0) 0f else hw / 2
                    while (x < s.w + hw) {
                        val hex = Path()
                        for (k in 0..5) {
                            val a = Math.toRadians(60.0 * k + 30)
                            val px = x + cos(a).toFloat() * r * .94f; val py = y + sin(a).toFloat() * r * .94f
                            if (k == 0) hex.moveTo(px, py) else hex.lineTo(px, py)
                        }
                        hex.close()
                        if (rnd.nextFloat() < .06f) c.drawPath(hex, lit)
                        c.drawPath(hex, st); x += hw
                    }
                    y += r * 1.5f; row++
                }
            }
            Texture.GRID -> {
                val g = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = .6f; color = Colors.alpha(p.accent, .1f) }
                var v = 0f
                while (v < max(s.w, s.h)) { c.drawLine(v, 0f, v, s.h, g); c.drawLine(0f, v, s.w, v, g); v += 18f }
                val scan = Paint().apply { color = Colors.alpha(0xFF000000.toInt(), .18f) }
                var y = 0f
                while (y < s.h) { c.drawRect(0f, y, s.w, y + 1f, scan); y += 3f }
                c.drawRect(0f, 0f, s.w, s.h, Paint().apply {
                    shader = RadialGradient(cx, cy, R * .7f, intArrayOf(Colors.alpha(p.accent, .16f), 0), null, Shader.TileMode.CLAMP)
                })
            }
            Texture.WAVES -> {
                val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = .9f }
                val ph = DoubleArray(4) { rnd.nextDouble() * 6.28 }
                val ox = cx + (rnd.nextFloat() - .5f) * s.w * .5f; val oy = cy + (rnd.nextFloat() - .5f) * s.h * .5f
                for (i in 1..44) {
                    val r = i * R / 40
                    val path = Path()
                    for (d in 0..360 step 3) {
                        val a = Math.toRadians(d.toDouble())
                        val rr = r * (1 + .18 * sin(2 * a + ph[0] + i * .08) + .1 * sin(3 * a + ph[1]) + .06 * sin(5 * a + ph[2] - i * .05)).toFloat()
                        val x = ox + cos(a).toFloat() * rr; val y = oy + sin(a).toFloat() * rr
                        if (d == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    st.color = Colors.alpha(if (i % 5 == 0) p.accent else light, if (i % 5 == 0) .35f else .12f)
                    c.drawPath(path, st)
                }
            }
            Texture.AURORA -> {
                val cols = listOf(p.accent, p.accent2, Colors.mix(p.accent, p.accent2, .5f), Colors.mix(p.base, 0xFFFFFFFF.toInt(), .25f))
                repeat(6) { i ->
                    val x = rnd.nextFloat() * s.w; val y = rnd.nextFloat() * s.h; val r = max(s.w, s.h) * (.35f + rnd.nextFloat() * .4f)
                    c.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(x, y, r, Colors.alpha(cols[i % cols.size], .55f), 0, Shader.TileMode.CLAMP)
                        blendMode = BlendMode.SCREEN
                    })
                }
            }
            Texture.PHOTO -> s.photo?.let { ph ->
                val src = centerCrop(ph.width, ph.height, s.w, s.h)
                c.drawBitmap(ph, src, RectF(0f, 0f, s.w, s.h), Paint(Paint.FILTER_BITMAP_FLAG))
                if (s.photoTint > 0) {
                    c.drawRect(0f, 0f, s.w, s.h, Paint().apply { color = Colors.alpha(p.base, s.photoTint); blendMode = BlendMode.MULTIPLY })
                    c.drawRect(0f, 0f, s.w, s.h, Paint().apply { color = Colors.alpha(p.base, s.photoTint * .5f); blendMode = BlendMode.OVERLAY })
                }
            }
        }
        // Vignette: depth at the edges, keeps the middle readable.
        c.drawRect(0f, 0f, s.w, s.h, Paint().apply {
            shader = RadialGradient(cx, cy, R * .82f, intArrayOf(0, 0, Colors.alpha(0xFF000000.toInt(), .55f)), floatArrayOf(0f, .62f, 1f), Shader.TileMode.CLAMP)
        })
    }

    private fun grain(c: Canvas, s: ArtSpec, rnd: Random) {
        val p = Paint()
        repeat((s.w * s.h / 40).toInt()) {
            p.color = if (rnd.nextBoolean()) 0x0CFFFFFF else 0x10000000
            val x = rnd.nextFloat() * s.w; val y = rnd.nextFloat() * s.h
            c.drawRect(x, y, x + .7f, y + .7f, p)
        }
    }

    // ---------- watch face furniture ----------

    private fun face(c: Canvas, s: ArtSpec, rnd: Random) {
        val p = s.pal
        val cx = s.w / 2; val cy = s.h / 2
        var R = s.w / 2
        if (s.bezel != Bezel.NONE) { bezel(c, s, cx, cy, R); R *= .82f }
        // Inner rehaut shadow where the dial meets the case.
        c.drawCircle(cx, cy, R, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 10f
            shader = RadialGradient(cx, cy, R, intArrayOf(0, 0, Colors.alpha(0xFF000000.toInt(), .6f)), floatArrayOf(0f, .94f, 1f), Shader.TileMode.CLAMP)
        })
        when (s.track) {
            Track.RAILWAY -> {
                val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = .9f; color = Colors.alpha(p.ink, .7f) }
                c.drawCircle(cx, cy, R * .955f, st); c.drawCircle(cx, cy, R * .915f, st)
                ticks(c, cx, cy, R * .955f, R * .04f, 60, .9f, Colors.alpha(p.ink, .7f))
            }
            Track.MINUTES -> {
                ticks(c, cx, cy, R * .965f, R * .035f, 60, 1.2f, Colors.alpha(p.ink, .65f))
                ticks(c, cx, cy, R * .965f, R * .07f, 12, 2.4f, p.ink)
            }
            Track.FINE -> {
                ticks(c, cx, cy, R * .97f, R * .022f, 240, .5f, Colors.alpha(p.ink, .45f))
                ticks(c, cx, cy, R * .97f, R * .045f, 60, 1f, Colors.alpha(p.ink, .75f))
                c.drawCircle(cx, cy, R * .925f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = .6f; color = Colors.alpha(p.ink, .5f) })
            }
            Track.NONE -> {}
        }
        s.meters.forEach { meter(c, s, it) }
        s.subdials.forEach { subdial(c, s, it) }
        s.windows.forEach { window(c, s, it) }
        indices(c, s, cx, cy, R * .89f)
        if (s.brand.isNotEmpty()) text(c, s.brand, cx, cy - R * .4f, R * .085f, s.font, p.ink, .18f)
        if (s.sub.isNotEmpty()) text(c, s.sub, cx, cy - R * .4f + R * .1f, R * .042f, s.font2, p.accent, .3f)
        s.labels.forEach { (t, x, y) -> text(c, t, x, y, 13f, s.font2, Colors.alpha(p.ink, .8f), .2f) }
        // Crystal reflection.
        c.drawCircle(cx, cy, s.w / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s.w * .6f, s.h * .6f, intArrayOf(0x14FFFFFF, 0x05FFFFFF, 0), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
        })
    }

    private fun ticks(c: Canvas, cx: Float, cy: Float, outer: Float, len: Float, count: Int, width: Float, color: Int, skipEvery: Int = 0) {
        val ln = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = width; this.color = color }
        for (i in 0 until count) {
            if (skipEvery > 0 && i % skipEvery == 0) continue
            val a = Math.toRadians(360.0 * i / count - 90)
            val cs = cos(a).toFloat(); val sn = sin(a).toFloat()
            c.drawLine(cx + cs * outer, cy + sn * outer, cx + cs * (outer - len), cy + sn * (outer - len), ln)
        }
    }

    private fun bezel(c: Canvas, s: ArtSpec, cx: Float, cy: Float, R: Float) {
        val p = s.pal
        val inner = R * .82f
        val ring = Path().apply { addCircle(cx, cy, R, Path.Direction.CW); addCircle(cx, cy, inner, Path.Direction.CCW) }
        val insert = if (s.bezel == Bezel.SEGMENT) 0xFF05070A.toInt() else Colors.mix(p.base2, 0xFF000000.toInt(), .45f)
        c.drawPath(ring, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = SweepGradient(cx, cy, intArrayOf(insert, Colors.mix(insert, 0xFFFFFFFF.toInt(), .12f), insert, Colors.mix(insert, 0xFFFFFFFF.toInt(), .08f), insert), null)
        })
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Colors.alpha(p.metal, .55f) }
        c.drawCircle(cx, cy, inner, edge); c.drawCircle(cx, cy, R - 1f, edge)
        val mid = (R + inner) / 2; val size = (R - inner) * .42f
        when (s.bezel) {
            Bezel.DIVER -> {
                c.drawArc(RectF(cx - mid, cy - mid, cx + mid, cy + mid), -90f, 90f, false, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = R - inner - 4; color = Colors.alpha(p.accent, .32f)
                })
                ticks(c, cx, cy, R - 4, (R - inner) * .28f, 60, 1.4f, p.metal, 5)
                for (m in 10..50 step 10) radialText(c, "$m", cx, cy, mid, m * 6f, size, s.font2, p.metal)
                for (m in listOf(5, 15, 25, 35, 45, 55)) dot(c, cx, cy, mid, m * 6f, 2.2f, p.metal)
                triangle(c, cx, cy, R - 6, inner + 6, 0f, p.lume, p.metal)
            }
            Bezel.TACHY -> {
                ticks(c, cx, cy, inner + (R - inner) * .32f, (R - inner) * .22f, 60, 1f, Colors.alpha(p.metal, .7f))
                radialText(c, "TACHYMETRE", cx, cy, mid + size * .15f, 20f, size * .7f, s.font2, p.accent)
                for (v in listOf(500, 400, 300, 250, 200, 175, 150, 125, 110, 100, 90, 80, 70, 60)) {
                    radialText(c, "$v", cx, cy, mid + size * .15f, 3600f / v * 6f, size * .78f, s.font2, p.metal)
                }
            }
            Bezel.COMPASS -> {
                ticks(c, cx, cy, R - 4, (R - inner) * .2f, 72, 1f, Colors.alpha(p.metal, .6f))
                listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f).forEach { (t, a) ->
                    radialText(c, t, cx, cy, mid, a, size * 1.15f, s.font2, if (t == "N") p.accent else p.metal)
                }
                for (a in 30..330 step 30) if (a % 90 != 0) radialText(c, "$a", cx, cy, mid, a.toFloat(), size * .72f, s.font2, Colors.alpha(p.metal, .8f))
            }
            Bezel.HOURS24 -> {
                c.drawArc(RectF(cx - mid, cy - mid, cx + mid, cy + mid), 90f, 180f, false, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = R - inner - 4; color = Colors.alpha(p.accent, .45f)
                })
                for (hr in 2..24 step 2) radialText(c, "$hr", cx, cy, mid, hr * 15f, size * .9f, s.font2, p.metal)
                for (hr in 1..23 step 2) dot(c, cx, cy, mid, hr * 15f, 2f, p.metal)
            }
            Bezel.SEGMENT -> {
                val seg = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = (R - inner) * .34f }
                val glow = Paint(seg).apply { maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL) }
                for (i in 0 until 60) {
                    val lit = i < 42
                    seg.color = if (lit) p.accent else Colors.alpha(p.accent, .14f)
                    glow.color = Colors.alpha(p.accent, if (lit) .5f else 0f)
                    val rect = RectF(cx - mid, cy - mid, cx + mid, cy + mid)
                    if (lit) c.drawArc(rect, i * 6f - 90f + .8f, 4.4f, false, glow)
                    c.drawArc(rect, i * 6f - 90f + .8f, 4.4f, false, seg)
                }
            }
            Bezel.NONE -> {}
        }
    }

    private fun indices(c: Canvas, s: ArtSpec, cx: Float, cy: Float, r: Float) {
        val p = s.pal
        for (hr in 0 until 12) {
            if (hr in s.skipHours) continue
            val deg = hr * 30f
            val a = Math.toRadians(deg - 90.0)
            val cs = cos(a).toFloat(); val sn = sin(a).toFloat()
            when (s.indices) {
                Indices.BATON -> {
                    val len = r * .15f; val w = if (hr % 3 == 0) 9f else 6.5f
                    val double = hr == 0
                    for (o in if (double) listOf(-6.5f, 6.5f) else listOf(0f)) applied(c, cx, cy, r, len, w, deg, o, p)
                }
                Indices.ARABIC -> {
                    val label = if (hr == 0) "12" else "$hr"
                    val big = hr % 3 == 0
                    shadowText(c, label, cx + cs * (r - r * .1f), cy + sn * (r - r * .1f), r * (if (big) .19f else .13f), s.font, p.metal)
                }
                Indices.ROMAN -> {
                    val label = listOf("XII", "I", "II", "III", "IIII", "V", "VI", "VII", "VIII", "IX", "X", "XI")[hr]
                    c.save(); c.rotate(deg, cx, cy)
                    shadowText(c, label, cx, cy - r + r * .1f, r * .13f, s.font, p.ink)
                    c.restore()
                }
                Indices.DOTS -> {
                    val rr = if (hr % 3 == 0) 9f else 6.5f
                    lume(c, cx + cs * (r - rr), cy + sn * (r - rr), rr, p)
                }
                Indices.DIVER -> when {
                    hr == 0 -> triangle(c, cx, cy, r, r - r * .2f, 0f, p.lume, p.metal)
                    hr % 3 == 0 -> applied(c, cx, cy, r, r * .19f, 14f, deg, 0f, p, lumeFill = true)
                    else -> lume(c, cx + cs * (r - 11f), cy + sn * (r - 11f), 10f, p)
                }
                Indices.HUD -> {
                    val ln = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = if (hr % 3 == 0) 5f else 2.5f; color = if (hr % 3 == 0) p.accent else Colors.alpha(p.ink, .7f) }
                    val glow = Paint(ln).apply { maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL) }
                    val len = if (hr % 3 == 0) r * .14f else r * .07f
                    c.drawLine(cx + cs * r, cy + sn * r, cx + cs * (r - len), cy + sn * (r - len), glow)
                    c.drawLine(cx + cs * r, cy + sn * r, cx + cs * (r - len), cy + sn * (r - len), ln)
                    if (hr % 3 == 0) text(c, "%02d".format(hr * 5), cx + cs * (r - len - 16), cy + sn * (r - len - 16), 14f, s.font2, Colors.alpha(p.accent, .9f), .1f)
                }
                Indices.NONE -> {}
            }
        }
    }

    private fun applied(c: Canvas, cx: Float, cy: Float, r: Float, len: Float, w: Float, deg: Float, offset: Float, p: Pal, lumeFill: Boolean = false) {
        c.save(); c.rotate(deg, cx, cy)
        val rect = RectF(cx + offset - w / 2, cy - r, cx + offset + w / 2, cy - r + len)
        c.drawRoundRect(RectF(rect).apply { offset(1.6f, 2.4f) }, 1.5f, 1.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x88000000.toInt(); maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
        })
        c.drawRoundRect(rect, 1.5f, 1.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(rect.left, 0f, rect.right, 0f,
                intArrayOf(Colors.mix(p.metal, 0xFFFFFFFF.toInt(), .6f), p.metal, Colors.mix(p.metal, 0xFF000000.toInt(), .5f)), floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
        })
        if (lumeFill || w >= 8f) c.drawRoundRect(RectF(rect).apply { inset(w * .28f, len * .1f) }, 1f, 1f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.lume })
        c.restore()
    }

    private fun lume(c: Canvas, x: Float, y: Float, r: Float, p: Pal) {
        c.drawCircle(x + 1.5f, y + 2.2f, r + 1.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88000000.toInt(); maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL) })
        c.drawCircle(x, y, r + 1.6f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(x - r, y - r, x + r, y + r, Colors.mix(p.metal, 0xFFFFFFFF.toInt(), .5f), Colors.mix(p.metal, 0xFF000000.toInt(), .4f), Shader.TileMode.CLAMP)
        })
        c.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.lume })
    }

    private fun triangle(c: Canvas, cx: Float, cy: Float, outer: Float, inner: Float, deg: Float, fill: Int, rim: Int) {
        c.save(); c.rotate(deg, cx, cy)
        val w = (outer - inner) * .62f
        val path = Path().apply { moveTo(cx - w, cy - outer); lineTo(cx + w, cy - outer); lineTo(cx, cy - inner); close() }
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88000000.toInt(); maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL) })
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f; color = rim })
        c.restore()
    }

    private fun dot(c: Canvas, cx: Float, cy: Float, r: Float, deg: Float, size: Float, color: Int) {
        val a = Math.toRadians(deg - 90.0)
        c.drawCircle(cx + cos(a).toFloat() * r, cy + sin(a).toFloat() * r, size, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun subdial(c: Canvas, s: ArtSpec, d: Subdial) {
        val p = s.pal
        val sunk = Colors.mix(p.base2, 0xFF000000.toInt(), .25f)
        c.drawCircle(d.x, d.y, d.r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(d.x, d.y - d.r * .3f, d.r * 1.2f, Colors.mix(sunk, 0xFFFFFFFF.toInt(), .08f), sunk, Shader.TileMode.CLAMP)
        })
        val az = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = .6f }
        var r = 3f; var i = 0
        while (r < d.r) { az.color = if (i++ % 2 == 0) 0x16FFFFFF else 0x22000000; c.drawCircle(d.x, d.y, r, az); r += 1.6f }
        c.drawCircle(d.x, d.y, d.r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f
            shader = LinearGradient(d.x, d.y - d.r, d.x, d.y + d.r, Colors.alpha(0xFF000000.toInt(), .7f), Colors.alpha(p.metal, .6f), Shader.TileMode.CLAMP)
        })
        val n = if (d.source == DataSource.SECONDS) 60 else 40
        for (k in 0..n) {
            if (d.sweep >= 360f && k == n) break
            val deg = d.start + d.sweep * k / n
            val a = Math.toRadians(deg - 90.0)
            val major = k % (n / 4) == 0
            val l = if (major) d.r * .2f else d.r * .1f
            val o = d.r * .9f
            c.drawLine(d.x + cos(a).toFloat() * o, d.y + sin(a).toFloat() * o, d.x + cos(a).toFloat() * (o - l), d.y + sin(a).toFloat() * (o - l),
                Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = if (major) 1.6f else .7f; color = Colors.alpha(p.ink, if (major) .9f else .6f) })
        }
        val nums = when (d.source) {
            DataSource.SECONDS -> listOf("60", "15", "30", "45")
            DataSource.BATTERY -> listOf("0", "", "50", "", "100")
            DataSource.HEART -> listOf("0", "", "100", "", "200")
            DataSource.STEPS -> listOf("0", "", "50", "", "100")
            DataSource.DAY -> listOf("1", "", "15", "", "31")
        }
        nums.forEachIndexed { k, t ->
            if (t.isEmpty()) return@forEachIndexed
            val frac = if (d.source == DataSource.SECONDS) k / 4f else k / 4f
            val a = Math.toRadians(d.start + d.sweep * frac - 90.0)
            text(c, t, d.x + cos(a).toFloat() * d.r * .55f, d.y + sin(a).toFloat() * d.r * .55f, d.r * .2f, s.font2, Colors.alpha(p.ink, .85f), 0f)
        }
        val label = when (d.source) { DataSource.SECONDS -> ""; DataSource.BATTERY -> "PWR"; DataSource.HEART -> "BPM"; DataSource.STEPS -> "STEP"; DataSource.DAY -> "DATE" }
        if (label.isNotEmpty()) text(c, label, d.x, d.y + d.r * .5f, d.r * .17f, s.font2, p.accent, .2f)
    }

    private fun meter(c: Canvas, s: ArtSpec, m: Meter) {
        val p = s.pal
        val rect = RectF(m.x - m.r, m.y - m.r, m.x + m.r, m.y + m.r)
        c.drawArc(rect, m.start - 90f, m.sweep, false, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = m.thick; strokeCap = Paint.Cap.ROUND; color = Colors.alpha(p.ink, .13f)
        })
        val tk = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f; color = Colors.alpha(p.ink, .35f) }
        for (k in 0..10) {
            val a = Math.toRadians(m.start + m.sweep * k / 10 - 90.0)
            val o = m.r + m.thick / 2 + 3; val l = if (k % 5 == 0) 7f else 3.5f
            c.drawLine(m.x + cos(a).toFloat() * o, m.y + sin(a).toFloat() * o, m.x + cos(a).toFloat() * (o + l), m.y + sin(a).toFloat() * (o + l), tk)
        }
        val a = Math.toRadians(m.start - 90.0 - 9)
        val ix = m.x + cos(a).toFloat() * m.r; val iy = m.y + sin(a).toFloat() * m.r
        m.icon?.let { icon(c, it, ix, iy, m.thick * 1.5f, p.accent) }
        if (m.label.isNotEmpty()) {
            val b = Math.toRadians(m.start + m.sweep - 90.0 + 12)
            text(c, m.label, m.x + cos(b).toFloat() * m.r, m.y + sin(b).toFloat() * m.r, 11f, s.font2, Colors.alpha(p.ink, .75f), .15f)
        }
    }

    private fun window(c: Canvas, s: ArtSpec, r: RectF) {
        val p = s.pal
        c.drawRoundRect(RectF(r).apply { inset(-3f, -3f) }, 4f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(r.left, r.top, r.right, r.bottom, Colors.mix(p.metal, 0xFFFFFFFF.toInt(), .5f), Colors.mix(p.metal, 0xFF000000.toInt(), .4f), Shader.TileMode.CLAMP)
        })
        c.drawRoundRect(r, 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.window })
        c.drawRoundRect(r, 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, r.top, 0f, r.top + r.height() * .4f, 0x66000000, 0, Shader.TileMode.CLAMP)
        })
    }

    // ---------- widget panels ----------

    private fun panel(c: Canvas, s: ArtSpec, rnd: Random) {
        val p = s.pal
        when (s.frame) {
            "luxe" -> {
                val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.2f; color = Colors.alpha(p.accent, .9f) }
                val i1 = s.corner * .45f
                c.drawRoundRect(RectF(i1, i1, s.w - i1, s.h - i1), s.corner * .6f, s.corner * .6f, st)
                st.strokeWidth = .9f; st.color = Colors.alpha(p.accent, .6f)
                val i2 = i1 + 7
                c.drawRoundRect(RectF(i2, i2, s.w - i2, s.h - i2), s.corner * .5f, s.corner * .5f, st)
            }
            "sport" -> {
                val stripe = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Colors.alpha(p.accent, .9f) }
                val x0 = s.w * .78f
                for (k in 0..2) {
                    val x = x0 + k * s.h * .14f
                    c.drawPath(Path().apply { moveTo(x, 0f); lineTo(x + s.h * .08f, 0f); lineTo(x + s.h * .08f - s.h * .5f, s.h); lineTo(x - s.h * .5f, s.h); close() },
                        stripe.apply { alpha = 230 - k * 70 })
                }
                ticksLine(c, s.corner, s.h - s.corner * .55f, s.w * .6f, 40, p.ink)
            }
            "cyber" -> {
                val br = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = p.accent }
                val glow = Paint(br).apply { maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL) }
                val m = s.corner * .5f; val l = min(s.w, s.h) * .14f
                for (pp in listOf(glow, br)) {
                    c.drawPath(Path().apply { moveTo(m, m + l); lineTo(m, m); lineTo(m + l, m) }, pp)
                    c.drawPath(Path().apply { moveTo(s.w - m - l, m); lineTo(s.w - m, m); lineTo(s.w - m, m + l) }, pp)
                    c.drawPath(Path().apply { moveTo(m, s.h - m - l); lineTo(m, s.h - m); lineTo(m + l, s.h - m) }, pp)
                    c.drawPath(Path().apply { moveTo(s.w - m - l, s.h - m); lineTo(s.w - m, s.h - m); lineTo(s.w - m, s.h - m - l) }, pp)
                }
                val bar = RectF(s.w * .62f, s.h - m - 14, s.w - m - 10, s.h - m - 6)
                c.drawRect(bar, Paint().apply { color = Colors.alpha(p.accent, .2f) })
                c.drawRect(RectF(bar).apply { right = left + width() * (.4f + rnd.nextFloat() * .5f) }, Paint().apply { color = p.accent })
            }
            "art" -> c.drawRect(0f, 0f, s.w, s.h, Paint().apply {
                shader = LinearGradient(0f, 0f, s.w * .75f, 0f, intArrayOf(Colors.alpha(0xFF000000.toInt(), .7f), Colors.alpha(0xFF000000.toInt(), .25f), 0), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
            })
        }
        if (s.brand.isNotEmpty()) c.drawText(s.brand, s.brandX, s.brandY, textPaint(s.brandSize, s.font, p.ink, .2f).apply { textAlign = Paint.Align.LEFT })
        s.labels.forEach { (t, x, y) -> text(c, t, x, y, min(s.w, s.h) * .055f, s.font2, Colors.alpha(p.accent, .9f), .2f, left = true) }
    }

    private fun ticksLine(c: Canvas, x: Float, y: Float, w: Float, n: Int, color: Int) {
        val ln = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.4f }
        for (k in 0..n) {
            ln.color = Colors.alpha(color, if (k % 5 == 0) .8f else .4f)
            val xx = x + w * k / n
            c.drawLine(xx, y, xx, y - if (k % 5 == 0) 14f else 7f, ln)
        }
    }

    // ---------- type & icons ----------

    private fun textPaint(size: Float, font: Typeface, color: Int, spacing: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; typeface = font; this.color = color; letterSpacing = spacing; textAlign = Paint.Align.CENTER
    }

    /** Text centered on (x, y), or starting at x when [left]. */
    private fun text(c: Canvas, t: String, x: Float, y: Float, size: Float, font: Typeface, color: Int, spacing: Float, left: Boolean = false) {
        val p = textPaint(size, font, color, spacing)
        if (left) p.textAlign = Paint.Align.LEFT
        val fm = p.fontMetrics
        c.drawText(t, x, y - (fm.ascent + fm.descent) / 2, p)
    }

    private fun shadowText(c: Canvas, t: String, x: Float, y: Float, size: Float, font: Typeface, color: Int) {
        val p = textPaint(size, font, color, 0f)
        val fm = p.fontMetrics
        val by = y - (fm.ascent + fm.descent) / 2
        c.drawText(t, x + 1.4f, by + 2f, Paint(p).apply { this.color = 0x88000000.toInt(); maskFilter = BlurMaskFilter(2.5f, BlurMaskFilter.Blur.NORMAL) })
        c.drawText(t, x, by, Paint(p).apply {
            shader = LinearGradient(0f, by + fm.ascent, 0f, by, Colors.mix(color, 0xFFFFFFFF.toInt(), .35f), Colors.mix(color, 0xFF000000.toInt(), .2f), Shader.TileMode.CLAMP)
        })
    }

    /** Text along the ring, upright at the top half and flipped at the bottom so it never reads upside down. */
    private fun radialText(c: Canvas, t: String, cx: Float, cy: Float, r: Float, deg: Float, size: Float, font: Typeface, color: Int) {
        val p = textPaint(size, font, color, .05f)
        val fm = p.fontMetrics
        val norm = ((deg % 360) + 360) % 360
        c.save()
        if (norm > 90 && norm < 270) {
            c.rotate(norm + 180, cx, cy)
            c.drawText(t, cx, cy + r - (fm.ascent + fm.descent) / 2, p)
        } else {
            c.rotate(norm, cx, cy)
            c.drawText(t, cx, cy - r - (fm.ascent + fm.descent) / 2, p)
        }
        c.restore()
    }

    private fun icon(c: Canvas, name: String, x: Float, y: Float, size: Float, color: Int) {
        val paths = IconLibrary.paths(name)
        if (paths.isEmpty()) return
        c.save()
        c.translate(x - size / 2, y - size / 2)
        c.scale(size / IconLibrary.VIEWPORT, size / IconLibrary.VIEWPORT)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        paths.forEach { c.drawPath(it.asAndroidPath(), p) }
        c.restore()
    }

    fun centerCrop(sw: Int, sh: Int, w: Float, h: Float): Rect {
        val s = max(w / sw, h / sh)
        val cw = (w / s).roundToInt(); val ch = (h / s).roundToInt()
        val x = (sw - cw) / 2; val y = (sh - ch) / 2
        return Rect(x, y, x + cw, y + ch)
    }
}
