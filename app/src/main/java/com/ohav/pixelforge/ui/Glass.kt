package com.ohav.pixelforge.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The studio's design language: iOS-like dark surfaces, frosted panels with a light rim, white
 * as the primary action color and one system blue for selection. Everything is a capsule or a
 * continuous rounded rect; nothing has a hard edge.
 */
object Ui {
    val Ink = Color(0xFF000000)
    val Sheet = Color(0xFF111113)
    val Raised = Color(0xFF1E1E21)
    val Raised2 = Color(0xFF2A2A2E)
    val Hairline = Color.White.copy(alpha = .08f)
    val Text1 = Color.White.copy(alpha = .96f)
    val Text2 = Color(0xFF8E8E93)
    val Blue = Color(0xFF0A84FF)
    val Danger = Color(0xFFFF453A)
    val Good = Color(0xFF30D158)

    val Title = 17.sp
    val Body = 15.sp
    val Caption = 12.sp
}

/** Forces the studio's dark scheme for the Material pieces still used (text fields, menus). */
@Composable
fun StudioDark(content: @Composable () -> Unit) {
    MaterialTheme(darkColorScheme(primary = Ui.Blue, background = Ui.Ink, surface = Ui.Sheet, surfaceVariant = Ui.Raised, onSurface = Ui.Text1, onSurfaceVariant = Ui.Text2), content = content)
}

/** Frosted glass: translucent fill, a soft top-lit rim and a hairline border. */
fun Modifier.glass(shape: Shape, tint: Color = Color.White, strength: Float = 1f): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(tint.copy(alpha = .10f * strength), tint.copy(alpha = .04f * strength))))
    .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .22f * strength), Color.White.copy(alpha = .04f)))), shape)

/** A round icon button on glass. */
@Composable
fun GlassIcon(icon: ImageVector, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, size: Dp = 40.dp, tint: Color = Ui.Text1, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier.size(size).pressScale(src).glass(CircleShape).clickable(src, null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, Modifier.size(size * .5f), tint = if (enabled) tint else tint.copy(alpha = .3f)) }
}

/** The primary action: a white capsule with dark text. */
@Composable
fun PrimaryPill(text: String, icon: ImageVector?, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Row(
        modifier.height(40.dp).pressScale(src).clip(CircleShape).background(if (enabled) Color.White else Color.White.copy(alpha = .25f))
            .clickable(src, null, enabled = enabled, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, null, Modifier.size(18.dp), tint = Color.Black); Spacer(Modifier.width(6.dp)) }
        Text(text, color = Color.Black, fontSize = Ui.Body, fontWeight = FontWeight.SemiBold)
    }
}

/** A secondary action: a glass capsule with an icon and a label. */
@Composable
fun GlassPill(text: String, icon: ImageVector?, modifier: Modifier = Modifier, tint: Color = Ui.Text1, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Row(
        modifier.height(40.dp).pressScale(src).glass(CircleShape).clickable(src, null, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        icon?.let { Icon(it, null, Modifier.size(18.dp), tint = tint); Spacer(Modifier.width(6.dp)) }
        Text(text, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** iOS segmented control: options share the width, the selected one sits on a raised plate. */
@Composable
fun Segmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(10.dp)).background(Ui.Raised).padding(2.dp)) {
        options.forEachIndexed { i, o ->
            val bg by animateColorAsState(if (i == selected) Ui.Raised2 else Color.Transparent, label = "seg")
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(bg).clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(o, color = if (i == selected) Ui.Text1 else Ui.Text2, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
        }
    }
}

/**
 * Control-center style slider: a wide rounded bar that fills from the start side, with its label
 * and value printed inside. Drag or tap anywhere on it; [onDone] ends the gesture (undo step).
 */
@Composable
fun BarSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, modifier: Modifier = Modifier, format: (Float) -> String = { it.toInt().toString() }, onDone: () -> Unit = {}, onChange: (Float) -> Unit) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val span = (range.endInclusive - range.start).coerceAtLeast(1e-3f)
    val frac by animateFloatAsState(((value - range.start) / span).coerceIn(0f, 1f), spring(stiffness = 2000f), label = "bar")
    val change by rememberUpdatedState(onChange)
    val done by rememberUpdatedState(onDone)
    fun set(x: Float, width: Int) {
        val f = ((if (rtl) width - x else x) / width).coerceIn(0f, 1f)
        change(range.start + f * span)
    }
    Box(
        modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(Ui.Raised)
            .pointerInput(range) { detectTapGestures { set(it.x, size.width); done() } }
            .pointerInput(range) { detectHorizontalDragGestures(onDragEnd = { done() }, onDragCancel = { done() }) { ch, _ -> set(ch.position.x, size.width) } },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(frac).align(Alignment.CenterStart).background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = .16f), Color.White.copy(alpha = .24f)))))
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = Ui.Text1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(format(value), color = Ui.Text2, fontSize = 14.sp)
        }
    }
}

/** A titled group of controls on a raised plate. */
@Composable
fun Section(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        title?.let { Text(it, Modifier.padding(start = 4.dp, bottom = 8.dp), color = Ui.Text2, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

/** Wrapping color dots: no sideways scrolling. 0 = "none". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorDots(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            val on = c == selected
            val ring by animateColorAsState(if (on) Color.White else Color.Transparent, label = "dot")
            Box(
                Modifier.size(34.dp).border(2.dp, ring, CircleShape).padding(4.dp).clip(CircleShape)
                    .background(if (c == 0L) Ui.Raised else Color(c))
                    .border(1.dp, Color.White.copy(alpha = .14f), CircleShape).clickable { onPick(c) }
                    .drawBehind { if (c == 0L) drawLine(Ui.Danger, Offset(size.width * .2f, size.height * .8f), Offset(size.width * .8f, size.height * .2f), 2.dp.toPx()) },
            )
        }
    }
}

/** A thin rounded handle at the top of a sheet. */
@Composable
fun Grabber(modifier: Modifier = Modifier) {
    Box(modifier.padding(vertical = 8.dp).size(36.dp, 5.dp).clip(CircleShape).background(Color.White.copy(alpha = .22f)))
}
