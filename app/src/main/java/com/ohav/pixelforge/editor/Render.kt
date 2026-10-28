package com.ohav.pixelforge.editor

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pictures a design needs while drawing: imported images by file name, Iconify icons by id,
 * picture hands by (layer, part 1-3). Null = not ready yet (a placeholder is drawn).
 */
class DocAssets(
    val image: (String) -> ImageBitmap?,
    val icon: (String) -> Iconify.Art?,
    val hand: (Layer, Int) -> ImageBitmap? = { _, _ -> null },
)

val FontWeights = listOf(FontWeight.Thin, FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.Bold)

/** Layers a phone widget shows as live system text instead of baking them into its picture. */
val Layer.isLive get() = kind == LayerKind.TIME || kind == LayerKind.DATE

/**
 * Draws [doc] scaled into this draw area (same aspect as the doc), clipped to its shape; mirrors
 * [FaceXml]. [live] = false leaves out TIME/DATE (widgets draw those as live TextClocks).
 */
fun DrawScope.drawDoc(doc: FaceDoc, bg: Color, measurer: TextMeasurer, now: LocalDateTime, assets: DocAssets, live: Boolean = true) {
    val u = size.width / doc.w
    val date = now.format(DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH))
    val shape = Path().apply {
        if (doc.round) addOval(Rect(Offset.Zero, size)) else addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(doc.corner * u)))
    }
    clipPath(shape) {
        if (doc.background2 == 0L) drawRect(bg)
        else drawRect(Brush.linearGradient(listOf(bg, Color(doc.background2)), Offset.Zero, Offset(size.width, size.height)))
        doc.layers.filter { it.visible && (live || !it.isLive) }.forEach { l ->
            val c = Color(l.color)
            val center = Offset(l.x * u, l.y * u)
            val r = l.size * u
            val brush = if (l.color2 == 0L) SolidColor(c) else Brush.linearGradient(
                listOf(c, Color(l.color2)), Offset(center.x - l.box().first * u / 2, center.y - l.box().second * u / 2),
                Offset(center.x + l.box().first * u / 2, center.y + l.box().second * u / 2),
            )
            when (l.kind) {
                LayerKind.RING -> drawCircle(brush, r, center, style = Stroke(l.thickness * u))
                LayerKind.ARC -> drawArc(
                    brush, l.angle - 90, l.height, false, Offset(center.x - r, center.y - r),
                    Size(2 * r, 2 * r), style = Stroke(l.thickness * u, cap = StrokeCap.Round),
                )
                LayerKind.TICKS -> l.ticks().forEach { (a, b) ->
                    drawLine(c, Offset(a.first * u, a.second * u), Offset(b.first * u, b.second * u), l.thickness * u, StrokeCap.Round)
                }
                LayerKind.HANDS -> if (l.text.isNotEmpty()) {
                    val sec = now.second + now.nano / 1e9f
                    val min = now.minute + sec / 60
                    listOf((now.hour % 12 + min / 60) * 30, min * 6, now.second * 6f).take(if (l.extra) 3 else 2).forEachIndexed { i, deg ->
                        val img = assets.hand(l, i + 1) ?: return@forEachIndexed
                        val h = l.handSpec(i + 1)
                        rotate(deg, center) {
                            drawImage(
                                img, IntOffset.Zero, IntSize(img.width, img.height),
                                IntOffset((center.x - h.w * u / 2).roundToInt(), (center.y - h.len * u).roundToInt()),
                                IntSize((h.w * u).roundToInt(), (h.h * u).roundToInt()), filterQuality = FilterQuality.High,
                            )
                        }
                    }
                } else {
                    val t = l.thickness * u
                    val sec = now.second + now.nano / 1e9f
                    val min = now.minute + sec / 60
                    fun hand(deg: Float, len: Float, w: Float, col: Color) = rotate(deg, center) {
                        drawLine(col, Offset(center.x, center.y + w * 1.6f), Offset(center.x, center.y - len), w, StrokeCap.Round)
                    }
                    val accent = if (l.color2 == 0L) c else Color(l.color2)
                    hand((now.hour % 12 + min / 60) * 30, r * .62f, t * 1.15f, c)
                    hand(min * 6, r, t, c)
                    if (l.extra) hand(now.second * 6f, r * 1.08f, maxOf(t * .35f, 2f * u), accent)
                    drawCircle(accent, t * .9f, center)
                }
                LayerKind.ICON -> rotate(l.rotation, center) {
                    val side = l.size * u
                    val tl = Offset(center.x - side / 2, center.y - side / 2)
                    if (Iconify.isIconify(l.text)) {
                        val art = assets.icon(l.text)
                        if (art == null) drawRoundRect(Color.White.copy(alpha = .1f), tl, Size(side, side), CornerRadius(side / 5))
                        else drawImage(
                            art.image, IntOffset.Zero, IntSize(art.image.width, art.image.height),
                            IntOffset(tl.x.roundToInt(), tl.y.roundToInt()), IntSize(side.roundToInt(), side.roundToInt()),
                            alpha = if (art.mono) 1f else c.alpha, colorFilter = if (art.mono) ColorFilter.tint(c) else null,
                            filterQuality = FilterQuality.High,
                        )
                    } else {
                        val s = side / IconLibrary.VIEWPORT
                        withTransform({ translate(tl.x, tl.y); scale(s, s, Offset.Zero) }) {
                            IconLibrary.paths(l.text).forEach { drawPath(it, c) }
                        }
                    }
                }
                LayerKind.IMAGE -> rotate(l.rotation, center) {
                    val w = l.size * u; val h = l.height * u
                    val tl = Offset(center.x - w / 2, center.y - h / 2)
                    val img = assets.image(l.text)
                    val clip = Path().apply { addRoundRect(RoundRect(tl.x, tl.y, tl.x + w, tl.y + h, CornerRadius(l.thickness * u))) }
                    clipPath(clip) {
                        if (img == null) drawRect(Color.Gray.copy(alpha = .3f), tl, Size(w, h))
                        else {
                            val src = ImageAssets.centerCrop(img.width, img.height, w, h)
                            drawImage(
                                img, IntOffset(src.left, src.top), IntSize(src.width(), src.height()),
                                IntOffset(tl.x.toInt(), tl.y.toInt()), IntSize(w.toInt(), h.toInt()), alpha = c.alpha,
                            )
                        }
                    }
                }
                LayerKind.CIRCLE -> drawCircle(brush, r, center)
                LayerKind.GAUGE -> {
                    val d = DataSource.of(l.text)
                    val sweep = l.height * d.sampleFraction(now)
                    if (l.extra) {
                        rotate(l.angle + sweep, center) {
                            drawLine(c, Offset(center.x, center.y + l.thickness * u * 1.6f), Offset(center.x, center.y - r), l.thickness * u, StrokeCap.Round)
                        }
                        drawCircle(if (l.color2 == 0L) c else Color(l.color2), l.thickness * u, center)
                    } else drawArc(
                        brush, l.angle - 90, sweep, false, Offset(center.x - r, center.y - r),
                        Size(2 * r, 2 * r), style = Stroke(l.thickness * u, cap = StrokeCap.Round),
                    )
                }
                LayerKind.RECT -> rotate(l.rotation, center) {
                    drawRoundRect(brush, Offset(center.x - r / 2, center.y - l.height * u / 2), Size(r, l.height * u), CornerRadius(l.thickness * u))
                }
                else -> {
                    val text = when (l.kind) {
                        LayerKind.TIME -> now.format(DateTimeFormatter.ofPattern(if (l.extra) "hh:mm:ss" else "hh:mm"))
                        LayerKind.DATE -> date
                        LayerKind.DATA -> DataSource.of(l.text).let { "${it.sample(now).roundToInt()}${it.unit}" }
                        else -> l.text
                    }
                    val m = measurer.measure(text, TextStyle(color = c, fontSize = (l.size * u).toSp(), fontWeight = FontWeights[l.weight.coerceIn(FontWeights.indices)]))
                    rotate(l.rotation, center) { drawText(m, topLeft = Offset(center.x - m.size.width / 2f, center.y - m.size.height / 2f)) }
                }
            }
        }
    }
}
