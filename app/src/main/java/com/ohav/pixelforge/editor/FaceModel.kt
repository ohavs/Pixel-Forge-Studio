package com.ohav.pixelforge.editor

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Canvas size of every face, in Watch Face Format pixels. */
const val FACE_SIZE = 450f

enum class LayerKind(val title: String) {
    TIME("שעה"), DATE("תאריך"), TEXT("טקסט"), RING("טבעת"), ARC("קשת"), TICKS("סימונים"), CIRCLE("עיגול"), RECT("מלבן");
    val isText get() = this == TIME || this == DATE || this == TEXT
}

/**
 * One layer. Position (x, y) is the layer's centre in face pixels. [size] is the font size for
 * text kinds, the radius for RING/CIRCLE and the width for RECT; [height] is the RECT height and
 * [thickness] the RING/ARC/TICKS stroke or RECT corner radius. ARC: [angle] start (degrees
 * clockwise from 12 o'clock) and [height] sweep. TICKS: [count] marks of length [height]
 * ending at radius [size]. Colors are ARGB.
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
    val bold: Boolean = true,
    val angle: Float = 0f,
    val count: Int = 12,
    val visible: Boolean = true,
    val locked: Boolean = false,
) {
    val name get() = if (kind == LayerKind.TEXT && text.isNotBlank()) text else kind.title

    /** Sample string shown for text kinds (the watch fills in the real time/date). */
    fun sample(time: String, date: String) = when (kind) {
        LayerKind.TIME -> time
        LayerKind.DATE -> date
        else -> text
    }

    /** Approximate box size (w, h) for text kinds; real for shapes. */
    fun box(): Pair<Float, Float> = when (kind) {
        LayerKind.TIME -> size * 2.9f to size * 1.25f
        LayerKind.DATE -> size * 0.62f * 10 to size * 1.3f
        LayerKind.TEXT -> (size * 0.62f * text.length.coerceAtLeast(1) + size * .4f) to size * 1.3f
        LayerKind.RING, LayerKind.ARC, LayerKind.TICKS, LayerKind.CIRCLE -> size * 2 to size * 2
        LayerKind.RECT -> size to height
    }

    fun hit(px: Float, py: Float): Boolean = when (kind) {
        LayerKind.RING, LayerKind.ARC -> abs(hypot(px - x, py - y) - size) < thickness / 2 + 18
        LayerKind.TICKS -> abs(hypot(px - x, py - y) - (size - height / 2)) < height / 2 + 14
        LayerKind.CIRCLE -> hypot(px - x, py - y) < size
        else -> box().let { (w, h) -> abs(px - x) < w / 2 + 8 && abs(py - y) < h / 2 + 8 }
    }

    /** TICKS segments as (outer, inner) points, starting at 12 o'clock. */
    fun ticks(): List<Pair<Pair<Float, Float>, Pair<Float, Float>>> = List(count.coerceAtLeast(1)) { i ->
        val a = Math.toRadians(360.0 * i / count - 90)
        val cs = kotlin.math.cos(a).toFloat(); val sn = kotlin.math.sin(a).toFloat()
        (x + cs * size to y + sn * size) to (x + cs * (size - height) to y + sn * (size - height))
    }

    fun toJson(): JSONObject = JSONObject().put("id", id).put("kind", kind.name).put("x", x).put("y", y)
        .put("size", size).put("height", height).put("thickness", thickness).put("color", color)
        .put("text", text).put("bold", bold).put("angle", angle).put("count", count).put("visible", visible).put("locked", locked)

    companion object {
        fun fromJson(o: JSONObject) = Layer(
            o.getLong("id"), LayerKind.valueOf(o.getString("kind")), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(),
            o.getDouble("size").toFloat(), o.optDouble("height", 0.0).toFloat(), o.optDouble("thickness", 0.0).toFloat(),
            o.getLong("color"), o.optString("text"), o.optBoolean("bold", true), o.optDouble("angle", 0.0).toFloat(), o.optInt("count", 12), o.optBoolean("visible", true), o.optBoolean("locked", false),
        )

        fun create(kind: LayerKind, id: Long, accent: Long): Layer = when (kind) {
            LayerKind.TIME -> Layer(id, kind, y = 205f, size = 96f)
            LayerKind.DATE -> Layer(id, kind, y = 290f, size = 28f, color = accent)
            LayerKind.TEXT -> Layer(id, kind, y = 130f, size = 30f, text = "HELLO", color = accent)
            LayerKind.RING -> Layer(id, kind, size = 205f, thickness = 8f, color = accent)
            LayerKind.ARC -> Layer(id, kind, size = 190f, thickness = 14f, angle = 225f, height = 120f, color = accent)
            LayerKind.TICKS -> Layer(id, kind, size = 218f, height = 16f, thickness = 4f, count = 12)
            LayerKind.CIRCLE -> Layer(id, kind, y = 340f, size = 22f, color = accent)
            LayerKind.RECT -> Layer(id, kind, y = 340f, size = 140f, height = 44f, thickness = 22f, color = accent)
        }
    }
}

/** The whole face. Layers are drawn in list order: first is the bottom. */
data class FaceDoc(val background: Long = 0xFF0E1015, val layers: List<Layer> = emptyList()) {
    val nextId get() = (layers.maxOfOrNull { it.id } ?: 0) + 1

    fun toJson(): String = JSONObject().put("background", background)
        .put("layers", JSONArray().apply { layers.forEach { put(it.toJson()) } }).toString()

    companion object {
        fun fromJson(s: String): FaceDoc {
            val o = JSONObject(s)
            val arr = o.getJSONArray("layers")
            return FaceDoc(o.getLong("background"), List(arr.length()) { Layer.fromJson(arr.getJSONObject(it)) })
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
    fun build(doc: FaceDoc): String = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<WatchFace width="450" height="450" clipShape="CIRCLE">""")
        appendLine("""  <Metadata key="CLOCK_TYPE" value="DIGITAL"/>""")
        appendLine("""  <Scene backgroundColor="${c(doc.background)}">""")
        appendLine("""    <Variant mode="AMBIENT" target="backgroundColor" value="#ff000000"/>""")
        doc.layers.filter { it.visible }.forEachIndexed { i, l -> append(layer(l, "l$i")) }
        appendLine("  </Scene>")
        appendLine("</WatchFace>")
    }

    private fun layer(l: Layer, name: String): String {
        val (w, h) = l.box()
        val bx = (l.x - w / 2).i; val by = (l.y - h / 2).i
        val font = """<Font family="SYNC_TO_DEVICE" size="${l.size.i}" weight="${if (l.bold) "BOLD" else "NORMAL"}" slant="NORMAL" color="${c(l.color)}">"""
        return when (l.kind) {
            LayerKind.TIME -> """
    <DigitalClock x="$bx" y="$by" width="${w.i}" height="${h.i}">
      <TimeText x="0" y="0" width="${w.i}" height="${h.i}" format="hh:mm" hourFormat="SYNC_TO_DEVICE" align="CENTER">
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
    <PartText x="$bx" y="$by" width="${w.i}" height="${h.i}" name="$name">
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
        <Stroke color="${c(l.color)}" thickness="${t.i}"/>
      </Ellipse>
    </PartDraw>
"""
            }
            LayerKind.ARC -> """
    <PartDraw x="0" y="0" width="450" height="450" name="$name">
      <Arc centerX="${l.x.i}" centerY="${l.y.i}" width="${(2 * l.size).i}" height="${(2 * l.size).i}" startAngle="${l.angle.i}" endAngle="${(l.angle + l.height).i}">
        <Stroke color="${c(l.color)}" thickness="${l.thickness.i}" cap="ROUND"/>
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
                append("    </PartDraw>
")
            }
            LayerKind.CIRCLE -> """
    <PartDraw x="$bx" y="$by" width="${w.i}" height="${h.i}" name="$name">
      <Ellipse x="0" y="0" width="${w.i}" height="${h.i}">
        <Fill color="${c(l.color)}"/>
      </Ellipse>
    </PartDraw>
"""
            LayerKind.RECT -> """
    <PartDraw x="$bx" y="$by" width="${w.i}" height="${h.i}" name="$name">
      <RoundRectangle x="0" y="0" width="${w.i}" height="${h.i}" cornerRadiusX="${l.thickness.i}" cornerRadiusY="${l.thickness.i}">
        <Fill color="${c(l.color)}"/>
      </RoundRectangle>
    </PartDraw>
"""
        }
    }

    private val Float.i get() = roundToInt()
    private fun c(argb: Long) = "#%08x".format(argb and 0xFFFFFFFF)
    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}

/** Ready-made faces shown in the Add tab. */
object Presets {
    private fun face(bg: Long, vararg layers: Layer) = FaceDoc(bg, layers.mapIndexed { i, l -> l.copy(id = i + 1L) })
    private fun l(kind: LayerKind, color: Long, f: Layer.() -> Layer = { this }) = Layer.create(kind, 0, color).copy(color = color).f()

    val all: List<Pair<String, FaceDoc>> = listOf(
        "Orbit" to FaceDoc.Starter,
        "Minimal" to face(
            0xFF000000,
            l(LayerKind.TIME, 0xFFFFFFFF) { copy(size = 120f, bold = false) },
            l(LayerKind.DATE, 0xFFB8BCC8) { copy(y = 300f, size = 26f, bold = false) },
        ),
        "Classic" to face(
            0xFF0E1015,
            l(LayerKind.TICKS, 0xFF6B7080) { copy(count = 60, height = 8f, thickness = 2f) },
            l(LayerKind.TICKS, 0xFFFFFFFF) { copy(count = 12, height = 22f, thickness = 5f) },
            l(LayerKind.TIME, 0xFFFFFFFF) { copy(size = 84f) },
            l(LayerKind.DATE, 0xFFFF7A59) { copy(y = 285f, size = 24f) },
        ),
        "Activity" to face(
            0xFF06231F,
            l(LayerKind.ARC, 0xFF14453D) { copy(size = 200f, thickness = 18f, angle = 0f, height = 360f) },
            l(LayerKind.ARC, 0xFF2EE59D) { copy(size = 200f, thickness = 18f, angle = 0f, height = 250f) },
            l(LayerKind.ARC, 0xFF0F3A52) { copy(size = 172f, thickness = 18f, angle = 0f, height = 360f) },
            l(LayerKind.ARC, 0xFF00C2FF) { copy(size = 172f, thickness = 18f, angle = 0f, height = 180f) },
            l(LayerKind.TIME, 0xFFFFFFFF) { copy(size = 88f) },
            l(LayerKind.DATE, 0xFF2EE59D) { copy(y = 285f, size = 24f) },
        ),
        "Neon" to face(
            0xFF1A1033,
            l(LayerKind.RING, 0xFFFF3D8B) { copy(size = 212f, thickness = 4f) },
            l(LayerKind.RING, 0xFF00C2FF) { copy(size = 198f, thickness = 2f) },
            l(LayerKind.TIME, 0xFFFF3D8B) { copy(size = 104f) },
            l(LayerKind.TEXT, 0xFF00C2FF) { copy(y = 125f, size = 22f, text = "NEON") },
            l(LayerKind.DATE, 0xFFFFFFFF) { copy(y = 295f, size = 24f) },
        ),
        "Sport" to face(
            0xFF2B0F16,
            l(LayerKind.TICKS, 0xFFFF7A59) { copy(count = 60, height = 14f, thickness = 3f) },
            l(LayerKind.RECT, 0xFFFF7A59) { copy(y = 305f, size = 150f, height = 46f, thickness = 23f) },
            l(LayerKind.TIME, 0xFFFFFFFF) { copy(y = 200f, size = 110f) },
            l(LayerKind.DATE, 0xFF2B0F16) { copy(y = 305f, size = 26f) },
        ),
        "Sunset" to face(
            0xFF101C33,
            l(LayerKind.CIRCLE, 0xFFFF7A59) { copy(y = 225f, size = 150f) },
            l(LayerKind.CIRCLE, 0xFFFFD166) { copy(x = 300f, y = 140f, size = 26f) },
            l(LayerKind.TIME, 0xFF101C33) { copy(size = 92f) },
            l(LayerKind.DATE, 0xFFFFFFFF) { copy(y = 390f, size = 24f) },
        ),
    )
}
