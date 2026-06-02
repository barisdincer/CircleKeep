package com.barisdincer.circlekeep.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.contactActionLabel
import com.barisdincer.circlekeep.ui.PersonWithWave
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CountPill(count)
        }
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun UpcomingSectionTitle(
    count: Int,
    expanded: Boolean,
    nextContact: PersonWithWave?,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Sıradakiler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    nextContact?.let { "${it.person.name} · ${it.statusText()} · ${it.contactType.label}" }
                        ?: "Yaklaşan ritim yok.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CountPill(count)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Kapat" else "Aç"
                )
            }
        }
    }
}

@Composable
private fun CountPill(count: Int) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            "$count",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun EmptyFocusCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
    val statusColor = when {
        contact.snoozedUntilDate != null -> MaterialTheme.colorScheme.tertiaryContainer
        contact.daysOverdue > 0 -> MaterialTheme.colorScheme.errorContainer
        contact.isDue -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusTextColor = when {
        contact.snoozedUntilDate != null -> MaterialTheme.colorScheme.onTertiaryContainer
        contact.daysOverdue > 0 -> MaterialTheme.colorScheme.onErrorContainer
        contact.isDue -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ContactTypeIconBox(contact.contactType.key)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(contact.person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${contact.contactType.label} · ${contact.scopeText()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor) {
                    Text(
                        contact.statusText(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Son temas: ${formatShortDate(contact.person.lastInteractionDate)} · ${contact.daysSinceLastInteraction} gün önce",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Ritim: ${contact.effectiveFrequencyDays} gün · ${contact.groupPolicyText()}",
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
                Button(
                    onClick = onLog,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(contactTypeIcon(contact.contactType.key), contentDescription = contact.contactType.label)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(contactActionLabel(contact.contactType.key, contact.contactType.label))
                }
                FilledTonalButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = "Ertele")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (contact.snoozedUntilDate != null) "Tarih değiştir" else "Ertele")
                }
            }
        }
    }
}

@Composable
private fun ContactTypeIconBox(typeKey: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            contactTypeIcon(typeKey),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(21.dp)
        )
    }
}

internal fun contactTypeIcon(typeKey: String): ImageVector {
    return when (typeKey) {
        DefaultContactTypes.MESSAGE -> Icons.Default.Sms
        DefaultContactTypes.MEETING -> Icons.Default.Group
        else -> Icons.Default.Call
    }
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

internal fun PersonWithWave.scopeText(): String {
    return wave?.name ?: "Kişiye özel"
}

private fun PersonWithWave.groupPolicyText(): String {
    return if (wave != null) {
        "grup ritmi kişi kişi güncellenir"
    } else {
        "kişiye özel takip"
    }
}

internal fun formatShortDate(timestamp: Long): String {
    return SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(timestamp))
}
