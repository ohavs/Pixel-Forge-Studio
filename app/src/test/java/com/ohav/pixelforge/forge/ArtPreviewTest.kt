package com.ohav.pixelforge.forge

import android.graphics.*
import com.ohav.pixelforge.editor.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtPreviewTest {
    private val now = java.time.LocalDateTime.of(2026, 10, 28, 10, 8, 32)

    private fun overlay(c: Canvas, layers: List<Layer>, scale: Float) {
        c.save(); c.scale(scale, scale)
        for (l in layers) when (l.kind) {
            LayerKind.HANDS -> {
                val angles = listOf((10 + 8 / 60f) * 30, 8.5f * 6, 32 * 6f)
                for (part in 1..(if (l.extra) 3 else 2)) {
                    val h = l.handSpec(part); val b = HandArt.bitmap(l, part, scale)
                    c.save(); c.rotate(angles[part - 1], l.x, l.y)
                    c.drawBitmap(b, null, RectF(l.x - h.w / 2, l.y - h.len, l.x + h.w / 2, l.y - h.len + h.h), Paint(Paint.FILTER_BITMAP_FLAG)); c.restore()
                }
            }
            LayerKind.GAUGE -> {
                val d = DataSource.of(l.text); val sw = l.height * d.sampleFraction(now)
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = l.color.toInt(); strokeWidth = l.thickness; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
                if (l.extra) { c.save(); c.rotate(l.angle + sw, l.x, l.y); c.drawLine(l.x, l.y + l.thickness * 1.6f, l.x, l.y - l.size, p); c.restore(); c.drawCircle(l.x, l.y, l.thickness, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = l.color2.toInt() }) }
                else c.drawArc(RectF(l.x - l.size, l.y - l.size, l.x + l.size, l.y + l.size), l.angle - 90, sw, false, p)
            }
            LayerKind.TIME, LayerKind.DATE, LayerKind.DATA -> {
                val t = when (l.kind) { LayerKind.TIME -> if (l.extra) "10:08:32" else "10:08"; LayerKind.DATE -> "Wed 28"; else -> DataSource.of(l.text).let { "${it.sample(now).toInt()}${it.unit}" } }
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = l.color.toInt(); textSize = l.size; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, listOf(100, 300, 400, 500, 700)[l.weight.coerceIn(0, 4)], false) }
                c.drawText(t, l.x, l.y - (p.ascent() + p.descent()) / 2, p)
            }
            else -> {}
        }
        c.restore()
    }

    @Test fun render() {
        val out = File(System.getProperty("artOut") ?: "build/art").apply { mkdirs() }
        val serif = Typeface.SERIF; val sans = Typeface.DEFAULT
        for (st in Style.entries) for (v in 0 until 6) {
            val rnd = Random(77 * 31 + v + st.ordinal * 1000)
            val pal = st.palettes[v % st.palettes.size]
            val tex = st.textures[rnd.nextInt(st.textures.size)]
            val layers = mutableListOf<Layer>()
            val rec = Recipe(style = st, complexity = 1 + v % 3)
            val spec = Forge.faceLayout(rec, pal, rnd, if (st == Style.LUXURY) serif else Typeface.DEFAULT_BOLD, sans, tex, null, 0f, v, layers)
            val bmp = DialArt.render(spec, 2f)
            overlay(Canvas(bmp), layers, 2f)
            File(out, "face_${st}_$v.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val sizes = listOf(800f to 400f, 400f to 400f, 800f to 200f)
        for (st in Style.entries) sizes.forEachIndexed { i, (w, h) ->
            val rnd = Random(i + st.ordinal * 50)
            val pal = st.palettes[i % st.palettes.size]
            val layers = mutableListOf<Layer>()
            val doc = FaceDoc(w = w, h = h, round = false, corner = 64f)
            val spec = Forge.widgetLayout(Recipe(style = st), pal, rnd, serif, sans, st.textures[i % st.textures.size], null, 0f, i, doc, layers)
            val sc = 900f / maxOf(w, h)
            val bmp = DialArt.render(spec, sc); overlay(Canvas(bmp), layers, sc)
            File(out, "widget_${st}_$i.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
