package com.ohav.pixelforge.forge

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import com.ohav.pixelforge.editor.Layer
import com.ohav.pixelforge.editor.handSpec
import kotlin.math.roundToInt

/**
 * Picture hands: metal with a light and a dark facet, lume inlays and a soft shadow, drawn at
 * any scale. The bitmap is [HandSpec.w] x [HandSpec.h] times the scale, pivot at (w / 2, len).
 * Hour/minute use the layer color as metal; the second hand and the cap use color2.
 */
object HandArt {
    private val cache = LruCache<String, Bitmap>(24)

    fun bitmap(l: Layer, part: Int, scale: Float): Bitmap {
        val key = "${l.text}|$part|${l.size}|${l.thickness}|${l.color}|${l.color2}|${l.extra}|$scale"
        cache.get(key)?.let { return it }
        val s = l.handSpec(part)
        val w = (s.w * scale).roundToInt().coerceAtLeast(2)
        val h = (s.h * scale).roundToInt().coerceAtLeast(2)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val metal = l.color.toInt()
        val accent = (if (l.color2 != 0L) l.color2 else l.color).toInt()
        val top = part == 3 || (part == 2 && !l.extra)
        draw(c, l.text, part, w.toFloat(), s.len * scale, s.tail * scale, metal, accent, top)
        cache.put(key, bmp)
        return bmp
    }

    private const val LUME = 0xFFEAF6E4.toInt()

    private fun draw(c: Canvas, style: String, part: Int, w: Float, len: Float, tail: Float, metal: Int, accent: Int, cap: Boolean) {
        val cx = w / 2
        val bw = w * .62f // body width; the rest is room for the shadow
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x99000000.toInt(); maskFilter = BlurMaskFilter(w * .09f, BlurMaskFilter.Blur.NORMAL)
        }
        fun metalPaint(col: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx - bw / 2, 0f, cx + bw / 2, 0f,
                intArrayOf(Colors.mix(col, 0xFFFFFFFF.toInt(), .55f), col, Colors.mix(col, 0xFF000000.toInt(), .45f)),
                floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
        }
        fun fill(p: Path, col: Int) {
            c.save(); c.translate(w * .05f, w * .07f); c.drawPath(p, shadow); c.restore()
            c.drawPath(p, metalPaint(col))
        }
        fun facets(left: Path, right: Path, col: Int) {
            val both = Path().apply { addPath(left); addPath(right) }
            c.save(); c.translate(w * .05f, w * .07f); c.drawPath(both, shadow); c.restore()
            c.drawPath(left, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Colors.mix(col, 0xFFFFFFFF.toInt(), .35f) })
            c.drawPath(right, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Colors.mix(col, 0xFF000000.toInt(), .38f) })
        }
        val lume = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LUME }

        if (part == 3) {
            // Second hand: needle, counterweight, accent tip.
            val nw = (bw * .22f).coerceAtLeast(1.5f)
            val p = Path().apply { addRoundRect(RectF(cx - nw / 2, 0f, cx + nw / 2, len + tail * .9f), nw / 2, nw / 2, Path.Direction.CW) }
            when (style) {
                "arrow", "baton" -> p.addCircle(cx, len * .2f, bw * .34f, Path.Direction.CW)
                "cyber" -> p.addRect(cx - bw * .3f, len * .08f, cx + bw * .3f, len * .2f, Path.Direction.CW)
                else -> p.addCircle(cx, len + tail * .62f, bw * .36f, Path.Direction.CW)
            }
            fill(p, accent)
            if (style == "arrow" || style == "baton") c.drawCircle(cx, len * .2f, bw * .2f, lume)
        } else when (style) {
            "dauphine" -> {
                val l = Path().apply { moveTo(cx, 0f); lineTo(cx - bw / 2, len); lineTo(cx, len + tail); close() }
                val r = Path().apply { moveTo(cx, 0f); lineTo(cx + bw / 2, len); lineTo(cx, len + tail); close() }
                facets(l, r, metal)
            }
            "leaf" -> {
                val p = Path().apply {
                    moveTo(cx, 0f)
                    cubicTo(cx + bw * .75f, len * .35f, cx + bw * .45f, len * .8f, cx + bw * .14f, len + tail)
                    lineTo(cx - bw * .14f, len + tail)
                    cubicTo(cx - bw * .45f, len * .8f, cx - bw * .75f, len * .35f, cx, 0f); close()
                }
                fill(p, metal)
                c.drawLine(cx, len * .08f, cx, len * .9f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Colors.mix(metal, 0xFF000000.toInt(), .4f); strokeWidth = bw * .05f })
            }
            "sword", "skeleton" -> {
                val p = Path().apply {
                    moveTo(cx, 0f); lineTo(cx + bw / 2, len * .16f); lineTo(cx + bw * .3f, len + tail)
                    lineTo(cx - bw * .3f, len + tail); lineTo(cx - bw / 2, len * .16f); close()
                }
                if (style == "skeleton") {
                    val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE; strokeWidth = bw * .16f; strokeJoin = Paint.Join.ROUND }
                    c.save(); c.translate(w * .05f, w * .07f); c.drawPath(p, Paint(shadow).apply { this.style = Paint.Style.STROKE; strokeWidth = bw * .16f }); c.restore()
                    c.drawPath(p, Paint(metalPaint(metal)).apply { this.style = Paint.Style.STROKE; strokeWidth = st.strokeWidth; strokeJoin = Paint.Join.ROUND })
                    c.drawPath(Path().apply { moveTo(cx, len * .05f); lineTo(cx + bw * .22f, len * .16f); lineTo(cx - bw * .22f, len * .16f); close() }, lume)
                } else {
                    fill(p, metal)
                    c.drawPath(Path().apply {
                        moveTo(cx, len * .1f); lineTo(cx + bw * .26f, len * .2f); lineTo(cx + bw * .14f, len * .86f)
                        lineTo(cx - bw * .14f, len * .86f); lineTo(cx - bw * .26f, len * .2f); close()
                    }, lume)
                }
            }
            "arrow" -> {
                val stick = Path().apply { addRoundRect(RectF(cx - bw * .18f, len * .3f, cx + bw * .18f, len + tail), bw * .1f, bw * .1f, Path.Direction.CW) }
                val head = Path().apply {
                    if (part == 1) { moveTo(cx, len * .04f); lineTo(cx + bw / 2, len * .42f); lineTo(cx, len * .34f); lineTo(cx - bw / 2, len * .42f); close() }
                    else { moveTo(cx, 0f); lineTo(cx + bw * .3f, len * .12f); lineTo(cx + bw * .18f, len * .32f); lineTo(cx - bw * .18f, len * .32f); lineTo(cx - bw * .3f, len * .12f); close() }
                }
                fill(Path().apply { addPath(stick); addPath(head) }, metal)
                c.drawPath(Path().apply {
                    if (part == 1) { moveTo(cx, len * .1f); lineTo(cx + bw * .3f, len * .36f); lineTo(cx, len * .3f); lineTo(cx - bw * .3f, len * .36f); close() }
                    else addRect(cx - bw * .09f, len * .06f, cx + bw * .09f, len * .7f, Path.Direction.CW)
                }, lume)
            }
            "cyber" -> {
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; alpha = 150; maskFilter = BlurMaskFilter(bw * .45f, BlurMaskFilter.Blur.NORMAL) }
                val bar = RectF(cx - bw * .2f, len * .02f, cx + bw * .2f, len + tail)
                c.drawRect(bar, glow)
                c.drawRect(bar, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = metal })
                c.drawRect(cx - bw * .2f, len * .02f, cx + bw * .2f, len * .2f, Paint().apply { color = accent })
            }
            else -> { // baton
                val p = Path().apply { addRoundRect(RectF(cx - bw * .32f, 0f, cx + bw * .32f, len + tail), bw * .32f, bw * .32f, Path.Direction.CW) }
                fill(p, metal)
                c.drawRoundRect(RectF(cx - bw * .14f, len * .06f, cx + bw * .14f, len * .74f), bw * .14f, bw * .14f, lume)
            }
        }
        if (cap) {
            val r = bw * (if (part == 3) .5f else .42f)
            c.drawCircle(cx + w * .03f, len + w * .04f, r, shadow)
            c.drawCircle(cx, len, r, metalPaint(if (part == 3) accent else metal))
            c.drawCircle(cx, len, r * .38f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111111.toInt() })
        }
    }
}

object Colors {
    fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(s: Int) = (((a shr s) and 0xFF) * (1 - t) + ((b shr s) and 0xFF) * t).roundToInt().coerceIn(0, 255)
        return (((a ushr 24) and 0xFF) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    fun alpha(c: Int, a: Float): Int = ((a * 255).roundToInt().coerceIn(0, 255) shl 24) or (c and 0xFFFFFF)
}
