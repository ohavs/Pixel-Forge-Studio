package com.ohav.pixelforge.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/** Staggered entrance: fades and rises into place, [index] steps of 70ms after the screen opens. */
fun Modifier.enter(index: Int): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(index * 70L); p.animateTo(1f, spring(dampingRatio = .75f, stiffness = 220f)) }
    graphicsLayer { alpha = p.value.coerceIn(0f, 1f); translationY = (1 - p.value) * 60f; scaleX = .96f + .04f * p.value; scaleY = scaleX }
}

/** Springy shrink while pressed. */
fun Modifier.pressScale(source: MutableInteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) .95f else 1f, spring(dampingRatio = .5f, stiffness = 400f), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}
