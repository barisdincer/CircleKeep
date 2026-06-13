package com.barisdincer.circlekeep.ui.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val CircleShadowColor = Color(0xFF59331C)

data class CircleFilterOption<T>(
    val value: T,
    val label: String,
    val count: Int? = null
)

/**
 * Shared screen shell: a soft warm header (large title + optional subtitle, back, actions)
 * over a transparent body. Replaces the repeated TopAppBar + divider on every screen.
 */
@Composable
fun CircleScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        topBar = { CircleTopBar(title = title, subtitle = subtitle, onBack = onBack, actions = actions) },
        content = content,
    )
}

@Composable
fun CircleTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(modifier = Modifier.statusBarsPadding(), color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = if (onBack != null) CircleSpacing.xs else CircleSpacing.md,
                    end = CircleSpacing.xs,
                    top = CircleSpacing.sm,
                    bottom = CircleSpacing.xs,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                }
                Spacer(Modifier.width(CircleSpacing.xxs))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            actions()
        }
    }
}

/** Cozy surface card: graduated radius, soft warm shadow, hairline border, optional press. */
@Composable
fun CircleCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(CircleRadius.card),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation: Dp = CircleElevation.card,
    content: @Composable () -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .shadow(elevation, shape, clip = false, ambientColor = CircleShadowColor, spotColor = CircleShadowColor)
        .clip(shape)
        .background(containerColor)
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
        .then(if (onClick != null) Modifier.circlePressable(onClick = onClick) else Modifier)
        .animateContentSize(animationSpec = CircleMotion.gentle())
    Box(modifier = base) { content() }
}

/** Bold branded hero surface (primary fill) used at the top of Dashboard and Reports. */
@Composable
fun CircleHeroCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(CircleRadius.hero)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(CircleElevation.raised, shape, clip = false, ambientColor = CircleShadowColor, spotColor = CircleShadowColor)
            .clip(shape)
            .background(containerColor)
            .padding(CircleSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(CircleSpacing.md),
        content = content,
    )
}

@Composable
fun CircleSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    count: Int? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (count != null) {
                    CircleCountPill(count)
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(8.dp))
            action()
        }
    }
}

/** Animated count badge used by section headers and panels. */
@Composable
fun CircleCountPill(
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(CircleRadius.pill), color = containerColor) {
        Text(
            "${animatedCount(count)}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

@Composable
fun CircleMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    helper: String? = null
) {
    CircleCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(CircleSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = accent)
                }
            }
            Text(value, style = MaterialTheme.typography.headlineSmall)
            if (!helper.isNullOrBlank()) {
                Text(helper, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Compact stat block (big animated number + label) for hero rows. */
@Composable
fun CircleStatChip(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CircleRadius.control))
            .background(containerColor)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
        }
        Text("${animatedCount(value)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = contentColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.85f))
    }
}

@Composable
fun CircleSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Ara"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(CircleRadius.pill),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(visible = value.isNotBlank(), enter = scaleIn() + fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Aramayı temizle")
                }
            }
        },
        placeholder = { Text(placeholder) },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
fun <T> CircleFilterRow(
    options: List<CircleFilterOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            CircleChip(
                selected = option.value == selected,
                label = option.count?.let { "${option.label} $it" } ?: option.label,
                onClick = { onSelected(option.value) },
            )
        }
    }
}

/** Pill chip with animated selected color and a gentle press scale. */
@Composable
fun CircleChip(
    selected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        animationSpec = CircleMotion.gentle(), label = "chipContainer",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = CircleMotion.gentle(), label = "chipContent",
    )
    val shape = RoundedCornerShape(CircleRadius.pill)
    Row(
        modifier = modifier
            .clip(shape)
            .background(container)
            .then(if (!selected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier)
            .circlePressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

@Composable
fun CircleStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CircleRadius.pill),
        color = containerColor
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Primary filled action button: control radius, leading icon, gentle press scale. */
@Composable
fun CirclePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, CircleMotion.gentle(), label = "btnScale")
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled,
        shape = RoundedCornerShape(CircleRadius.control),
        interactionSource = interaction,
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Tonal secondary action button matching CirclePrimaryButton. */
@Composable
fun CircleTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, CircleMotion.gentle(), label = "btnScale")
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(48.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled,
        shape = RoundedCornerShape(CircleRadius.control),
        interactionSource = interaction,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Bouncy primary FAB shared across list screens. */
@Composable
fun CircleFab(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, CircleMotion.bouncy(), label = "fabScale")
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(CircleRadius.control),
        interactionSource = interaction,
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
fun CircleEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    CircleCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = null,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(CircleSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(CircleRadius.pill))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(2.dp))
                CirclePrimaryButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun CircleFormSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    CircleCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(CircleSpacing.md),
            verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
fun CircleDestructiveDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(CircleRadius.card),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(body) },
        confirmButton = {
            OutlinedButton(
                onClick = onConfirm,
                shape = RoundedCornerShape(CircleRadius.control),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

@Composable
fun CircleSkeletonRow(modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CircleRadius.card))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.72f).height(12.dp).clip(RoundedCornerShape(CircleRadius.pill)).background(shimmer))
        Box(modifier = Modifier.fillMaxWidth(0.48f).height(10.dp).clip(RoundedCornerShape(CircleRadius.pill)).background(shimmer))
    }
}

/** Animated multi-segment progress strip for the Reports distributions. */
@Composable
fun CircleSegmentedBar(
    segments: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
) {
    val visible = segments.filter { it.first > 0f }
    if (visible.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(CircleRadius.pill))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(CircleRadius.pill)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        visible.forEach { (weight, color) ->
            val animatedWeight by animateFloatAsState(weight.coerceAtLeast(0.01f), CircleMotion.gentle(), label = "segment")
            Box(modifier = Modifier.weight(animatedWeight).fillMaxWidth().height(height).background(color))
        }
    }
}

/** Single animated progress bar (label + value above, filling track below). */
@Composable
fun CircleProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 8.dp,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), CircleMotion.gentle(), label = "progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(CircleRadius.pill))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(CircleRadius.pill))
                .background(color)
        )
    }
}
