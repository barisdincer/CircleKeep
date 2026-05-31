package com.barisdincer.circlekeep.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.ContactDatePreset
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.UserPreferences
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.data.contactActionLabel
import com.barisdincer.circlekeep.data.resolveTimestamp
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.PersonWithWave
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: NetworkViewModel,
    userPreferences: UserPreferences,
    onProfileClick: () -> Unit
) {
    val dashboard by viewModel.dashboardReminders.collectAsState()
    val people by viewModel.people.collectAsState()
    val waves by viewModel.waves.collectAsState()
    val interactions by viewModel.interactions.collectAsState()
    val activeContactTypes by viewModel.activeContactTypes.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var logSheet by remember { mutableStateOf<ContactLogSheetState?>(null) }
    val dueCount = dashboard.today.size + dashboard.overdue.size

    LaunchedEffect(uiMessage) {
        val message = uiMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearUiMessage()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable(onClick = onProfileClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                userPreferences.displayInitials,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
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
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    onSync = { viewModel.syncCallLog(context) },
                    onOpenBatchLog = { logSheet = ContactLogSheetState.Batch }
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
                    onOpenLogSheet = { logSheet = ContactLogSheetState.Single(contact) },
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
                    onOpenLogSheet = { logSheet = ContactLogSheetState.Single(contact) },
                    onSnooze = { viewModel.snoozePersonForDays(contact.person.id, 1) }
                )
            }

            item {
                ReminderSectionHeader(
                    title = "Sıradakiler",
                    count = dashboard.upcoming.size,
                    emptyText = "Sırada bekleyen ritim yok."
                )
            }
            items(dashboard.upcoming) { contact ->
                ContactReminderCard(
                    contact = contact,
                    onOpenLogSheet = { logSheet = ContactLogSheetState.Single(contact) },
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
                    onOpenLogSheet = { logSheet = ContactLogSheetState.Single(contact) },
                    onSnooze = { viewModel.snoozePersonForDays(contact.person.id, 1) }
                )
            }

            if (dueCount == 0 && dashboard.upcoming.isEmpty() && dashboard.snoozed.isEmpty()) {
                item {
                    EmptyReminderState()
                }
            }
        }

        logSheet?.let { sheet ->
            ContactLogSheet(
                sheet = sheet,
                people = people,
                waves = waves,
                contactTypes = activeContactTypes,
                onDismiss = { logSheet = null },
                onSave = { personIds, type, note, timestamp ->
                    viewModel.logInteractions(personIds, type, note, timestamp)
                    logSheet = null
                }
            )
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = contentColor)
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
    onSync: () -> Unit,
    onOpenBatchLog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Hal hatır ritmi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
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
            Spacer(modifier = Modifier.height(12.dp))
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
            FilledTonalButton(
                onClick = onOpenBatchLog,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.EditNote, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grup veya çoklu temas kaydet")
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
    onOpenLogSheet: () -> Unit,
    onSnooze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(contact.person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Grup: ${contact.wave?.name ?: "Yok"} (${contact.effectiveFrequencyDays} gün) · ${contact.actionTimingLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ReminderStatusBadge(contact)
            }

            Spacer(modifier = Modifier.height(10.dp))
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

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenLogSheet,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(contactTypeIcon(contact.contactType.key), contentDescription = contact.contactType.label)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(contactActionLabel(contact.contactType.key, contact.contactType.label))
                }
                FilledTonalButton(
                    onClick = onOpenLogSheet,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
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
                dateStr
            }
            contact.daysOverdue > 0 -> "${contact.daysOverdue} gün geçti"
            contact.daysOverdue == 0L && contact.isDue -> "Bugün"
            contact.daysOverdue < 0 -> "${-contact.daysOverdue} gün sonra"
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

private fun PersonWithWave.actionTimingLabel(): String {
    val timing = when {
        snoozedUntilDate != null -> "Ertelendi"
        daysOverdue > 0 -> "${daysOverdue} gün gecikti"
        daysOverdue == 0L && isDue -> "Bugün"
        daysOverdue < 0 -> "${-daysOverdue} gün sonra"
        else -> "Takipte"
    }
    return "$timing · ${contactType.label}"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ContactLogSheet(
    sheet: ContactLogSheetState,
    people: List<Person>,
    waves: List<Wave>,
    contactTypes: List<ContactType>,
    onDismiss: () -> Unit,
    onSave: (List<Int>, String, String, Long) -> Unit
) {
    val initialPersonIds = when (sheet) {
        ContactLogSheetState.Batch -> emptySet()
        is ContactLogSheetState.Single -> setOf(sheet.contact.person.id)
    }
    val initialTypeKey = when (sheet) {
        ContactLogSheetState.Batch -> contactTypes.firstOrNull()?.key ?: DefaultContactTypes.CALL
        is ContactLogSheetState.Single -> sheet.contact.contactType.key
    }

    var selectedPersonIds by remember(sheet) { mutableStateOf(initialPersonIds) }
    var selectedTypeKey by remember(sheet, contactTypes) { mutableStateOf(initialTypeKey) }
    var selectedDatePreset by remember(sheet) { mutableStateOf(ContactDatePreset.TODAY) }
    var note by remember(sheet) { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    val selectedType = contactTypes.find { it.key == selectedTypeKey }
        ?: DefaultContactTypes.all.first { it.key == DefaultContactTypes.CALL }
    val editableParticipants = sheet is ContactLogSheetState.Batch
    val selectedPeople = people.filter { it.id in selectedPersonIds }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (editableParticipants) "Temas kaydet" else "${selectedPeople.firstOrNull()?.name ?: "Kişi"} ile temas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (editableParticipants) {
                        "Bir grubu veya birden fazla kişiyi seçip aynı temas kaydını ekleyebilirsin."
                    } else {
                        "Kaydetmeden önce türü, zamanı ve gerekiyorsa kısa notu kontrol et."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("İletişim türü") },
                    leadingIcon = { Icon(contactTypeIcon(selectedType.key), contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    contactTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            leadingIcon = { Icon(contactTypeIcon(type.key), contentDescription = null) },
                            onClick = {
                                selectedTypeKey = type.key
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    ContactDatePreset.TODAY,
                    ContactDatePreset.YESTERDAY,
                    ContactDatePreset.THREE_DAYS_AGO,
                    ContactDatePreset.WEEK_AGO
                ).forEach { preset ->
                    FilterChip(
                        selected = selectedDatePreset == preset,
                        onClick = { selectedDatePreset = preset },
                        label = { Text(preset.label) }
                    )
                }
            }

            if (editableParticipants) {
                ExposedDropdownMenuBox(
                    expanded = groupExpanded,
                    onExpandedChange = { groupExpanded = !groupExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedPeople.isEmpty()) "Katılımcı seç" else "${selectedPeople.size} kişi seçili",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gruba göre hızlı seç") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tüm seçimi temizle") },
                            onClick = {
                                selectedPersonIds = emptySet()
                                groupExpanded = false
                            }
                        )
                        waves.forEach { wave ->
                            val groupIds = people.filter { it.waveId == wave.id }.map { it.id }.toSet()
                            DropdownMenuItem(
                                text = { Text("${wave.name} (${groupIds.size} kişi)") },
                                onClick = {
                                    selectedPersonIds = selectedPersonIds + groupIds
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(people, key = { it.id }) { person ->
                        val checked = person.id in selectedPersonIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedPersonIds = if (checked) {
                                        selectedPersonIds - person.id
                                    } else {
                                        selectedPersonIds + person.id
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedPersonIds = if (checked) {
                                        selectedPersonIds - person.id
                                    } else {
                                        selectedPersonIds + person.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(person.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    waves.find { it.id == person.waveId }?.name ?: "Grup yok",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Text(
                        selectedPeople.firstOrNull()?.name ?: "Kişi",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Kısa not") },
                placeholder = { Text("Ne konuştunuz?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = selectedPersonIds.isNotEmpty(),
                    onClick = {
                        val timestamp = selectedDatePreset.resolveTimestamp() ?: System.currentTimeMillis()
                        onSave(selectedPersonIds.toList(), selectedType.key, note, timestamp)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("${contactActionLabel(selectedType.key, selectedType.label)} kaydet")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private fun contactTypeIcon(typeKey: String): ImageVector {
    return when (typeKey) {
        DefaultContactTypes.MESSAGE -> Icons.Default.Sms
        DefaultContactTypes.MEETING -> Icons.Default.Group
        else -> Icons.Default.Call
    }
}

private sealed interface ContactLogSheetState {
    data class Single(val contact: PersonWithWave) : ContactLogSheetState
    data object Batch : ContactLogSheetState
}
