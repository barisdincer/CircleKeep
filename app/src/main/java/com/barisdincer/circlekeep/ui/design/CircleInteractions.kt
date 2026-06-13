package com.barisdincer.circlekeep.ui.design

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Makes any surface feel alive: a gentle spring scale-down while pressed plus a warm ripple.
 * Used on every tappable card and custom button so interaction feedback is consistent.
 */
fun Modifier.circlePressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = CircleMotion.gentle(),
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick,
        )
}

/** Smoothly tweens an integer toward its target — used for counters and stat values. */
@Composable
fun animatedCount(target: Int): Int {
    val value by animateIntAsState(
        targetValue = target,
        animationSpec = CircleMotion.snappy(),
        label = "animatedCount",
    )
    return value
}

/** A slow warm sweep for skeleton/loading placeholders. */
@Composable
fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -400f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Restart),
        label = "shimmerTranslate",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate, 0f),
        end = Offset(translate + 400f, 0f),
    )
}

/** Warm accent helper used where a plain Color over surfaces needs a subtle tint. */
fun Color.softer(alpha: Float = 0.5f): Color = copy(alpha = alpha)
