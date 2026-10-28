package com.ohav.pixelforge.editor

import com.ohav.pixelforge.editor.LayerKind.*

/** A color/mood set every preset layout is rendered in. Icons are Iconify ids. */
class Theme(
    val name: String, val bg: Long, val bg2: Long, val fg: Long, val accent: Long, val accent2: Long, val track: Long,
    val emoji: String, val emoji2: String, val glyph: String, val quote: String,
) {
    /** [fg] at 70% opacity, for secondary text. */
    val soft get() = (fg and 0xFFFFFF) or 0xB3000000
}

val Themes = listOf(
    Theme("Midnight", 0xFF0B0D17, 0xFF1B1F3A, 0xFFFFFFFF, 0xFF8B7BFF, 0xFF00C2FF, 0xFF262B48, "fluent-emoji:crescent-moon", "fluent-emoji:milky-way", "ph:moon-stars-fill", "SWEET DREAMS"),
    Theme("Sunset", 0xFF2B0F2E, 0xFFB8455A, 0xFFFFFFFF, 0xFFFFD166, 0xFFFF7A59, 0xFF4A1F3E, "fluent-emoji:sunset", "fluent-emoji:sun", "ph:sun-horizon-fill", "GOLDEN HOUR"),
    Theme("Ocean", 0xFF031B2E, 0xFF0A4D68, 0xFFFFFFFF, 0xFF00E0FF, 0xFF2EE59D, 0xFF123A52, "fluent-emoji:water-wave", "fluent-emoji:tropical-fish", "ph:waves-bold", "STAY SALTY"),
    Theme("Forest", 0xFF0B1F14, 0xFF1E4D2B, 0xFFEFFFF4, 0xFF7CFFA0, 0xFFFFD166, 0xFF1F3B29, "fluent-emoji:evergreen-tree", "fluent-emoji:four-leaf-clover", "ph:tree-evergreen-fill", "GO OUTSIDE"),
    Theme("Candy", 0xFFFFE3F1, 0xFFE2D9FF, 0xFF2B1840, 0xFFFF4FA3, 0xFF7B61FF, 0xFFF2C4DD, "fluent-emoji:lollipop", "fluent-emoji:candy", "ph:heart-fill", "SO SWEET"),
    Theme("Neon", 0xFF0A0014, 0xFF26004D, 0xFFFFFFFF, 0xFFFF2BD6, 0xFF00F0FF, 0xFF2E1450, "fluent-emoji:high-voltage", "fluent-emoji:ringed-planet", "ph:lightning-fill", "PLAY LOUD"),
    Theme("Mono", 0xFF000000, 0, 0xFFFFFFFF, 0xFFB8BCC8, 0xFFFFFFFF, 0xFF26262A, "fluent-emoji:gem-stone", "fluent-emoji:sparkles", "ph:asterisk-bold", "LESS IS MORE"),
    Theme("Fire", 0xFF1A0500, 0xFF5A1200, 0xFFFFF4E6, 0xFFFF6A00, 0xFFFFD166, 0xFF3D1408, "fluent-emoji:fire", "fluent-emoji:rocket", "ph:fire-fill", "KEEP GOING"),
    Theme("Ice", 0xFFE8F4FF, 0xFFB9DCFF, 0xFF0B2545, 0xFF3D7BFF, 0xFF00C2FF, 0xFFC9DFF5, "fluent-emoji:snowflake", "fluent-emoji:snowman", "ph:snowflake-bold", "STAY COOL"),
    Theme("Gold", 0xFF0E0B05, 0xFF2A2210, 0xFFFFF3D6, 0xFFE8B64C, 0xFFFFE29A, 0xFF332A18, "fluent-emoji:crown", "fluent-emoji:glowing-star", "ph:crown-fill", "MAKE IT HAPPEN"),
)

private fun L(
    kind: LayerKind, color: Long, x: Float = 225f, y: Float = 225f, size: Float, height: Float = 0f, thickness: Float = 0f,
    text: String = "", weight: Int = 4, color2: Long = 0, rotation: Float = 0f, extra: Boolean = false, angle: Float = 0f, count: Int = 12,
) = Layer(0, kind, x, y, size, height, thickness, color, text, weight, color2, rotation, extra, angle, count)

private fun doc(t: Theme, vararg layers: Layer, w: Float = FACE_SIZE, h: Float = FACE_SIZE, corner: Float = 0f) =
    FaceDoc(t.bg, layers.mapIndexed { i, l -> l.copy(id = i + 1L) }, t.bg2, w, h, round = corner == 0f, corner = corner)

/** Ready-made watch faces: the originals plus every layout in every [Themes] mood. */
object Presets {
    private val layouts: List<Pair<String, (Theme) -> FaceDoc>> = listOf(
        "Orbit" to { t -> doc(t, L(RING, t.accent, size = 205f, thickness = 8f, color2 = t.accent2), L(TIME, t.fg, y = 210f, size = 104f, weight = 3), L(DATE, t.accent, y = 295f, size = 26f)) },
        "Minimal" to { t -> doc(t, L(TIME, t.fg, y = 215f, size = 124f, weight = 0), L(DATE, t.soft, y = 305f, size = 24f, weight = 1)) },
        "Classic" to { t ->
            doc(
                t, L(TICKS, t.track, size = 218f, height = 8f, thickness = 2f, count = 60), L(TICKS, t.fg, size = 218f, height = 22f, thickness = 5f),
                L(TIME, t.fg, size = 84f), L(DATE, t.accent, y = 285f, size = 24f),
            )
        },
        "Rings" to { t ->
            doc(
                t, L(ARC, t.track, size = 200f, thickness = 18f, height = 360f), L(ARC, t.accent, size = 200f, thickness = 18f, height = 250f),
                L(ARC, t.track, size = 172f, thickness = 18f, height = 360f), L(ARC, t.accent2, size = 172f, thickness = 18f, height = 180f),
                L(TIME, t.fg, size = 88f), L(DATE, t.accent, y = 285f, size = 24f),
            )
        },
        "Analog" to { t ->
            doc(
                t, L(TICKS, t.track, size = 218f, height = 8f, thickness = 2f, count = 60), L(TICKS, t.fg, size = 218f, height = 26f, thickness = 6f),
                L(DATE, t.accent, x = 320f, size = 22f), L(HANDS, t.fg, size = 170f, thickness = 10f, color2 = t.accent, extra = true),
            )
        },
        "Emoji" to { t -> doc(t, L(ICON, 0xFFFFFFFF, y = 122f, size = 110f, text = t.emoji), L(TIME, t.fg, y = 255f, size = 96f), L(DATE, t.accent, y = 330f, size = 24f)) },
        "Glyph" to { t ->
            doc(
                t, L(ARC, t.track, size = 200f, thickness = 12f, height = 360f), L(ARC, t.accent, size = 200f, thickness = 12f, angle = 210f, height = 300f, color2 = t.accent2),
                L(ICON, t.accent, y = 125f, size = 56f, text = t.glyph), L(TIME, t.fg, y = 230f, size = 96f), L(DATE, t.soft, y = 310f, size = 24f),
            )
        },
        "Bold" to { t ->
            doc(
                t, L(TIME, t.fg, y = 200f, size = 130f), L(RECT, t.accent, y = 310f, size = 170f, height = 52f, thickness = 26f, color2 = t.accent2),
                L(DATE, 0xFFFFFFFF, y = 310f, size = 26f),
            )
        },
        "Orb" to { t ->
            doc(
                t, L(CIRCLE, t.accent, size = 150f, color2 = t.accent2), L(CIRCLE, t.accent2, x = 330f, y = 115f, size = 22f),
                L(TIME, 0xFFFFFFFF, size = 84f), L(DATE, t.fg, y = 400f, size = 22f),
            )
        },
        "Dial" to { t ->
            doc(
                t, L(TICKS, t.track, size = 218f, height = 10f, thickness = 2f, count = 60),
                L(ARC, t.accent, size = 190f, thickness = 22f, angle = 225f, height = 270f, color2 = t.accent2),
                L(ICON, t.accent2, y = 335f, size = 44f, text = t.glyph), L(TIME, t.fg, y = 205f, size = 92f, weight = 3), L(DATE, t.soft, y = 270f, size = 22f),
            )
        },
        "Stripe" to { t ->
            doc(
                t, L(RECT, t.accent, size = 680f, height = 130f, rotation = -30f, color2 = t.accent2),
                L(TIME, 0xFFFFFFFF, size = 100f), L(DATE, t.fg, y = 340f, size = 24f),
            )
        },
        "Stickers" to { t ->
            doc(
                t, L(ICON, 0xFFFFFFFF, x = 118f, y = 130f, size = 84f, text = t.emoji, rotation = -12f),
                L(ICON, 0xFFFFFFFF, x = 335f, y = 122f, size = 66f, text = t.emoji2, rotation = 10f),
                L(ICON, t.accent, x = 330f, y = 345f, size = 50f, text = t.glyph, rotation = 15f),
                L(TIME, t.fg, y = 235f, size = 100f), L(DATE, t.accent, y = 310f, size = 24f),
            )
        },
        "Analog emoji" to { t ->
            doc(
                t, L(TICKS, t.accent, size = 218f, height = 20f, thickness = 5f), L(ICON, 0xFFFFFFFF, y = 125f, size = 72f, text = t.emoji),
                L(DATE, t.soft, y = 330f, size = 22f), L(HANDS, t.fg, size = 165f, thickness = 9f, color2 = t.accent2, extra = true),
            )
        },
        "Pill" to { t ->
            doc(
                t, L(RECT, t.track, y = 215f, size = 330f, height = 130f, thickness = 65f), L(TIME, t.fg, y = 215f, size = 96f, weight = 3),
                L(ICON, t.accent, y = 105f, size = 48f, text = t.glyph), L(DATE, t.accent, y = 320f, size = 26f),
            )
        },
    )

    /** (group, [(name, face)]) for the Add tab. */
    val groups: List<Pair<String, List<Pair<String, FaceDoc>>>> =
        listOf("מקוריים" to Originals.all) + layouts.map { (name, f) -> name to Themes.map { it.name to f(it) } }
}

/** Ready-made phone widgets in four sizes, every layout in every [Themes] mood. */
object WidgetPresets {
    class Size(val title: String, val w: Float, val h: Float, val corner: Float)

    val SMALL = Size("קטן 2×2", 400f, 400f, 64f)
    val WIDE = Size("רחב 4×2", 800f, 400f, 64f)
    val LARGE = Size("גדול 4×4", 800f, 800f, 72f)
    val SLIM = Size("פס 4×1", 800f, 200f, 56f)
    val sizes = listOf(SMALL, WIDE, LARGE, SLIM)

    private fun w(s: Size, t: Theme, vararg layers: Layer) = doc(t, *layers, w = s.w, h = s.h, corner = s.corner)

    private val layouts: List<Pair<String, (Theme) -> FaceDoc>> = listOf(
        "Hero" to { t ->
            w(WIDE, t, L(TIME, t.fg, x = 270f, y = 170f, size = 130f), L(DATE, t.accent, x = 270f, y = 275f, size = 32f, weight = 3),
                L(CIRCLE, (t.accent and 0xFFFFFF) or 0x33000000, x = 630f, y = 200f, size = 135f), L(ICON, 0xFFFFFFFF, x = 630f, y = 200f, size = 210f, text = t.emoji))
        },
        "Clock" to { t -> w(SMALL, t, L(TIME, t.fg, x = 200f, y = 180f, size = 110f), L(DATE, t.accent, x = 200f, y = 270f, size = 30f, weight = 3)) },
        "Emoji" to { t ->
            w(SMALL, t, L(ICON, 0xFFFFFFFF, x = 200f, y = 135f, size = 150f, text = t.emoji), L(TIME, t.fg, x = 200f, y = 280f, size = 80f),
                L(DATE, t.soft, x = 200f, y = 345f, size = 24f, weight = 3))
        },
        "Banner" to { t ->
            w(SLIM, t, L(ICON, t.accent, x = 95f, y = 100f, size = 76f, text = t.glyph), L(TIME, t.fg, x = 340f, y = 100f, size = 96f),
                L(DATE, t.soft, x = 640f, y = 100f, size = 34f, weight = 3))
        },
        "Dial" to { t ->
            w(SMALL, t, L(ARC, t.track, x = 200f, y = 200f, size = 155f, thickness = 22f, height = 360f),
                L(ARC, t.accent, x = 200f, y = 200f, size = 155f, thickness = 22f, angle = 225f, height = 270f, color2 = t.accent2),
                L(TIME, t.fg, x = 200f, y = 195f, size = 78f), L(DATE, t.accent, x = 200f, y = 265f, size = 22f, weight = 3))
        },
        "Quote" to { t ->
            w(WIDE, t, L(TEXT, t.fg, x = 400f, y = 150f, size = (560f / t.quote.length).coerceAtMost(60f), text = t.quote),
                L(RECT, t.accent, x = 400f, y = 230f, size = 90f, height = 8f, thickness = 4f, color2 = t.accent2),
                L(TIME, t.soft, x = 400f, y = 300f, size = 46f, weight = 2))
        },
        "Big emoji" to { t ->
            w(LARGE, t, L(CIRCLE, (t.accent and 0xFFFFFF) or 0x33000000, x = 400f, y = 320f, size = 230f),
                L(ICON, 0xFFFFFFFF, x = 400f, y = 320f, size = 300f, text = t.emoji), L(TIME, t.fg, x = 400f, y = 610f, size = 120f),
                L(DATE, t.accent, x = 400f, y = 710f, size = 34f, weight = 3))
        },
        "Stacked" to { t ->
            w(WIDE, t, L(TIME, t.fg, x = 400f, y = 160f, size = 150f),
                L(RECT, t.accent, x = 400f, y = 300f, size = 260f, height = 64f, thickness = 32f, color2 = t.accent2),
                L(DATE, 0xFFFFFFFF, x = 400f, y = 300f, size = 32f))
        },
        "Stickers" to { t ->
            w(LARGE, t, L(ICON, 0xFFFFFFFF, x = 210f, y = 210f, size = 200f, text = t.emoji, rotation = -10f),
                L(ICON, 0xFFFFFFFF, x = 600f, y = 190f, size = 150f, text = t.emoji2, rotation = 12f),
                L(ICON, t.accent, x = 620f, y = 600f, size = 120f, text = t.glyph, rotation = 14f),
                L(ICON, 0xFFFFFFFF, x = 180f, y = 610f, size = 120f, text = "fluent-emoji:sparkles"),
                L(TIME, t.fg, x = 400f, y = 410f, size = 130f))
        },
        "Pill" to { t ->
            w(SLIM, t, L(RECT, t.track, x = 400f, y = 100f, size = 740f, height = 150f, thickness = 75f),
                L(ICON, 0xFFFFFFFF, x = 115f, y = 100f, size = 110f, text = t.emoji), L(TIME, t.fg, x = 380f, y = 100f, size = 90f),
                L(DATE, t.accent, x = 640f, y = 100f, size = 32f, weight = 3))
        },
    )

    val groups: List<Pair<String, List<Pair<String, FaceDoc>>>> = layouts.map { (name, f) -> name to Themes.map { it.name to f(it) } }

    val starter get() = groups.first().second.first().second
}
/** The first hand-made faces. */
private object Originals {
    private fun face(bg: Long, vararg layers: Layer) = FaceDoc(bg, layers.mapIndexed { i, l -> l.copy(id = i + 1L) })
    private fun l(kind: LayerKind, color: Long, f: Layer.() -> Layer = { this }) = Layer.create(kind, 0, color).copy(color = color).f()

    val all: List<Pair<String, FaceDoc>> = listOf(
        "Orbit" to FaceDoc.Starter,
        "Minimal" to face(
            0xFF000000,
            l(LayerKind.TIME, 0xFFFFFFFF) { copy(size = 120f, weight = 0) },
            l(LayerKind.DATE, 0xFFB8BCC8) { copy(y = 300f, size = 26f, weight = 1) },
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
        "Analog" to face(
            0xFF0E1015,
            l(LayerKind.TICKS, 0xFF6B7080) { copy(count = 60, height = 8f, thickness = 2f) },
            l(LayerKind.TICKS, 0xFFFFFFFF) { copy(count = 12, height = 26f, thickness = 6f) },
            l(LayerKind.DATE, 0xFF8B7BFF) { copy(x = 320f, y = 225f, size = 22f) },
            l(LayerKind.HANDS, 0xFFFFFFFF) { copy(color2 = 0xFFFF6A4D) },
        ),
        "Gradient" to face(
            0xFF000000,
            l(LayerKind.CIRCLE, 0xFF6C5CE7) { copy(y = 225f, size = 225f, color2 = 0xFFFF3D8B) },
            l(LayerKind.TIME, 0xFFFFFFFF) { copy(size = 100f) },
            l(LayerKind.DATE, 0xCCFFFFFF) { copy(y = 295f, size = 24f) },
        ),
        "Weather" to face(
            0xFF101C33,
            l(LayerKind.ARC, 0xFF1E3358) { copy(size = 205f, thickness = 10f, angle = 0f, height = 360f) },
            l(LayerKind.ARC, 0xFFFFD166) { copy(size = 205f, thickness = 10f, angle = 270f, height = 140f, color2 = 0xFFFF7A59) },
            l(LayerKind.ICON, 0xFFFFD166) { copy(y = 115f, size = 64f, text = "WbSunny") },
            l(LayerKind.TIME, 0xFFFFFFFF) { copy(y = 225f, size = 96f) },
            l(LayerKind.DATE, 0xFF8FB3FF) { copy(y = 310f, size = 24f) },
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
