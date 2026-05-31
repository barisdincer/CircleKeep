package com.barisdincer.circlekeep.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.PersonWithWave
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: NetworkViewModel) {
    val dashboard by viewModel.dashboardReminders.collectAsState()
    val people by viewModel.people.collectAsState()
    val interactions by viewModel.interactions.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val context = LocalContext.current
    val dueCount = dashboard.today.size + dashboard.overdue.size

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("CircleKeep", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                            val currentDate = SimpleDateFormat("d MMMM, EEEE", Locale("tr", "TR")).format(Date())
                            Text(currentDate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard("Rehber", "${people.size} kişi", Icons.Default.Group, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                    StatCard("Bekleyen", "$dueCount kişi", Icons.Default.Waves, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f))
                }
            }

            item {
                RhythmSummaryCard(
                    interactions = interactions.size,
                    recentInteractions = interactions.count { it.timestamp > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000 },
                    syncMessage = syncState.message,
                    isSyncing = syncState.isSyncing,
                    onSync = { viewModel.syncCallLog(context) }
                )
            }

            item {
                ReminderSectionHeader(
                    title = "Bugün",
                    count = dashboard.today.size,
                    emptyText = "Bugün net bekleyen kimse yok."
                )
            }
            items(dashboard.today) { contact ->
                ContactReminderCard(
                    contact = contact,
                    onLogInteraction = { type, note -> viewModel.logInteraction(contact.person.id, type, note) },
                    onSnooze = { viewModel.snoozePersonForDays(contact.person.id, 1) }
                )
            }

            item {
                ReminderSectionHeader(
                    title = "Gecikenler",
                    count = dashboard.overdue.size,
                    emptyText = "Gecikmiş ritim yok."
                )
            }
            items(dashboard.overdue) { contact ->
                ContactReminderCard(
                    contact = contact,
                    onLogInteraction = { type, note -> viewModel.logInteraction(contact.person.id, type, note) },
                    onSnooze = { viewModel.snoozePersonForDays(contact.person.id, 1) }
                )
            }

            item {
                ReminderSectionHeader(
                    title = "Yakında",
                    count = dashboard.upcoming.size,
                    emptyText = "Önümüzdeki hafta yaklaşan ritim yok."
                )
            }
            items(dashboard.upcoming) { contact ->
                ContactReminderCard(
                    contact = contact,
                    onLogInteraction = { type, note -> viewModel.logInteraction(contact.person.id, type, note) },
                    onSnooze = { viewModel.snoozePersonForDays(contact.person.id, 1) }
                )
            }

            item {
                ReminderSectionHeader(
                    title = "Ertelenenler",
                    count = dashboard.snoozed.size,
                    emptyText = "Ertelenmiş hatırlatma yok."
                )
            }
            items(dashboard.snoozed) { contact ->
                ContactReminderCard(
                    contact = contact,
                    onLogInteraction = { type, note -> viewModel.logInteraction(contact.person.id, type, note) },
                    onSnooze = { viewModel.snoozePersonForDays(contact.person.id, 1) }
                )
            }

            if (dueCount == 0 && dashboard.upcoming.isEmpty() && dashboard.snoozed.isEmpty()) {
                item {
                    EmptyReminderState()
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, containerColor: androidx.compose.ui.graphics.Color, contentColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = contentColor)
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 2.dp), color = contentColor.copy(alpha = 0.7f))
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = contentColor)
            }
        }
    }
}

@Composable
private fun RhythmSummaryCard(
    interactions: Int,
    recentInteractions: Int,
    syncMessage: String?,
    isSyncing: Boolean,
    onSync: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Hal hatır ritmi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Toplam temas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    Text("$interactions", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Son 7 gün", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    Text("$recentInteractions", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    syncMessage ?: "Arama kayıtları uygulama açıldığında kontrol edilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f)
                )
                TextButton(enabled = !isSyncing, onClick = onSync) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aramaları eşle")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSyncing) "Kontrol" else "Eşle")
                }
            }
        }
    }
}

@Composable
private fun ReminderSectionHeader(title: String, count: Int, emptyText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AssistChip(onClick = {}, label = { Text("$count") }, enabled = false)
        }
        if (count == 0) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyReminderState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Ritimler yolunda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Yakın çevren için şimdilik bekleyen aksiyon yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ContactReminderCard(
    contact: PersonWithWave,
    onLogInteraction: (String, String) -> Unit,
    onSnooze: () -> Unit
) {
    var showNoteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(contact.person.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Döngü: ${contact.wave?.name ?: "Yok"} (${contact.effectiveFrequencyDays} gün) · ${contact.contactType.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ReminderStatusBadge(contact)
            }

            Spacer(modifier = Modifier.height(16.dp))
            val dateStr = SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(contact.person.lastInteractionDate))
            Text(
                "Son temas: $dateStr (${contact.daysSinceLastInteraction} gün önce)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (contact.person.nextConversationHint.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Bir dahaki sefere: ${contact.person.nextConversationHint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onLogInteraction(contact.contactType.key, "") },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = contact.contactType.label)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Temas ettim")
                }
                FilledTonalButton(
                    onClick = { showNoteDialog = true },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = "Notla kaydet")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Notla")
                }
            }
            TextButton(
                onClick = onSnooze,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Schedule, contentDescription = "Yarın hatırlat")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Yarın hatırlat")
            }
        }
    }

    if (showNoteDialog) {
        LogWithNoteDialog(
            contactTypeLabel = contact.contactType.label,
            onDismiss = { showNoteDialog = false },
            onSave = { note ->
                onLogInteraction(contact.contactType.key, note)
                showNoteDialog = false
            }
        )
    }
}

@Composable
private fun ReminderStatusBadge(contact: PersonWithWave) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (contact.daysOverdue > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    ) {
        val status = when {
            contact.snoozedUntilDate != null -> {
                val dateStr = SimpleDateFormat("d MMM", Locale("tr", "TR")).format(Date(contact.snoozedUntilDate))
                "$dateStr"
            }
            contact.daysOverdue > 0 -> "${contact.daysOverdue} gün geçti"
            contact.daysOverdue == 0L && contact.isDue -> "Bugün"
            contact.daysOverdue < 0 -> "${-contact.daysOverdue} gün kaldı"
            else -> "Takipte"
        }
        Text(
            status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (contact.daysOverdue > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LogWithNoteDialog(
    contactTypeLabel: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$contactTypeLabel notu") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Kısa not") },
                placeholder = { Text("Ne konuştunuz?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            Button(onClick = { onSave(note) }) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
