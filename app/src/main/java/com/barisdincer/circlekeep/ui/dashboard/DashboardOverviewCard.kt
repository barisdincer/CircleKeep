package com.barisdincer.circlekeep.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.ui.PersonWithWave

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DashboardHeroCard(
    peopleCount: Int,
    todayCount: Int,
    overdueCount: Int,
    snoozedCount: Int,
    upcomingCount: Int,
    recentInteractionCount: Int,
    topContact: PersonWithWave?,
    nextContact: PersonWithWave?,
    syncMessage: String?,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onOpenBatchLog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Bugünkü odak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    focusSummary(overdueCount, todayCount, nextContact),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            }

            topContact?.let { contact ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (contact.daysOverdue > 0) "İlk bakılacak kişi" else "Bugün zamanı gelen",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(contact.person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${contact.contactType.label} · ${contact.statusText()} · ${contact.scopeText()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip("Rehber", peopleCount)
                MetricChip("Bugün", todayCount)
                MetricChip("Geciken", overdueCount)
                MetricChip("Sırada", upcomingCount)
                MetricChip("Ertelenen", snoozedCount)
                MetricChip("Son kayıt", recentInteractionCount)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenBatchLog,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Etkinlik ekle")
                }
                OutlinedButton(
                    enabled = !isSyncing,
                    onClick = onSync,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aramaları eşle")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSyncing) "Eşleniyor" else "Eşle")
                }
            }

            Text(
                syncMessage ?: "Grup etkinlikleri katılan kişilere ayrı ayrı kaydedilir; gelmeyen kişi beklemede kalır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MetricChip(label: String, value: Int) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text("$label $value") }
    )
}

private fun focusSummary(overdueCount: Int, todayCount: Int, nextContact: PersonWithWave?): String {
    return when {
        overdueCount > 0 -> "$overdueCount geciken ritim var; önce bunları toparlamak iyi olur."
        todayCount > 0 -> "$todayCount kişi için bugün temas zamanı."
        nextContact != null -> "Bugün zorunlu aksiyon yok. Sıradaki: ${nextContact.person.name}, ${nextContact.statusText()}."
        else -> "Bugün zorunlu aksiyon yok; ritimler dengede."
    }
}
