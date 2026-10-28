package com.ohav.pixelforge.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Built-in icon assets (Material Icons, bundled with the app, free and offline). 24-unit viewport. */
@Suppress("DEPRECATION")
object IconLibrary {
    val categories: List<Pair<String, List<Pair<String, ImageVector>>>> = listOf(
        "סמלים" to listOf(
            "Favorite" to Icons.Filled.Favorite, "FavoriteBorder" to Icons.Filled.FavoriteBorder, "Star" to Icons.Filled.Star,
            "StarBorder" to Icons.Filled.StarBorder, "Bolt" to Icons.Filled.Bolt, "Whatshot" to Icons.Filled.Whatshot,
            "AutoAwesome" to Icons.Filled.AutoAwesome, "Flare" to Icons.Filled.Flare, "Verified" to Icons.Filled.Verified,
            "Shield" to Icons.Filled.Shield, "ThumbUp" to Icons.Filled.ThumbUp, "EmojiEmotions" to Icons.Filled.EmojiEmotions,
            "Mood" to Icons.Filled.Mood, "Pets" to Icons.Filled.Pets, "MusicNote" to Icons.Filled.MusicNote,
            "Headphones" to Icons.Filled.Headphones, "Lightbulb" to Icons.Filled.Lightbulb, "Anchor" to Icons.Filled.Anchor,
            "Cake" to Icons.Filled.Cake, "Grade" to Icons.Filled.Grade,
        ),
        "מזג אוויר" to listOf(
            "WbSunny" to Icons.Filled.WbSunny, "NightsStay" to Icons.Filled.NightsStay, "Cloud" to Icons.Filled.Cloud,
            "AcUnit" to Icons.Filled.AcUnit, "Umbrella" to Icons.Filled.Umbrella, "FlashOn" to Icons.Filled.FlashOn,
            "WbTwilight" to Icons.Filled.WbTwilight, "DarkMode" to Icons.Filled.DarkMode, "LightMode" to Icons.Filled.LightMode,
            "Thermostat" to Icons.Filled.Thermostat, "Opacity" to Icons.Filled.Opacity, "Bedtime" to Icons.Filled.Bedtime,
        ),
        "כושר" to listOf(
            "DirectionsRun" to Icons.Filled.DirectionsRun, "DirectionsWalk" to Icons.Filled.DirectionsWalk,
            "DirectionsBike" to Icons.Filled.DirectionsBike, "Pool" to Icons.Filled.Pool, "FitnessCenter" to Icons.Filled.FitnessCenter,
            "SelfImprovement" to Icons.Filled.SelfImprovement, "SportsSoccer" to Icons.Filled.SportsSoccer,
            "SportsBasketball" to Icons.Filled.SportsBasketball, "SportsEsports" to Icons.Filled.SportsEsports,
            "LocalFireDepartment" to Icons.Filled.LocalFireDepartment, "Spa" to Icons.Filled.Spa,
        ),
        "טבע ואוכל" to listOf(
            "LocalFlorist" to Icons.Filled.LocalFlorist, "Eco" to Icons.Filled.Eco, "Park" to Icons.Filled.Park,
            "LocalCafe" to Icons.Filled.LocalCafe, "Restaurant" to Icons.Filled.Restaurant, "LocalBar" to Icons.Filled.LocalBar,
        ),
        "מערכת" to listOf(
            "BatteryFull" to Icons.Filled.BatteryFull, "BatteryChargingFull" to Icons.Filled.BatteryChargingFull,
            "Bluetooth" to Icons.Filled.Bluetooth, "Wifi" to Icons.Filled.Wifi, "SignalCellularAlt" to Icons.Filled.SignalCellularAlt,
            "Notifications" to Icons.Filled.Notifications, "Alarm" to Icons.Filled.Alarm, "Timer" to Icons.Filled.Timer,
            "Watch" to Icons.Filled.Watch, "Phone" to Icons.Filled.Phone, "Email" to Icons.Filled.Email,
            "CalendarToday" to Icons.Filled.CalendarToday, "Schedule" to Icons.Filled.Schedule, "Settings" to Icons.Filled.Settings,
            "Lock" to Icons.Filled.Lock, "Home" to Icons.Filled.Home, "Work" to Icons.Filled.Work, "School" to Icons.Filled.School,
        ),
        "מסע" to listOf(
            "Flight" to Icons.Filled.Flight, "DirectionsCar" to Icons.Filled.DirectionsCar, "Train" to Icons.Filled.Train,
            "Place" to Icons.Filled.Place, "Explore" to Icons.Filled.Explore, "Navigation" to Icons.Filled.Navigation,
            "Public" to Icons.Filled.Public, "Language" to Icons.Filled.Language,
        ),
        "צורות" to listOf(
            "Circle" to Icons.Filled.Circle, "Brightness1" to Icons.Filled.Brightness1, "RadioButtonUnchecked" to Icons.Filled.RadioButtonUnchecked,
            "Adjust" to Icons.Filled.Adjust, "Album" to Icons.Filled.Album, "CropSquare" to Icons.Filled.CropSquare,
            "ChangeHistory" to Icons.Filled.ChangeHistory,
        ),
    )

    private val byName = categories.flatMap { it.second }.toMap()
    private val cache = HashMap<String, List<Path>>()

    fun vector(name: String): ImageVector? = byName[name]

    /** The icon's fill paths in its 24x24 viewport (cached). */
    fun paths(name: String): List<Path> = cache.getOrPut(name) {
        val v = byName[name] ?: return@getOrPut emptyList()
        buildList {
            fun walk(g: VectorGroup) {
                for (node in g) when (node) {
                    is VectorGroup -> walk(node)
                    is VectorPath -> add(PathParser().addPathNodes(node.pathData).toPath().apply { fillType = node.pathFillType })
                }
            }
            walk(v.root)
        }
    }

    const val VIEWPORT = 24f
}

/** Imported pictures and the PNGs sent to the watch's image slots. */
object ImageAssets {
    fun dir(context: Context) = File(context.filesDir, "faces/images").apply { mkdirs() }

    /** Copies a picked picture (max 600px side) into app storage. Returns (file name, width / height). */
    fun import(context: Context, uri: Uri): Pair<String, Float> {
        val src = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
            val s = 600f / max(info.size.width, info.size.height)
            if (s < 1) d.setTargetSize((info.size.width * s).roundToInt(), (info.size.height * s).roundToInt())
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val name = "img_${System.currentTimeMillis()}.png"
        File(dir(context), name).outputStream().use { src.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return name to src.width.toFloat() / src.height
    }

    fun load(context: Context, name: String): Bitmap? = File(dir(context), name).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    /** Renders an ICON or IMAGE layer at its on-watch pixel size as PNG bytes. */
    fun render(context: Context, l: Layer): ByteArray {
        val (w, h) = l.box()
        val bmp = Bitmap.createBitmap(w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (l.kind == LayerKind.ICON) {
            paint.color = l.color.toInt()
            canvas.scale(bmp.width / IconLibrary.VIEWPORT, bmp.height / IconLibrary.VIEWPORT)
            IconLibrary.paths(l.text).forEach { canvas.drawPath(it.asAndroidPath(), paint) }
        } else {
            val src = load(context, l.text) ?: return png(bmp)
            paint.alpha = (l.color ushr 24 and 0xFF).toInt()
            val clip = android.graphics.Path().apply {
                addRoundRect(RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()), l.thickness, l.thickness, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(clip)
            canvas.drawBitmap(src, centerCrop(src.width, src.height, bmp.width.toFloat(), bmp.height.toFloat()), RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()), paint)
        }
        return png(bmp)
    }

    /** Source rectangle that fills a w x h box without distortion. */
    fun centerCrop(sw: Int, sh: Int, w: Float, h: Float): android.graphics.Rect {
        val s = max(w / sw, h / sh)
        val cw = (w / s).roundToInt(); val ch = (h / s).roundToInt()
        val x = (sw - cw) / 2; val y = (sh - ch) / 2
        return android.graphics.Rect(x, y, x + cw, y + ch)
    }

    private fun png(b: Bitmap) = ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
}
