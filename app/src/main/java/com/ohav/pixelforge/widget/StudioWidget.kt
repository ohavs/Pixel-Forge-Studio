package com.ohav.pixelforge.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.absolutePadding
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import com.ohav.pixelforge.MainActivity
import com.ohav.pixelforge.R
import com.ohav.pixelforge.editor.DocAssets
import com.ohav.pixelforge.editor.FaceDoc
import com.ohav.pixelforge.editor.ImageAssets
import com.ohav.pixelforge.editor.Iconify
import com.ohav.pixelforge.editor.Layer
import com.ohav.pixelforge.editor.LayerKind
import com.ohav.pixelforge.editor.drawDoc
import com.ohav.pixelforge.editor.isLive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Home-screen widget made in the Widget Studio. Hybrid: the design is baked into one picture,
 * and TIME/DATE layers sit on top as TextClocks, which the launcher keeps current by itself (no
 * wake-ups, no battery cost). Each placed widget keeps its own copy of a design.
 */
class StudioWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val doc = StudioWidgets.design(context, GlanceAppWidgetManager(context).getAppWidgetId(id))
        Iconify.ensure(context, doc.layers.filter { it.kind == LayerKind.ICON }.map { it.text })
        val picture = withContext(Dispatchers.Default) { StudioWidgets.render(context, doc) }
        provideContent { Content(doc, picture) }
    }

    @Composable
    private fun Content(doc: FaceDoc, picture: Bitmap) {
        val ctx = LocalContext.current
        val box = LocalSize.current
        // The design keeps its aspect and is centred in whatever cells the launcher gives it.
        val s = min(box.width.value / doc.w, box.height.value / doc.h)
        val dw = doc.w * s
        val dh = doc.h * s
        Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()), contentAlignment = Alignment.Center) {
            Box(GlanceModifier.size(dw.dp, dh.dp)) {
                Image(ImageProvider(picture), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                doc.layers.filter { it.visible && it.isLive }.forEach { l ->
                    // A box centred on the layer: as wide as fits on both sides, so text centres on x.
                    val half = min(l.x, doc.w - l.x) * s
                    val h = l.box().second * s
                    val top = (l.y * s - h / 2).coerceAtLeast(0f)
                    Box(
                        GlanceModifier.fillMaxSize().absolutePadding(
                            left = (l.x * s - half).dp, top = top.dp,
                            right = (dw - l.x * s - half).coerceAtLeast(0f).dp, bottom = (dh - top - h).coerceAtLeast(0f).dp,
                        ),
                    ) { AndroidRemoteViews(clock(ctx, l, s), GlanceModifier.fillMaxSize()) }
                }
            }
        }
    }

    private fun clock(ctx: Context, l: Layer, scale: Float): RemoteViews {
        val rv = RemoteViews(ctx.packageName, ClockLayouts[l.weight.coerceIn(ClockLayouts.indices)])
        rv.setTextColor(R.id.clock, l.color.toInt())
        rv.setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_DIP, l.size * scale)
        val (f12, f24) = when {
            l.kind == LayerKind.DATE -> "EEE d" to "EEE d"
            l.extra -> "hh:mm:ss" to "HH:mm:ss"
            else -> "hh:mm" to "HH:mm"
        }
        rv.setCharSequence(R.id.clock, "setFormat12Hour", f12)
        rv.setCharSequence(R.id.clock, "setFormat24Hour", f24)
        return rv
    }

    private companion object {
        /** One TextClock layout per font weight (RemoteViews can't set a typeface). */
        val ClockLayouts = listOf(R.layout.wclock_0, R.layout.wclock_1, R.layout.wclock_2, R.layout.wclock_3, R.layout.wclock_4)
    }
}

class StudioWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudioWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { StudioWidgets.placed(context, it).delete() }
    }
}

/** Designs of placed widgets, pinning and rendering. */
object StudioWidgets {
    fun placed(ctx: Context, appWidgetId: Int) = File(ctx.filesDir, "widgets/placed/$appWidgetId.json")
    private fun current(ctx: Context) = File(ctx.filesDir, "widgets/current.json")

    /** A widget's design. A new widget takes the design open in the studio when it first draws. */
    fun design(ctx: Context, appWidgetId: Int): FaceDoc {
        val f = placed(ctx, appWidgetId)
        if (!f.exists()) current(ctx).takeIf { it.exists() }?.let { f.parentFile?.mkdirs(); it.copyTo(f) }
        return runCatching { FaceDoc.fromJson(f.readText()) }.getOrDefault(com.ohav.pixelforge.editor.WidgetPresets.starter)
    }

    /** Asks the launcher to place a new widget showing [doc]. Returns a status line. */
    suspend fun pin(ctx: Context, doc: FaceDoc): String {
        current(ctx).apply { parentFile?.mkdirs() }.writeText(doc.toJson())
        val ok = runCatching { GlanceAppWidgetManager(ctx).requestPinGlanceAppWidget(StudioWidgetReceiver::class.java) }.getOrDefault(false)
        return if (ok) "בחר מקום במסך הבית" else "המשגר לא תומך בהוספה ישירה — הוסף ווידג'ט \"Orbit\" מרשימת הווידג'טים"
    }

    /** Puts [doc] on every placed widget. */
    suspend fun applyToAll(ctx: Context, doc: FaceDoc): String {
        val ids = GlanceAppWidgetManager(ctx).getGlanceIds(StudioWidget::class.java)
        if (ids.isEmpty()) return "אין עדיין ווידג'טים במסך הבית"
        ids.forEach { id ->
            placed(ctx, GlanceAppWidgetManager(ctx).getAppWidgetId(id)).apply { parentFile?.mkdirs() }.writeText(doc.toJson())
        }
        StudioWidget().updateAll(ctx)
        return "עודכנו ${ids.size} ווידג'טים"
    }

    /** Bakes everything except live text into a picture (max 900px side, under the RemoteViews bitmap limit). */
    fun render(ctx: Context, doc: FaceDoc): Bitmap {
        val px = 900f / max(doc.w, doc.h)
        val w = (doc.w * px).roundToInt()
        val h = (doc.h * px).roundToInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val density = Density(ctx)
        val measurer = TextMeasurer(createFontFamilyResolver(ctx), density, LayoutDirection.Ltr)
        val assets = DocAssets(image = { ImageAssets.load(ctx, it)?.asImageBitmap() }, icon = { Iconify.peek(ctx, it) })
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp)), Size(w.toFloat(), h.toFloat())) {
            drawDoc(doc, Color(doc.background), measurer, LocalDateTime.now(), assets, live = false)
        }
        return bmp
    }
}
