package com.barisdincer.circlekeep.ui.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.ui.theme.CircleTheme

object CircleSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
}

/** Graduated radius scale — small chips up to large cozy heroes and sheets. */
object CircleRadius {
    val chip = 10.dp
    val control = 16.dp
    val card = 20.dp
    val hero = 28.dp
    val sheet = 28.dp
    val pill = 999.dp
}

/** Soft warm depth. Cards rest flat (border + tint); raised/interactive surfaces lift gently. */
object CircleElevation {
    val card = 1.dp
    val raised = 3.dp
    val fab = 6.dp
    val bar = 10.dp
}

/** Single source of truth for motion so every screen feels consistent. */
object CircleMotion {
    fun <T> gentle(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> bouncy(): SpringSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)

    fun <T> snappy(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}

object CirclePalette {
    val Primary = Color(0xFFD85A30)
    val Secondary = Color(0xFF6B5249)
    val Accent = Color(0xFFBA7517)
    val Success = Color(0xFF2F8F5B)
    val Warning = Color(0xFFBA7517)
    val Error = Color(0xFFC5483B)

    val LightBackground = Color(0xFFFBF7F4)
    val LightSurface = Color(0xFFFFFDFB)
    val LightSurfaceRaised = Color(0xFFF4ECE6)

    val DarkBackground = Color(0xFF1A1512)
    val DarkSurface = Color(0xFF221C18)
    val DarkSurfaceRaised = Color(0xFF2D2520)
}

/** Deterministic warm avatar tones, picked from a person's name. Stable across themes. */
data class CircleAvatarTone(val container: Color, val content: Color)

private val avatarTones = listOf(
    CircleAvatarTone(Color(0xFFFBE3D2), Color(0xFF993C1D)), // coral
    CircleAvatarTone(Color(0xFFFCEFD7), Color(0xFF854F0B)), // amber
    CircleAvatarTone(Color(0xFFF0E4DC), Color(0xFF5A4036)), // taupe
    CircleAvatarTone(Color(0xFFDDF0E4), Color(0xFF1F6B43)), // sage
    CircleAvatarTone(Color(0xFFF6DFD7), Color(0xFF9A4A2E)), // clay
    CircleAvatarTone(Color(0xFFEDE3F0), Color(0xFF6A4A78)), // mauve
)

fun avatarToneFor(seed: String): CircleAvatarTone {
    if (seed.isBlank()) return avatarTones.first()
    val index = (seed.hashCode() % avatarTones.size + avatarTones.size) % avatarTones.size
    return avatarTones[index]
}

/** Rhythm states share one color language across dashboard, people, and detail screens. */
enum class RhythmState { OVERDUE, TODAY, UPCOMING, SNOOZED, ON_TRACK }

@Composable
fun rhythmContainerColor(state: RhythmState): Color = when (state) {
    RhythmState.OVERDUE -> MaterialTheme.colorScheme.errorContainer
    RhythmState.TODAY -> MaterialTheme.colorScheme.primaryContainer
    RhythmState.UPCOMING -> MaterialTheme.colorScheme.tertiaryContainer
    RhythmState.SNOOZED -> MaterialTheme.colorScheme.secondaryContainer
    RhythmState.ON_TRACK -> CircleTheme.extras.successContainer
}

@Composable
fun rhythmContentColor(state: RhythmState): Color = when (state) {
    RhythmState.OVERDUE -> MaterialTheme.colorScheme.onErrorContainer
    RhythmState.TODAY -> MaterialTheme.colorScheme.onPrimaryContainer
    RhythmState.UPCOMING -> MaterialTheme.colorScheme.onTertiaryContainer
    RhythmState.SNOOZED -> MaterialTheme.colorScheme.onSecondaryContainer
    RhythmState.ON_TRACK -> CircleTheme.extras.onSuccessContainer
}
