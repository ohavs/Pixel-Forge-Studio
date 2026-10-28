package com.ohav.pixelforge.editor

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Canvas size of every face, in Watch Face Format pixels. */
const val FACE_SIZE = 450f

enum class LayerKind(val title: String) {
    TIME("שעה"), DATE("תאריך"), TEXT("טקסט"), RING("טבעת"), ARC("קשת"), TICKS("סימונים"), HANDS("מחוגים"), CIRCLE("עיגול"), RECT("מלבן"), ICON("אייקון"), IMAGE("תמונה");
    val isImage get() = this == ICON || this == IMAGE
    val isText get() = this == TIME || this == DATE || this == TEXT
}

/**
 * One layer. Position (x, y) is the layer's centre in face pixels. [size] is the font size for
 * text kinds, the radius for RING/CIRCLE and the width for RECT; [height] is the RECT height and
 * [thickness] the RING/ARC/TICKS stroke or RECT corner radius. ARC: [angle] start (degrees
 * clockwise from 12 o'clock) and [height] sweep. TICKS: [count] marks of length [height]
 * ending at radius [size]. HANDS: minute hand of length [size] and width [thickness], [color2]
 * the second hand. Colors are ARGB (alpha = opacity); [color2] != 0 makes shapes a gradient.
 * [extra]: seconds for TIME, a second hand for HANDS. [rotation] turns RECT, TEXT and images.
 * ICON: [text] is the icon name, [size] its side. IMAGE: [text] is the picture's file name,
 * [size] x [height] its box and [thickness] the corner radius. Both go to image slots on the watch.
 */
data class Layer(
    val id: Long,
    val kind: LayerKind,
    val x: Float = FACE_SIZE / 2,
    val y: Float = FACE_SIZE / 2,
    val size: Float,
    val height: Float = 0f,
    val thickness: Float = 0f,
    val color: Long = 0xFFFFFFFF,
    val text: String = "",
    val weight: Int = 4,
    val color2: Long = 0,
    val rotation: Float = 0f,
    val extra: Boolean = false,
    val angle: Float = 0f,
    val count: Int = 12,
    val visible: Boolean = true,
    val locked: Boolean = false,
) {
    val name get() = if ((kind == LayerKind.TEXT || kind == LayerKind.ICON) && text.isNotBlank()) text else kind.title

    /** Approximate box size (w, h) for text kinds; real for shapes. */
    fun box(): Pair<Float, Float> = when (kind) {
        LayerKind.TIME -> size * (if (extra) 4.4f else 2.9f) to size * 1.25f
        LayerKind.DATE -> size * 0.62f * 10 to size * 1.3f
        LayerKind.TEXT -> (size * 0.62f * text.length.coerceAtLeast(1) + size * .4f) to size * 1.3f
        LayerKind.RING, LayerKind.ARC, LayerKind.TICKS, LayerKind.HANDS, LayerKind.CIRCLE -> size * 2 to size * 2
        LayerKind.RECT, LayerKind.IMAGE -> size to height
        LayerKind.ICON -> size to size
    }

    fun hit(px: Float, py: Float): Boolean = when (kind) {
        LayerKind.RING, LayerKind.ARC -> abs(hypot(px - x, py - y) - size) < thickness / 2 + 18
        LayerKind.TICKS -> abs(hypot(px - x, py - y) - (size - height / 2)) < height / 2 + 14
        LayerKind.CIRCLE, LayerKind.HANDS -> hypot(px - x, py - y) < size
        else -> box().let { (w, h) -> abs(px - x) < w / 2 + 8 && abs(py - y) < h / 2 + 8 }
    }

    /** Uniformly resized by [f] (pinch). ARC's [height] is an angle, so it stays. */
    fun scaled(f: Float): Layer = copy(
        size = (size * f).coerceIn(4f, 1000f),
        height = if (kind == LayerKind.ARC) height else (height * f).coerceIn(0f, 1000f),
        thickness = (thickness * f).coerceIn(0f, 500f),
    )

    /** TICKS segments as (outer, inner) points, starting at 12 o'clock. */
    fun ticks(): List<Pair<Pair<Float, Float>, Pair<Float, Float>>> = List(count.coerceAtLeast(1)) { i ->
        val a = Math.toRadians(360.0 * i / count - 90)
        val cs = kotlin.math.cos(a).toFloat(); val sn = kotlin.math.sin(a).toFloat()
        (x + cs * size to y + sn * size) to (x + cs * (size - height) to y + sn * (size - height))
    }

    fun toJson(): JSONObject = JSONObject().put("id", id).put("kind", kind.name).put("x", x).put("y", y)
        .put("size", size).put("height", height).put("thickness", thickness).put("color", color)
        .put("text", text).put("weight", weight).put("color2", color2).put("rotation", rotation).put("extra", extra).put("angle", angle).put("count", count).put("visible", visible).put("locked", locked)

    companion object {
        fun fromJson(o: JSONObject) = Layer(
            o.getLong("id"), LayerKind.valueOf(o.getString("kind")), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(),
            o.getDouble("size").toFloat(), o.optDouble("height", 0.0).toFloat(), o.optDouble("thickness", 0.0).toFloat(),
            o.getLong("color"), o.optString("text"), o.optInt("weight", 4), o.optLong("color2", 0), o.optDouble("rotation", 0.0).toFloat(), o.optBoolean("extra", false), o.optDouble("angle", 0.0).toFloat(), o.optInt("count", 12), o.optBoolean("visible", true), o.optBoolean("locked", false),
        )

        fun create(kind: LayerKind, id: Long, accent: Long): Layer = when (kind) {
            LayerKind.TIME -> Layer(id, kind, y = 205f, size = 96f)
            LayerKind.DATE -> Layer(id, kind, y = 290f, size = 28f, color = accent)
            LayerKind.TEXT -> Layer(id, kind, y = 130f, size = 30f, text = "HELLO", color = accent)
            LayerKind.RING -> Layer(id, kind, size = 205f, thickness = 8f, color = accent)
            LayerKind.ARC -> Layer(id, kind, size = 190f, thickness = 14f, angle = 225f, height = 120f, color = accent)
            LayerKind.TICKS -> Layer(id, kind, size = 218f, height = 16f, thickness = 4f, count = 12)
            LayerKind.HANDS -> Layer(id, kind, size = 170f, thickness = 10f, color2 = accent, extra = true)
            LayerKind.CIRCLE -> Layer(id, kind, y = 340f, size = 22f, color = accent)
            LayerKind.RECT -> Layer(id, kind, y = 340f, size = 140f, height = 44f, thickness = 22f, color = accent)
            LayerKind.ICON -> Layer(id, kind, y = 120f, size = 56f, text = "Favorite", color = accent)
            LayerKind.IMAGE -> Layer(id, kind, size = 200f, height = 200f, thickness = 100f)
        }
    }
}

/**
 * A whole design: a watch face (450 x 450, round) or a phone widget ([w] x [h], rounded rect
 * with [corner]). Layers are drawn in list order: first is the bottom. [background2] != 0 makes
 * the background a diagonal gradient.
 */
data class FaceDoc(
    val background: Long = 0xFF0E1015,
    val layers: List<Layer> = emptyList(),
    val background2: Long = 0,
    val w: Float = FACE_SIZE,
    val h: Float = FACE_SIZE,
    val round: Boolean = true,
    val corner: Float = 0f,
) {
    val nextId get() = (layers.maxOfOrNull { it.id } ?: 0) + 1

    fun toJson(): String = JSONObject().put("background", background).put("background2", background2)
        .put("w", w).put("h", h).put("round", round).put("corner", corner)
        .put("layers", JSONArray().apply { layers.forEach { put(it.toJson()) } }).toString()

    /** Changes the canvas size, keeping layers in the same place relative to the centre. */
    fun resized(nw: Float, nh: Float): FaceDoc {
        val dx = (nw - w) / 2; val dy = (nh - h) / 2
        return copy(w = nw, h = nh, layers = layers.map { it.copy(x = (it.x + dx).coerceIn(0f, nw), y = (it.y + dy).coerceIn(0f, nh)) })
    }

    companion object {
        fun fromJson(s: String): FaceDoc {
            val o = JSONObject(s)
            val arr = o.getJSONArray("layers")
            return FaceDoc(
                o.getLong("background"), List(arr.length()) { Layer.fromJson(arr.getJSONObject(it)) }, o.optLong("background2", 0),
                o.optDouble("w", FACE_SIZE.toDouble()).toFloat(), o.optDouble("h", FACE_SIZE.toDouble()).toFloat(),
                o.optBoolean("round", true), o.optDouble("corner", 0.0).toFloat(),
            )
        }

        val Starter = FaceDoc(
            0xFF120E2E,
            listOf(
                Layer.create(LayerKind.RING, 1, 0xFF8B7BFF),
                Layer.create(LayerKind.TIME, 2, 0xFF8B7BFF),
                Layer.create(LayerKind.DATE, 3, 0xFFFF6A4D),
            ),
        )
    }
}

/** Turns a [FaceDoc] into Watch Face Format v1 XML. */
object FaceXml {
    /** Number of image slots baked into the template APK (watchface/src/custom/res/drawable-nodpi). */
    const val SLOTS = 24

    /** Layers that need a picture on the watch, in slot order. */
    fun imageLayers(doc: FaceDoc) = doc.layers.filter { it.visible && it.kind.isImage }.take(SLOTS)

    fun build(doc: FaceDoc): String = buildString {
        val slots = imageLayers(doc).withIndex().associate { (i, l) -> l.id to i }
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<WatchFace width="450" height="450" clipShape="CIRCLE">""")
        val analog = doc.layers.any { it.visible && it.kind == LayerKind.HANDS }
        appendLine("""  <Metadata key="CLOCK_TYPE" value="${if (analog) "ANALOG" else "DIGITAL"}"/>""")
        appendLine("""  <Scene backgroundColor="${c(doc.background)}">""")
        appendLine("""    <Variant mode="AMBIENT" target="backgroundColor" value="#ff000000"/>""")
        if (doc.background2 != 0L) append("""
    <PartDraw x="0" y="0" width="450" height="450" name="bg">
      <Variant mode="AMBIENT" target="alpha" value="0"/>
      <Rectangle x="0" y="0" width="450" height="450">
        <Fill color="${c(doc.background)}"><LinearGradient startX="0" startY="0" endX="450" endY="450" colors="${c(doc.background)} ${c(doc.background2)}" positions="0 1"/></Fill>
      </Rectangle>
    </PartDraw>
""")
        doc.layers.filter { it.visible && (!it.kind.isImage || it.id in slots) }
            .forEachIndexed { i, l -> append(layer(l, "l$i", slots[l.id] ?: 0)) }
        appendLine("  </Scene>")
        appendLine("</WatchFace>")
    }

    private fun layer(l: Layer, name: String, slot: Int): String {
        val (w, h) = l.box()
        val bx = (l.x - w / 2).i; val by = (l.y - h / 2).i
        val font = """<Font family="SYNC_TO_DEVICE" size="${l.size.i}" weight="${Weights[l.weight.coerceIn(Weights.indices)]}" slant="NORMAL" color="${c(l.color)}">"""
        return when (l.kind) {
            LayerKind.TIME -> """
    <DigitalClock x="$bx" y="$by" width="${w.i}" height="${h.i}">
      <TimeText x="0" y="0" width="${w.i}" height="${h.i}" format="${if (l.extra) "hh:mm:ss" else "hh:mm"}" hourFormat="SYNC_TO_DEVICE" align="CENTER">
        ${font.dropLast(1)}/>
      </TimeText>
    </DigitalClock>
"""
            LayerKind.DATE -> """
    <PartText x="$bx" y="$by" width="${w.i}" height="${h.i}" name="$name">
      <Text align="CENTER" ellipsis="TRUE">
        $font<Template>%s %s<Parameter expression="[DAY_OF_WEEK_S]"/><Parameter expression="[DAY]"/></Template></Font>
      </Text>
    </PartText>
"""
            LayerKind.TEXT -> """
    <PartText x="$bx" y="$by" width="${w.i}" height="${h.i}" angle="${l.rotation.i}" name="$name">
      <Text align="CENTER" ellipsis="TRUE">
        $font${escape(l.text)}</Font>
      </Text>
    </PartText>
"""
            LayerKind.RING -> {
                val r = l.size; val t = l.thickness
                val box = (2 * r + t).i
                """
    <PartDraw x="${(l.x - r - t / 2).i}" y="${(l.y - r - t / 2).i}" width="$box" height="$box" name="$name">
      <Ellipse x="${(t / 2).i}" y="${(t / 2).i}" width="${(2 * r).i}" height="${(2 * r).i}">
        ${stroke(l, 0f, 0f, box.toFloat(), box.toFloat())}
      </Ellipse>
    </PartDraw>
"""
            }
            LayerKind.ARC -> """
    <PartDraw x="0" y="0" width="450" height="450" name="$name">
      <Arc centerX="${l.x.i}" centerY="${l.y.i}" width="${(2 * l.size).i}" height="${(2 * l.size).i}" startAngle="${l.angle.i}" endAngle="${(l.angle + l.height).i}">
        ${stroke(l, l.x - l.size, l.y - l.size, l.x + l.size, l.y + l.size)}
      </Arc>
    </PartDraw>
"""
            LayerKind.TICKS -> buildString {
                append("""
    <PartDraw x="0" y="0" width="450" height="450" name="$name">
""")
                l.ticks().forEach { (a, b) ->
                    append("""      <Line startX="${a.first.i}" startY="${a.second.i}" endX="${b.first.i}" endY="${b.second.i}"><Stroke color="${c(l.color)}" thickness="${l.thickness.i}" cap="ROUND"/></Line>
""")
                }
                append("    </PartDraw>\n")
            }
            LayerKind.HANDS -> buildString {
                append(hand(l, "${name}h", "([HOUR_0_11] + [MINUTE] / 60) * 30", l.size * .62f, l.thickness * 1.15f, l.color, false))
                append(hand(l, "${name}m", "([MINUTE] + [SECOND] / 60) * 6", l.size, l.thickness, l.color, false))
                if (l.extra) append(hand(l, "${name}s", "[SECOND] * 6", l.size * 1.08f, (l.thickness * .35f).coerceAtLeast(2f), l.color2.takeIf { it != 0L } ?: l.color, true))
                val r = l.thickness * .9f
                append("""
    <PartDraw x="${(l.x - r).i}" y="${(l.y - r).i}" width="${(2 * r).i}" height="${(2 * r).i}" name="${name}c">
      <Ellipse x="0" y="0" width="${(2 * r).i}" height="${(2 * r).i}"><Fill color="${c(l.color2.takeIf { it != 0L } ?: l.color)}"/></Ellipse>
    </PartDraw>
""")
            }
            LayerKind.CIRCLE -> """
    <PartDraw x="$bx" y="$by" width="${w.i}" height="${h.i}" name="$name">
      <Ellipse x="0" y="0" width="${w.i}" height="${h.i}">
        ${fill(l, w, h)}
      </Ellipse>
    </PartDraw>
"""
            LayerKind.ICON, LayerKind.IMAGE -> """
    <PartImage x="$bx" y="$by" width="${w.i}" height="${h.i}" angle="${l.rotation.i}" name="$name">
      <Image resource="slot_$slot"/>
    </PartImage>
"""
            LayerKind.RECT -> """
    <PartDraw x="$bx" y="$by" width="${w.i}" height="${h.i}" angle="${l.rotation.i}" name="$name">
      <RoundRectangle x="0" y="0" width="${w.i}" height="${h.i}" cornerRadiusX="${l.thickness.i}" cornerRadiusY="${l.thickness.i}">
        ${fill(l, w, h)}
      </RoundRectangle>
    </PartDraw>
"""
        }
    }

    /** One rotating hand: a rounded bar from a short tail through the pivot to [len]. */
    private fun hand(l: Layer, name: String, angle: String, len: Float, t: Float, color: Long, hideInAmbient: Boolean): String {
        val tail = t * 1.6f
        return """
    <PartDraw x="${(l.x - 225).i}" y="${(l.y - 225).i}" width="450" height="450" pivotX="0.5" pivotY="0.5" name="$name">
      <Transform target="angle" value="$angle"/>${if (hideInAmbient) "\n      <Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>" else ""}
      <RoundRectangle x="${(225 - t / 2).i}" y="${(225 - len).i}" width="${t.i}" height="${(len + tail).i}" cornerRadiusX="${(t / 2).i}" cornerRadiusY="${(t / 2).i}">
        <Fill color="${c(color)}"/>
      </RoundRectangle>
    </PartDraw>
"""
    }

    private fun gradient(l: Layer, x1: Float, y1: Float, x2: Float, y2: Float) =
        """<LinearGradient startX="${x1.i}" startY="${y1.i}" endX="${x2.i}" endY="${y2.i}" colors="${c(l.color)} ${c(l.color2)}" positions="0 1"/>"""

    private fun fill(l: Layer, w: Float, h: Float) =
        if (l.color2 == 0L) """<Fill color="${c(l.color)}"/>""" else """<Fill color="${c(l.color)}">${gradient(l, 0f, 0f, w, h)}</Fill>"""

    private fun stroke(l: Layer, x1: Float, y1: Float, x2: Float, y2: Float): String {
        val open = """<Stroke color="${c(l.color)}" thickness="${l.thickness.i}"${if (l.kind == LayerKind.RING) "" else " cap=\"ROUND\""}"""
        return if (l.color2 == 0L) "$open/>" else "$open>${gradient(l, x1, y1, x2, y2)}</Stroke>"
    }

    private val Weights = listOf("THIN", "LIGHT", "NORMAL", "MEDIUM", "BOLD")
    private val Float.i get() = roundToInt()
    private fun c(argb: Long) = "#%08x".format(argb and 0xFFFFFFFF)
    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
