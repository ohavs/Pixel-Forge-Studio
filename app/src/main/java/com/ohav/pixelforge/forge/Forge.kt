package com.ohav.pixelforge.forge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.Typeface
import com.ohav.pixelforge.editor.DataSource
import com.ohav.pixelforge.editor.FACE_SIZE
import com.ohav.pixelforge.editor.FaceDoc
import com.ohav.pixelforge.editor.ImageAssets
import com.ohav.pixelforge.editor.Layer
import com.ohav.pixelforge.editor.LayerKind
import java.io.File
import kotlin.math.max
import kotlin.random.Random

/** A colorway: dial [base]→[base2], [metal] hands/indices, [accent]s, [lume], printed [ink], date [window]. */
class Pal(
    val name: String, val words: String,
    base: Long, base2: Long, metal: Long, accent: Long, accent2: Long = accent, lume: Long = 0xFFEAF6E4, ink: Long = metal, window: Long = 0xFFF2F0EA,
) {
    val base = base.toInt(); val base2 = base2.toInt(); val metal = metal.toInt(); val accent = accent.toInt()
    val accent2 = accent2.toInt(); val lume = lume.toInt(); val ink = ink.toInt(); val window = window.toInt()
    /** Readable text on the date window. */
    val windowInk: Int get() {
        val lum = .3f * ((window shr 16) and 0xFF) + .59f * ((window shr 8) and 0xFF) + .11f * (window and 0xFF)
        return (if (lum > 128) 0xFF15171A else 0xFFF2F0EA).toInt()
    }
}

enum class Style(
    val title: String, val fonts: List<String>, val fonts2: List<String>, val palettes: List<Pal>,
    val textures: List<Texture>, val photoCategories: List<String>, val hands: List<String>, val subs: List<String>,
) {
    LUXURY(
        "יוקרה", listOf("Cinzel", "Playfair Display", "Cormorant Garamond", "Bodoni Moda", "Marcellus", "Italiana", "Cormorant SC", "Prata"),
        listOf("Montserrat", "Josefin Sans", "Raleway", "Jost"),
        listOf(
            Pal("Midnight", "deep midnight blue and polished silver", 0xFF123060, 0xFF040E26, 0xFFE6E2D6, 0xFFC9A45C),
            Pal("Emerald", "emerald green and warm gold", 0xFF0E4A38, 0xFF03170F, 0xFFE2C27C, 0xFFE2C27C, ink = 0xFFEBDDB8),
            Pal("Salmon", "salmon pink and blued steel", 0xFFEDB9A0, 0xFFC4846A, 0xFF1F2E4F, 0xFF1F3F7A, ink = 0xFF2A2730, window = 0xFFF7EFE8),
            Pal("Onyx", "black lacquer and yellow gold", 0xFF1A1A1C, 0xFF000000, 0xFFE0BE72, 0xFFE0BE72, ink = 0xFFE9DDBF),
            Pal("Opaline", "silver opaline and blued hands", 0xFFEDEDEA, 0xFFB9BAB6, 0xFF1C3264, 0xFF1C4FA0, ink = 0xFF22252B),
            Pal("Bordeaux", "burgundy red and rose gold", 0xFF651424, 0xFF24050C, 0xFFF0C9A8, 0xFFF0C9A8, ink = 0xFFF3E0D0),
        ),
        listOf(Texture.SUNBURST, Texture.GUILLOCHE, Texture.CLOUS, Texture.BRUSHED, Texture.SUNBURST),
        listOf("fabric", "metal", "wood"), listOf("dauphine", "leaf", "sword", "baton"),
        listOf("AUTOMATIC", "CHRONOMETER", "SWISS MADE", "PERPETUAL", "GRAND COMPLICATION"),
    ),
    SPORT(
        "ספורט", listOf("Oswald", "Barlow Condensed", "Rajdhani", "Teko", "Saira Condensed", "Bebas Neue", "Anton"),
        listOf("Barlow", "Roboto Condensed", "Saira", "Titillium Web"),
        listOf(
            Pal("Blaze", "matte black and blaze orange", 0xFF1B1D21, 0xFF07080A, 0xFFF2F2F2, 0xFFFF6A1A),
            Pal("Ultra", "raw titanium grey and safety yellow", 0xFF34383E, 0xFF15171A, 0xFFE9E9E9, 0xFFFFC21A),
            Pal("Navy", "navy blue and signal red", 0xFF102440, 0xFF050B14, 0xFFF0F0F0, 0xFFE53935),
            Pal("Field", "olive drab and desert sand", 0xFF353A28, 0xFF14160E, 0xFFE8DCC0, 0xFFD2A85F, lume = 0xFFF0E2B6),
            Pal("Glacier", "ice blue and cyan", 0xFF132632, 0xFF060D12, 0xFFEAF6FF, 0xFF00D1FF),
            Pal("Volt", "carbon black and neon lime", 0xFF141A15, 0xFF050A06, 0xFFF0F0F0, 0xFFB6FF3B),
        ),
        listOf(Texture.CARBON, Texture.HEX, Texture.BRUSHED, Texture.WAVES, Texture.CARBON),
        listOf("metal", "concrete", "rock"), listOf("arrow", "sword", "baton"),
        listOf("PROFESSIONAL", "1000M · 3300FT", "TITANIUM", "EXPEDITION", "TACTICAL"),
    ),
    ART(
        "אמנותי", listOf("Fraunces", "DM Serif Display", "Abril Fatface", "Righteous", "Poiret One", "Comfortaa", "Syne"),
        listOf("Outfit", "Manrope", "Sora", "Urbanist"),
        listOf(
            Pal("Nebula", "violet and hot pink nebula", 0xFF1A0B3D, 0xFF06020F, 0xFFFFFFFF, 0xFFFF3D8B, 0xFF00C2FF),
            Pal("Lagoon", "teal lagoon and deep blue", 0xFF042A2E, 0xFF010A0D, 0xFFFFFFFF, 0xFF2EE59D, 0xFF3D7BFF),
            Pal("Ember", "ember orange and gold dusk", 0xFF2B0F16, 0xFF0A0305, 0xFFFFFFFF, 0xFFFF7A59, 0xFFFFD166),
            Pal("Iris", "lavender and coral", 0xFF17142A, 0xFF07060E, 0xFFFFFFFF, 0xFF8B7BFF, 0xFFFF6A4D),
            Pal("Forest", "misty forest green and moonlight", 0xFF0D1F17, 0xFF030806, 0xFFFFFFFF, 0xFF9BE3B4, 0xFFE8F1FF),
        ),
        listOf(Texture.AURORA, Texture.WAVES, Texture.AURORA),
        listOf("rock", "terrain"), listOf("skeleton", "leaf", "baton"),
        listOf("", "", "ATELIER"),
    ),
    CYBER(
        "סייברפאנק", listOf("Orbitron", "Audiowide", "Michroma", "Chakra Petch", "Exo 2", "Oxanium", "Major Mono Display"),
        listOf("Share Tech Mono", "JetBrains Mono", "Space Mono", "VT323"),
        listOf(
            Pal("Neon", "electric cyan and magenta neon", 0xFF071022, 0xFF02040A, 0xFFE8FBFF, 0xFF00F0FF, 0xFFFF2BD6, ink = 0xFFBFF8FF),
            Pal("Matrix", "terminal green on black", 0xFF03140A, 0xFF010603, 0xFFD8FFE2, 0xFF3BFF6E, 0xFF00C2FF, ink = 0xFFB8FFC9),
            Pal("Amber", "amber CRT glow", 0xFF140C02, 0xFF050300, 0xFFFFE9C2, 0xFFFFB000, 0xFFFF5A1F, ink = 0xFFFFDDA0),
            Pal("Synth", "synthwave pink and purple", 0xFF170826, 0xFF06020D, 0xFFFFE6F7, 0xFFFF3D8B, 0xFF7A5CFF, ink = 0xFFFFD0EE),
            Pal("Alert", "red alert on gunmetal", 0xFF14080A, 0xFF050203, 0xFFFFE3E3, 0xFFFF2B3A, 0xFFFFB000, ink = 0xFFFFC9CC),
        ),
        listOf(Texture.GRID, Texture.HEX, Texture.GRID, Texture.WAVES),
        listOf("metal", "floor"), listOf("cyber", "baton", "skeleton"),
        listOf("SYS//ONLINE", "NEURAL LINK", "CORE v2.6", "NODE 07"),
    ),
}

enum class BgSource(val title: String) { AUTO("אוטומטי"), PROCEDURAL("גימור"), TEXTURE("טקסטורה"), AI("AI") }
enum class ClockType(val title: String) { AUTO("מעורב"), ANALOG("אנלוגי"), DIGITAL("דיגיטלי") }

/** What the user controls; everything else varies between the generated variations. */
data class Recipe(
    val style: Style = Style.LUXURY,
    val bg: BgSource = BgSource.AUTO,
    val type: ClockType = ClockType.AUTO,
    val complexity: Int = 2,
    val data: Boolean = true,
    val seconds: Boolean = true,
    val palette: Int = -1,
    val font: String = "",
    val brand: String = "ORBIT",
    val prompt: String = "",
)

/** One generated design plus what went differently than asked (e.g. AI unavailable). */
class Forged(val doc: FaceDoc, val note: String = "")

object Forge {
    /**
     * Builds variation [variant] of [recipe] on a canvas like [canvas] (face or widget size).
     * Downloads fonts / textures / AI art as needed and falls back to procedural finishes offline.
     */
    suspend fun generate(ctx: Context, recipe: Recipe, canvas: FaceDoc, seed: Int, variant: Int): Forged {
        val rnd = Random(seed * 31 + variant)
        val st = recipe.style
        val pal = st.palettes[if (recipe.palette in st.palettes.indices) recipe.palette else (variant + seed).mod(st.palettes.size)]
        val face = canvas.round
        val fontName = recipe.font.ifEmpty { st.fonts[rnd.nextInt(st.fonts.size)] }
        val font = GoogleFonts.typeface(ctx, fontName) ?: if (st == Style.LUXURY) Typeface.SERIF else Typeface.DEFAULT_BOLD
        val font2 = GoogleFonts.typeface(ctx, st.fonts2[rnd.nextInt(st.fonts2.size)]) ?: Typeface.DEFAULT
        val w = canvas.w; val h = canvas.h
        val scale = if (face) 2f else 900f / max(w, h)
        val pw = (w * scale).toInt(); val ph = (h * scale).toInt()

        var note = ""
        val source = when (recipe.bg) {
            BgSource.AUTO -> when {
                st == Style.ART && AiImages.remaining(ctx) > 0 -> BgSource.AI
                rnd.nextFloat() < .3f -> BgSource.TEXTURE
                else -> BgSource.PROCEDURAL
            }
            else -> recipe.bg
        }
        var photo: Bitmap? = null
        if (source == BgSource.AI) photo = runCatching { AiImages.generate(ctx, prompt(recipe, pal, face, w / h), pw, ph, seed + variant) }
            .onFailure { note = "AI: ${it.message}" }.getOrNull()
        if (source == BgSource.TEXTURE) photo = Textures.pick(ctx, st.photoCategories, seed * 7 + variant).also { if (it == null) note = "טקסטורות לא זמינות (אין רשת?)" }
        val texture = if (photo != null) Texture.PHOTO else st.textures[rnd.nextInt(st.textures.size)]
        val tint = if (source == BgSource.TEXTURE) .62f else 0f

        val art = mutableListOf<Layer>()
        val spec = if (face) faceLayout(recipe, pal, rnd, font, font2, texture, photo, tint, seed + variant, art)
        else widgetLayout(recipe, pal, rnd, font, font2, texture, photo, tint, seed + variant, canvas, art)
        val bmp = DialArt.render(spec, scale)
        val name = "forge_${System.currentTimeMillis()}_$variant.png"
        File(ImageAssets.dir(ctx), name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val dial = Layer(1, LayerKind.IMAGE, w / 2, h / 2, size = w, height = h, thickness = if (face) w / 2 else canvas.corner, text = name, locked = true)
        val layers = (listOf(dial) + art).mapIndexed { i, l -> l.copy(id = i + 1L) }
        return Forged(canvas.copy(background = pal.base2.toLong() and 0xFFFFFFFF, background2 = 0, layers = layers), note)
    }

    private fun prompt(r: Recipe, pal: Pal, face: Boolean, aspect: Float): String {
        val subject = r.prompt.trim()
        val shape = if (face) "centered radial composition for a round smartwatch face" else
            "wide ${"%.1f".format(aspect)}:1 home-screen widget banner, main subject on the right, calm darker space on the left for text"
        val clean = "no text, no letters, no numbers, no logos, no watch hands"
        return when (r.style) {
            Style.LUXURY -> "Macro top-down photograph of a luxury watch dial surface, ${subject.ifEmpty { "exquisite guilloche enamel" }}, ${pal.words}, soft studio light, $shape, $clean"
            Style.SPORT -> "Top-down rugged sport watch dial texture, ${subject.ifEmpty { "carbon and titanium" }}, ${pal.words}, technical materials, $shape, $clean"
            Style.ART -> "${subject.ifEmpty { "dreamlike surreal landscape" }}, ${pal.words}, rich detailed digital artwork, darker edges, $shape, $clean"
            Style.CYBER -> "${subject.ifEmpty { "neon cyberpunk city at night" }}, ${pal.words}, futuristic HUD aesthetic, glowing details on dark, $shape, $clean"
        }
    }

    private fun L(kind: LayerKind, x: Float, y: Float, size: Float, color: Int, vararg mods: (Layer) -> Layer): Layer =
        mods.fold(Layer(0, kind, x, y, size = size, color = color.toLong() and 0xFFFFFFFF)) { l, m -> m(l) }

    // ---------- watch faces ----------

    private fun faceLayout(
        r: Recipe, pal: Pal, rnd: Random, font: Typeface, font2: Typeface, texture: Texture, photo: Bitmap?, tint: Float, seed: Int, out: MutableList<Layer>,
    ): ArtSpec {
        val st = r.style
        val c = FACE_SIZE / 2
        val digital = when (r.type) {
            ClockType.ANALOG -> false
            ClockType.DIGITAL -> true
            ClockType.AUTO -> rnd.nextFloat() < when (st) { Style.LUXURY -> 0f; Style.SPORT -> .35f; Style.ART -> .4f; Style.CYBER -> .6f }
        }
        val cx = r.complexity
        val bezel = when {
            st == Style.SPORT && (cx == 3 || (cx == 2 && rnd.nextBoolean())) -> listOf(Bezel.DIVER, Bezel.COMPASS, Bezel.HOURS24)[rnd.nextInt(3)]
            st == Style.CYBER && cx >= 2 -> Bezel.SEGMENT
            st == Style.LUXURY && cx == 3 && !digital -> Bezel.TACHY
            else -> Bezel.NONE
        }
        val R = if (bezel != Bezel.NONE) c * .82f else c
        val track = when (st) {
            Style.LUXURY -> listOf(Track.RAILWAY, Track.FINE, Track.MINUTES)[rnd.nextInt(3)]
            Style.SPORT -> if (bezel == Bezel.NONE) Track.MINUTES else listOf(Track.MINUTES, Track.NONE)[rnd.nextInt(2)]
            Style.ART -> if (cx >= 2) Track.FINE else Track.NONE
            Style.CYBER -> if (cx >= 2) Track.FINE else Track.NONE
        }
        val metal = pal.metal
        val subdials = mutableListOf<Subdial>()
        val meters = mutableListOf<Meter>()
        val windows = mutableListOf<RectF>()
        val skip = mutableSetOf<Int>()
        val labels = mutableListOf<Triple<String, Float, Float>>()
        var indices: Indices
        var brandOn = r.brand.isNotBlank() && st != Style.ART
        var sub = st.subs[rnd.nextInt(st.subs.size)]

        if (!digital) {
            indices = when (st) {
                Style.LUXURY -> listOf(Indices.BATON, Indices.ROMAN, Indices.ARABIC, Indices.BATON)[rnd.nextInt(4)]
                Style.SPORT -> listOf(Indices.DIVER, Indices.DIVER, Indices.ARABIC, Indices.DOTS)[rnd.nextInt(4)]
                Style.ART -> listOf(Indices.DOTS, Indices.NONE, Indices.BATON)[rnd.nextInt(3)]
                Style.CYBER -> Indices.HUD
            }
            val n = when (cx) { 1 -> rnd.nextInt(2); 2 -> 1 + rnd.nextInt(2); else -> 2 + rnd.nextInt(2) }.let { if (st == Style.ART) minOf(it, 1) else it }
            val pos = when (n) { 0 -> emptyList(); 1 -> listOf(6); 2 -> listOf(3, 9); else -> listOf(3, 6, 9) }
            val dataSources = if (r.data) mutableListOf(DataSource.BATTERY, DataSource.STEPS, DataSource.HEART).apply { shuffle(rnd) } else mutableListOf()
            pos.forEachIndexed { i, hr ->
                val src = if (hr == 6 || hr == 9 && n == 2 || dataSources.isEmpty()) DataSource.SECONDS else dataSources.removeAt(0)
                val a = Math.toRadians(hr * 30.0 - 90)
                val d = R * .46f
                val x = c + kotlin.math.cos(a).toFloat() * d; val y = c + kotlin.math.sin(a).toFloat() * d
                val (start, sweep) = if (src == DataSource.SECONDS) 0f to 360f else 225f to 270f
                val sd = Subdial(x, y, R * .21f, src, start, sweep)
                subdials += sd
                if (indices == Indices.ARABIC || indices == Indices.ROMAN) skip += hr % 12
                out += L(LayerKind.GAUGE, x, y, sd.r * .82f, if (st == Style.LUXURY) metal else pal.accent, {
                    it.copy(thickness = 2.4f, angle = start, height = sweep, extra = true, text = src.name, color2 = metal.toLong() and 0xFFFFFFFF)
                })
            }
            if (cx >= 2 && st != Style.ART && st != Style.CYBER && 3 !in pos) {
                val wx = c + R * .62f
                windows += RectF(wx - 18f, c - 13f, wx + 18f, c + 13f)
                skip += 3
                out += L(LayerKind.DATA, wx, c, 19f, pal.windowInk, { it.copy(text = DataSource.DAY.name, weight = 3) })
            }
            if (6 in pos || 12 in pos) sub = "" else if (subdials.isEmpty() && st != Style.CYBER) {
                // No sub-dials: a printed line low on the dial balances the brand.
                labels += Triple(sub, c, c + R * .42f); sub = ""
            }
            val handStyle = st.hands[rnd.nextInt(st.hands.size)]
            out += L(LayerKind.HANDS, c, c, R * .8f, if (handStyle == "cyber") pal.ink else metal, {
                it.copy(thickness = when (st) { Style.SPORT -> 7.5f; Style.CYBER -> 5f; else -> 6f }, text = handStyle, extra = r.seconds, color2 = pal.accent.toLong() and 0xFFFFFFFF)
            })
        } else {
            indices = if (cx >= 3 && st != Style.ART) Indices.HUD else Indices.NONE
            val weight = when (st) { Style.LUXURY -> 1; Style.SPORT -> 4; Style.ART -> 1; Style.CYBER -> 2 }
            out += L(LayerKind.TIME, c, c - 18f, if (st == Style.SPORT) 104f else 96f, pal.ink, { it.copy(weight = weight, extra = r.seconds && st == Style.CYBER) })
            out += L(LayerKind.DATE, c, c + 58f, 26f, pal.accent, { it.copy(weight = 3) })
            if (r.data) {
                val mr = R * .86f
                val sources = listOf(
                    Triple(DataSource.STEPS, 215f, "DirectionsWalk"),
                    Triple(DataSource.BATTERY, 35f, "BatteryFull"),
                ) + if (cx >= 2) listOf(Triple(DataSource.HEART, 150f, "Favorite")) else emptyList()
                val sweep = if (cx >= 2) 60f else 110f
                sources.forEachIndexed { i, (src, start0, icon) ->
                    val start = if (cx >= 2) listOf(240f, 0f, 120f)[i] + 30f else start0
                    meters += Meter(c, c, mr, 9f, start, sweep, when (src) { DataSource.STEPS -> "STEPS"; DataSource.BATTERY -> "PWR"; else -> "BPM" }, icon)
                    out += L(LayerKind.GAUGE, c, c, mr, if (i == 1) pal.accent2 else pal.accent, { it.copy(thickness = 9f, angle = start, height = sweep, text = src.name) })
                }
                val dy = c + R * .52f
                if (cx >= 2) {
                    out += L(LayerKind.DATA, c - 70f, dy, 22f, pal.ink, { it.copy(text = DataSource.STEPS.name, weight = 3) })
                    out += L(LayerKind.DATA, c + 70f, dy, 22f, pal.ink, { it.copy(text = DataSource.HEART.name, weight = 3) })
                    labels += Triple("STEPS", c - 70f, dy + 20f); labels += Triple("BPM", c + 70f, dy + 20f)
                } else out += L(LayerKind.DATA, c, dy, 22f, pal.ink, { it.copy(text = DataSource.BATTERY.name, weight = 3) })
            }
            brandOn = brandOn && st != Style.SPORT
            sub = ""
        }
        return ArtSpec(
            FACE_SIZE, FACE_SIZE, true, 0f, pal, texture, photo, tint, indices, track, bezel, subdials, meters, windows, skip,
            if (brandOn) r.brand.uppercase() else "", if (brandOn) sub else "", font, font2, labels = labels, seed = seed,
        )
    }

    // ---------- widgets ----------

    private fun widgetLayout(
        r: Recipe, pal: Pal, rnd: Random, font: Typeface, font2: Typeface, texture: Texture, photo: Bitmap?, tint: Float, seed: Int, doc: FaceDoc, out: MutableList<Layer>,
    ): ArtSpec {
        val st = r.style
        val w = doc.w; val h = doc.h
        val weight = when (st) { Style.LUXURY -> 1; Style.SPORT -> 4; Style.ART -> 1; Style.CYBER -> 2 }
        val m = doc.corner * .9f + 8f
        val slim = w / h > 3
        val square = w / h < 1.3f
        val timeSize = when { slim -> h * .46f; square -> w * .25f; else -> h * .33f }
        val tx = if (square) w / 2 else m + timeSize * 1.45f
        val ty = if (slim) h / 2 else h * (if (square) .48f else .5f)
        out += L(LayerKind.TIME, tx, ty, timeSize, pal.ink, { it.copy(weight = weight) })
        if (slim) out += L(LayerKind.DATE, w * .72f, h / 2, h * .17f, pal.accent, { it.copy(weight = 3) })
        else out += L(LayerKind.DATE, tx, ty + timeSize * .8f, timeSize * .25f, pal.accent, { it.copy(weight = 3) })
        val brandSize = if (slim) 0f else minOf(w, h) * .065f
        val labels = mutableListOf<Triple<String, Float, Float>>()
        val sub = st.subs[rnd.nextInt(st.subs.size)]
        if (!slim && !square && sub.isNotEmpty()) labels += Triple(sub, m, h - m + 2f)
        return ArtSpec(
            w, h, false, doc.corner, pal, texture, photo, tint, font = font, font2 = font2,
            frame = when (st) { Style.LUXURY -> "luxe"; Style.SPORT -> "sport"; Style.ART -> "art"; Style.CYBER -> "cyber" },
            brand = if (brandSize > 0 && r.brand.isNotBlank()) r.brand.uppercase() else "",
            brandX = m, brandY = m + brandSize, brandSize = brandSize, labels = labels, seed = seed,
        )
    }
}
