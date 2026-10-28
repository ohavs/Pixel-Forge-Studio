package com.ohav.pixelforge.editor

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** A color set for the generators: [bg] first, then 3-4 colors from back to front. */
class GenPalette(val title: String, val bg: Long, val colors: List<Long>)

/**
 * Procedural backgrounds in the spirit of Haikei (layered waves, blobs, peaks, low-poly...).
 * Every one is drawn from a seed, so "shuffle" gives a new variation and the same seed redraws
 * the same picture at any size.
 */
enum class Generator(val title: String) {
    WAVES("גלים בשכבות"), PEAKS("פסגות"), BLOB("כתם"), BLOBS("כתמים רכים"), RINGS("טבעות"), LOWPOLY("לואו-פולי"),
    BURST("קרניים"), CIRCLES("עיגולים"), TOPO("טופוגרפיה"), STEPS("מדרגות");

    companion object {
        val palettes = listOf(
            GenPalette("לילה", 0xFF0B1026, listOf(0xFF1B2A6B, 0xFF3D4FC4, 0xFF7B6CFF, 0xFFB9A8FF)),
            GenPalette("שקיעה", 0xFF1A0A1F, listOf(0xFF5B1A4A, 0xFFB3305B, 0xFFF26B4F, 0xFFFFC36A)),
            GenPalette("אוקיינוס", 0xFF031A24, listOf(0xFF06485E, 0xFF0B7A8C, 0xFF19B3B1, 0xFF8BF0D8)),
            GenPalette("יער", 0xFF0A1A12, listOf(0xFF184A2C, 0xFF2E7D4A, 0xFF65B26B, 0xFFC9E59A)),
            GenPalette("ניאון", 0xFF07040F, listOf(0xFF3A0CA3, 0xFF7209B7, 0xFFF72585, 0xFF4CC9F0)),
            GenPalette("חול", 0xFF2A2118, listOf(0xFF6B4F35, 0xFFA87D53, 0xFFD9B48A, 0xFFF4E2C8)),
            GenPalette("מונו", 0xFF0A0A0A, listOf(0xFF222222, 0xFF3A3A3A, 0xFF5C5C5C, 0xFF9A9A9A)),
            GenPalette("קרח", 0xFFEAF4FB, listOf(0xFFBFDDF2, 0xFF8EC1E6, 0xFF5A9BD4, 0xFF2D6CB0)),
        )
    }
}

object Generators {
    /** Draws [g] into a new [w] x [h] bitmap. */
    fun render(g: Generator, pal: GenPalette, seed: Int, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = Random(seed)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(pal.bg.toInt())
        val cols = pal.colors.map { it.toInt() }
        val wf = w.toFloat(); val hf = h.toFloat()
        val s = minOf(wf, hf)
        when (g) {
            Generator.WAVES -> cols.forEachIndexed { i, col ->
                val base = hf * (.35f + .55f * (i + 1) / cols.size)
                val f1 = 1f + r.nextFloat() * 2; val f2 = 2f + r.nextFloat() * 3; val ph = r.nextFloat() * 6.28f
                val amp = hf * (.03f + r.nextFloat() * .05f)
                val path = Path().apply {
                    moveTo(0f, hf)
                    for (x in 0..64) {
                        val t = x / 64f
                        lineTo(t * wf, base + amp * (sin(t * f1 * 2 * PI + ph) + .5 * sin(t * f2 * 2 * PI + ph * 2)).toFloat())
                    }
                    lineTo(wf, hf); close()
                }
                p.color = col; c.drawPath(path, p)
            }
            Generator.PEAKS -> cols.forEachIndexed { i, col ->
                val base = hf * (.4f + .5f * (i + 1) / cols.size)
                val n = 6 + r.nextInt(6)
                val path = Path().apply {
                    moveTo(0f, hf); lineTo(0f, base)
                    for (k in 1..n) lineTo(wf * k / n, base - hf * (.04f + r.nextFloat() * .14f) * (if (k % 2 == 0) .3f else 1f))
                    lineTo(wf, hf); close()
                }
                p.color = col; c.drawPath(path, p)
            }
            Generator.BLOB -> {
                val path = blob(r, wf / 2, hf / 2, s * .36f, 9, .28f)
                p.shader = LinearGradient(0f, 0f, wf, hf, cols.first(), cols.last(), Shader.TileMode.CLAMP)
                c.drawPath(path, p)
            }
            Generator.BLOBS -> {
                p.maskFilter = BlurMaskFilter(s * .12f, BlurMaskFilter.Blur.NORMAL)
                repeat(7) { i ->
                    p.color = cols[i % cols.size]
                    c.drawPath(blob(r, r.nextFloat() * wf, r.nextFloat() * hf, s * (.18f + r.nextFloat() * .2f), 7, .3f), p)
                }
            }
            Generator.RINGS -> {
                val n = 8 + r.nextInt(6)
                val cx = wf * (.3f + r.nextFloat() * .4f); val cy = hf * (.3f + r.nextFloat() * .4f)
                val maxR = hypot(wf, hf)
                for (k in n downTo 1) {
                    p.color = mix(cols.first(), cols.last(), 1 - k.toFloat() / n)
                    c.drawCircle(cx, cy, maxR * k / n, p)
                }
            }
            Generator.LOWPOLY -> {
                val nx = 7; val ny = (7 * hf / wf).toInt().coerceAtLeast(3)
                val pts = Array(ny + 1) { y -> Array(nx + 1) { x ->
                    val jx = if (x in 1 until nx) (r.nextFloat() - .5f) * wf / nx * .8f else 0f
                    val jy = if (y in 1 until ny) (r.nextFloat() - .5f) * hf / ny * .8f else 0f
                    floatArrayOf(wf * x / nx + jx, hf * y / ny + jy)
                } }
                fun tri(a: FloatArray, b: FloatArray, d: FloatArray) {
                    val mx = (a[0] + b[0] + d[0]) / 3; val my = (a[1] + b[1] + d[1]) / 3
                    p.color = ramp(cols, ((mx / wf + my / hf) / 2 + (r.nextFloat() - .5f) * .12f).coerceIn(0f, 1f))
                    c.drawPath(Path().apply { moveTo(a[0], a[1]); lineTo(b[0], b[1]); lineTo(d[0], d[1]); close() }, p)
                }
                for (y in 0 until ny) for (x in 0 until nx) {
                    tri(pts[y][x], pts[y][x + 1], pts[y + 1][x]); tri(pts[y][x + 1], pts[y + 1][x + 1], pts[y + 1][x])
                }
            }
            Generator.BURST -> {
                val n = 16 + r.nextInt(16)
                val cx = wf / 2; val cy = hf / 2; val big = hypot(wf, hf)
                for (k in 0 until n) {
                    val a0 = 2 * PI * k / n; val a1 = 2 * PI * (k + .5) / n
                    p.color = cols[k % 2 + (cols.size - 2)]
                    c.drawPath(Path().apply {
                        moveTo(cx, cy); lineTo(cx + big * cos(a0).toFloat(), cy + big * sin(a0).toFloat())
                        lineTo(cx + big * cos(a1).toFloat(), cy + big * sin(a1).toFloat()); close()
                    }, p)
                }
                p.shader = RadialGradient(cx, cy, s * .7f, cols.first(), 0x00000000, Shader.TileMode.CLAMP)
                c.drawCircle(cx, cy, s * .7f, p)
            }
            Generator.CIRCLES -> repeat(26) {
                p.color = cols[r.nextInt(cols.size)]
                p.alpha = 150 + r.nextInt(105)
                c.drawCircle(r.nextFloat() * wf, r.nextFloat() * hf, s * (.03f + r.nextFloat() * r.nextFloat() * .22f), p)
            }
            Generator.TOPO -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = s * .006f
                val cx = wf * (.3f + r.nextFloat() * .4f); val cy = hf * (.3f + r.nextFloat() * .4f)
                val f = IntArray(3) { 2 + r.nextInt(4) }; val ph = FloatArray(3) { r.nextFloat() * 6.28f }
                for (k in 1..22) {
                    val rad = s * .05f * k
                    p.color = ramp(cols, k / 22f)
                    c.drawPath(Path().apply {
                        for (i in 0..120) {
                            val a = 2 * PI * i / 120
                            val wob = 1 + .12f * (sin(a * f[0] + ph[0]) + sin(a * f[1] + ph[1] + k * .15) * .6 + sin(a * f[2] + ph[2]) * .3).toFloat()
                            val x = cx + rad * wob * cos(a).toFloat(); val y = cy + rad * wob * sin(a).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }, p)
                }
            }
            Generator.STEPS -> {
                val n = 6 + r.nextInt(4)
                val fromLeft = r.nextBoolean()
                for (k in 0 until n) {
                    p.color = ramp(cols, k / (n - 1f))
                    val top = hf * (.25f + .7f * k / n)
                    val x0 = if (fromLeft) 0f else wf * (.15f * r.nextFloat())
                    c.drawRoundRect(x0, top, wf * (.6f + .4f * (n - k) / n) + x0, hf + s, s * .06f, s * .06f, p)
                }
            }
        }
        return bmp
    }

    /** A smooth closed blob: [n] points at a radius varied by up to [jitter], joined with quads. */
    private fun blob(r: Random, cx: Float, cy: Float, rad: Float, n: Int, jitter: Float): Path {
        val pts = List(n) { i ->
            val a = 2 * PI * i / n
            val rr = rad * (1 - jitter + 2 * jitter * r.nextFloat())
            floatArrayOf(cx + rr * cos(a).toFloat(), cy + rr * sin(a).toFloat())
        }
        return Path().apply {
            val mid = { i: Int -> val a = pts[i % n]; val b = pts[(i + 1) % n]; floatArrayOf((a[0] + b[0]) / 2, (a[1] + b[1]) / 2) }
            mid(0).let { moveTo(it[0], it[1]) }
            for (i in 1..n) { val q = pts[i % n]; val m = mid(i); quadTo(q[0], q[1], m[0], m[1]) }
            close()
        }
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(sh: Int) = (((a shr sh) and 0xFF) * (1 - t) + ((b shr sh) and 0xFF) * t).toInt() and 0xFF
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun ramp(cols: List<Int>, t: Float): Int {
        val x = t * (cols.size - 1)
        val i = x.toInt().coerceIn(0, cols.size - 2)
        return mix(cols[i], cols[i + 1], x - i)
    }
}
