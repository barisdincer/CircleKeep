package com.barisdincer.circlekeep.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.contactActionLabel
import com.barisdincer.circlekeep.ui.PersonWithWave
import com.barisdincer.circlekeep.ui.design.CircleAvatar
import com.barisdincer.circlekeep.ui.design.CircleCard
import com.barisdincer.circlekeep.ui.design.CircleCountPill
import com.barisdincer.circlekeep.ui.design.CirclePrimaryButton
import com.barisdincer.circlekeep.ui.design.CircleSpacing
import com.barisdincer.circlekeep.ui.design.CircleStatusPill
import com.barisdincer.circlekeep.ui.design.CircleTonalButton
import com.barisdincer.circlekeep.ui.design.RhythmState
import com.barisdincer.circlekeep.ui.design.rhythmContainerColor
import com.barisdincer.circlekeep.ui.design.rhythmContentColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun DashboardActionBar(
    recentInteractionCount: Int,
    syncMessage: String?,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onOpenBatchLog: () -> Unit
) {
    CircleCard {
        Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Yeni temas kaydı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Buluşma, arama veya mesajı tek kişiye ya da bir gruptaki katılımcılara hızlıca işle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CirclePrimaryButton(
                    text = "Etkinlik ekle",
                    icon = Icons.Default.EditNote,
                    onClick = onOpenBatchLog,
                    modifier = Modifier.weight(1f),
                )
                SyncButton(
                    isSyncing = isSyncing,
                    onClick = onSync,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                syncMessage ?: "Son listede $recentInteractionCount temas kaydı var.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Tonal "sync" button whose leading refresh icon spins while [isSyncing]. */
@Composable
private fun SyncButton(
    isSyncing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "sync")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Restart),
        label = "syncAngle",
    )
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        enabled = !isSyncing,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(com.barisdincer.circlekeep.ui.design.CircleRadius.control),
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = "Aramaları eşle",
            modifier = Modifier
                .size(18.dp)
                .rotate(if (isSyncing) angle else 0f),
        )
        Spacer(Modifier.width(8.dp))
        Text(if (isSyncing) "Eşleniyor" else "Eşle", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun ReminderSectionTitle(
    title: String,
    count: Int,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            CircleCountPill(count, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun UpcomingSectionPanel(
    count: Int,
    expanded: Boolean,
    nextContact: PersonWithWave?,
    contacts: List<PersonWithWave>,
    onToggle: () -> Unit,
    onLog: (PersonWithWave) -> Unit,
    onSnooze: (PersonWithWave) -> Unit
) {
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    CircleCard(onClick = onToggle) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = CircleSpacing.md, vertical = CircleSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Sıradakiler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        nextContact?.let { "${it.person.name} · ${it.upcomingStatusText()} · ${it.contactType.label}" }
                            ?: "Yaklaşan ritim yok.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircleCountPill(count, containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Kapat" else "Aç",
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            }
            AnimatedVisibility(visible = expanded && contacts.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = CircleSpacing.sm, end = CircleSpacing.sm, bottom = CircleSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(CircleSpacing.xs)
                ) {
                    contacts.forEach { contact ->
                        ReminderCard(
                            contact = contact,
                            onLog = { onLog(contact) },
                            onSnooze = { onSnooze(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyFocusCard() {
    CircleCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), border = null, elevation = 0.dp) {
        Row(
            modifier = Modifier.padding(CircleSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Bugün sakin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Yaklaşanları açarak sıradaki ritimleri kontrol edebilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun ReminderCard(
    contact: PersonWithWave,
    onLog: () -> Unit,
    onSnooze: () -> Unit
) {
    val state = contact.rhythmState()
    CircleCard {
        Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircleAvatar(name = contact.person.name, size = 44.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(contact.person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(contactTypeIcon(contact.contactType.key), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${contact.contactType.label} · ${contact.scopeText()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                CircleStatusPill(
                    label = contact.statusText(),
                    containerColor = rhythmContainerColor(state),
                    contentColor = rhythmContentColor(state),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Son temas: ${formatShortDate(contact.lastInteractionDate)} · ${contact.daysSinceLastInteraction} gün önce",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    contact.nextDateLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (contact.person.nextConversationHint.isNotBlank()) {
                    Text(
                        "Not: ${contact.person.nextConversationHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CirclePrimaryButton(
                    text = contactActionLabel(contact.contactType.key, contact.contactType.label),
                    icon = contactTypeIcon(contact.contactType.key),
                    onClick = onLog,
                    modifier = Modifier.weight(1f),
                )
                CircleTonalButton(
                    text = if (contact.snoozedUntilDate != null) "Tarih değiştir" else "Ertele",
                    icon = Icons.Default.Schedule,
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

internal fun contactTypeIcon(typeKey: String): ImageVector {
    return when (typeKey) {
        DefaultContactTypes.MESSAGE -> Icons.Default.Sms
        DefaultContactTypes.MEETING -> Icons.Default.Group
        else -> Icons.Default.Call
    }
}

internal fun PersonWithWave.rhythmState(): RhythmState = when {
    snoozedUntilDate != null -> RhythmState.SNOOZED
    daysOverdue > 0 -> RhythmState.OVERDUE
    daysOverdue == 0L && isDue -> RhythmState.TODAY
    else -> RhythmState.UPCOMING
}

internal fun PersonWithWave.statusText(): String {
    return when {
        snoozedUntilDate != null -> "Ertelendi ${formatShortDate(snoozedUntilDate)}"
        daysOverdue > 0 -> "${daysOverdue} gün gecikti"
        daysOverdue == 0L && isDue -> "Bugün"
        daysOverdue < 0 -> "${-daysOverdue} gün sonra"
        else -> "Takipte"
    }
}

private fun PersonWithWave.upcomingStatusText(): String {
    return when {
        daysOverdue < 0 -> "${-daysOverdue} gün sonra · ${nextDueDateText()}"
        else -> statusText()
    }
}

internal fun PersonWithWave.scopeText(): String {
    return wave?.name ?: "Kişiye özel"
}

private fun PersonWithWave.nextDateLine(): String {
    val prefix = when {
        snoozedUntilDate != null -> "Erteleme tarihi: ${formatShortDate(snoozedUntilDate)}"
        daysOverdue < 0 -> "Sıradaki tarih: ${nextDueDateText()}"
        daysOverdue == 0L && isDue -> "Bugünün ritmi: ${nextDueDateText()}"
        daysOverdue > 0 -> "Ritim tarihi geçmiş: ${nextDueDateText()}"
        else -> "Ritim: ${effectiveFrequencyDays} gün"
    }
    return "$prefix · ${groupPolicyText()}"
}

private fun PersonWithWave.groupPolicyText(): String {
    return if (wave != null) {
        "grup ritmi kişi kişi güncellenir"
    } else {
        "kişiye özel takip"
    }
}

private fun PersonWithWave.nextDueDateText(): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = lastInteractionDate
        add(Calendar.DAY_OF_YEAR, effectiveFrequencyDays)
    }
    return formatShortDate(calendar.timeInMillis)
}

internal fun formatShortDate(timestamp: Long): String {
    return SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(timestamp))
}
