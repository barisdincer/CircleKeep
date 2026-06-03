package com.barisdincer.circlekeep.ui.waves

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.InteractionLog
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.data.groupMemberRhythm
import com.barisdincer.circlekeep.data.sortedByTurkish
import com.barisdincer.circlekeep.data.statusLabel
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.components.DatePickerField
import com.barisdincer.circlekeep.ui.components.InteractionEventGroup
import com.barisdincer.circlekeep.ui.components.interactionEventGroups

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupDetailScreen(
    waveId: Int,
    viewModel: NetworkViewModel,
    onBack: () -> Unit,
    onPersonClick: (Int) -> Unit,
    onAddPersonToGroup: (Int) -> Unit
) {
    val waves by viewModel.waves.collectAsState()
    val people by viewModel.people.collectAsState()
    val interactions by viewModel.allInteractions.collectAsState()
    val contactTypes by viewModel.contactTypes.collectAsState()
    val activeContactTypes by viewModel.activeContactTypes.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val wave = waves.find { it.id == waveId }
    val groupPeople = people.filter { it.waveId == waveId }.sortedByTurkish { it.name }
    val groupPersonIds = groupPeople.map { it.id }.toSet()
    val groupLogs = interactions
        .filter { it.personId in groupPersonIds }
        .sortedByDescending { it.timestamp }
    val groupEvents = interactionEventGroups(groupLogs)

    var showExistingPersonSheet by remember { mutableStateOf(false) }
    var showGroupLogSheet by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<InteractionEventGroup?>(null) }
    var deletingEvent by remember { mutableStateOf<InteractionEventGroup?>(null) }

    LaunchedEffect(uiMessage) {
        val message = uiMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearUiMessage()
    }

    if (wave == null) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            topBar = {
                TopAppBar(
                    title = { Text("Grup bulunamadı") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    }
                )
            }
        ) { padding ->
            Text(
                "Bu grup artık mevcut değil.",
                modifier = Modifier.padding(padding).padding(16.dp)
            )
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(wave.name, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
                .padding(horizontal = 12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GroupActionsCard(
                    memberCount = groupPeople.size,
                    frequencyDays = wave.frequencyDays.coerceAtLeast(1),
                    onNewPerson = { onAddPersonToGroup(wave.id) },
                    onExistingPerson = { showExistingPersonSheet = true },
                    onLogGroup = { showGroupLogSheet = true }
                )
            }

            item {
                Text("Üyeler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (groupPeople.isEmpty()) {
                item {
                    EmptyGroupCard()
                }
            }

            items(groupPeople, key = { it.id }) { person ->
                val rhythm = groupMemberRhythm(person, wave)
                val lastLog = groupLogs.firstOrNull { it.personId == person.id }
                GroupMemberCard(
                    person = person,
                    rhythmLabel = rhythm.statusLabel(),
                    daysSince = rhythm.daysSinceLastInteraction,
                    effectiveFrequencyDays = rhythm.effectiveFrequencyDays,
                    lastLog = lastLog,
                    contactTypes = contactTypes,
                    onClick = { onPersonClick(person.id) }
                )
            }

            item {
                Text("Temas geçmişi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (groupEvents.isEmpty()) {
                item {
                    Text(
                        "Bu grup için temas kaydı yok.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(groupEvents, key = { "${it.timestamp}-${it.type}-${it.note}-${it.ids.joinToString("-")}" }) { event ->
                GroupEventCard(
                    group = event,
                    people = people,
                    contactTypes = contactTypes,
                    onEdit = { editingEvent = event },
                    onDelete = { deletingEvent = event }
                )
            }
        }

        if (showExistingPersonSheet) {
            ExistingPersonSheet(
                people = people.filter { it.waveId != wave.id },
                onDismiss = { showExistingPersonSheet = false },
                onSave = { personIds ->
                    viewModel.movePeopleToWave(personIds, wave.id)
                    showExistingPersonSheet = false
                }
            )
        }

        if (showGroupLogSheet) {
            GroupContactLogSheet(
                groupPeople = groupPeople,
                contactTypes = activeContactTypes,
                onDismiss = { showGroupLogSheet = false },
                onSave = { personIds, type, note, timestamp ->
                    viewModel.logInteractions(personIds, type, note, timestamp)
                    showGroupLogSheet = false
                }
            )
        }

        editingEvent?.let { event ->
            EditGroupEventSheet(
                group = event,
                contactTypes = contactTypes,
                onDismiss = { editingEvent = null },
                onSave = { updatedLogs ->
                    viewModel.updateInteractionLogs(updatedLogs)
                    editingEvent = null
                }
            )
        }

        deletingEvent?.let { event ->
            DeleteGroupEventSheet(
                group = event,
                onDismiss = { deletingEvent = null },
                onDelete = {
                    viewModel.deleteInteractionLogs(event.ids)
                    deletingEvent = null
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupActionsCard(
    memberCount: Int,
    frequencyDays: Int,
    onNewPerson: () -> Unit,
    onExistingPerson: () -> Unit,
    onLogGroup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "$memberCount kişi · $frequencyDays günde bir",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onLogGroup, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.EditNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Etkinlik ekle")
                }
                OutlinedButton(onClick = onNewPerson, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yeni kişi")
                }
                OutlinedButton(onClick = onExistingPerson, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Group, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mevcut kişi")
                }
            }
        }
    }
}

@Composable
private fun GroupMemberCard(
    person: Person,
    rhythmLabel: String,
    daysSince: Long,
    effectiveFrequencyDays: Int,
    lastLog: InteractionLog?,
    contactTypes: List<ContactType>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        rhythmLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                "$daysSince gün önce temas · ritim $effectiveFrequencyDays gün",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            lastLog?.let {
                Text(
                    "Son kayıt: ${interactionTypeLabel(it.type, contactTypes)} · ${formatShortDate(it.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyGroupCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            "Bu grupta henüz kişi yok.",
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExistingPersonSheet(
    people: List<Person>,
    onDismiss: () -> Unit,
    onSave: (List<Int>) -> Unit
) {
    var selectedPersonIds by remember(people) { mutableStateOf(emptySet<Int>()) }
    val sortedPeople = people.sortedByTurkish { it.name }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Mevcut kişileri ekle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Birden fazla kişiyi seçip gruba tek seferde ekleyebilirsin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (people.isEmpty()) {
                Text("Gruba eklenebilecek başka kişi yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (sortedPeople.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selectedPersonIds.size} kişi seçili", style = MaterialTheme.typography.labelLarge)
                    TextButton(
                        onClick = {
                            selectedPersonIds = if (selectedPersonIds.size == sortedPeople.size) {
                                emptySet()
                            } else {
                                sortedPeople.map { it.id }.toSet()
                            }
                        }
                    ) {
                        Text(if (selectedPersonIds.size == sortedPeople.size) "Temizle" else "Tümünü seç")
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sortedPeople, key = { it.id }) { person ->
                    val checked = person.id in selectedPersonIds
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (checked) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        onClick = {
                            selectedPersonIds = if (checked) {
                                selectedPersonIds - person.id
                            } else {
                                selectedPersonIds + person.id
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                            Text(person.name, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = selectedPersonIds.isNotEmpty(),
                    onClick = { onSave(selectedPersonIds.toList()) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Gruba ekle")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GroupContactLogSheet(
    groupPeople: List<Person>,
    contactTypes: List<ContactType>,
    onDismiss: () -> Unit,
    onSave: (List<Int>, String, String, Long) -> Unit
) {
    val typeOptions = contactTypes.ifEmpty { DefaultContactTypes.all }
    var selectedTypeKey by remember(typeOptions) { mutableStateOf(typeOptions.first().key) }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedPersonIds by remember(groupPeople) { mutableStateOf(groupPeople.map { it.id }.toSet()) }
    var note by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    val selectedType = typeOptions.find { it.key == selectedTypeKey } ?: typeOptions.first()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Etkinlik ekle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Bu gruptan katılan kişileri seç; tarih, tür ve not hepsine tek seferde kaydedilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                OutlinedTextField(
                    value = selectedType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("İletişim türü") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    typeOptions.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                selectedTypeKey = type.key
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            DatePickerField(
                label = "Tarih",
                selectedMillis = selectedTimestamp,
                onDateSelected = { selectedTimestamp = it }
            )

            Text("Katılımcılar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            LazyColumn(
                modifier = Modifier.heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(groupPeople, key = { it.id }) { person ->
                    val checked = person.id in selectedPersonIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                        Text(person.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Not") },
                placeholder = { Text("Buluşma, arama veya mesaj notu") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = selectedPersonIds.isNotEmpty(),
                    onClick = {
                        onSave(selectedPersonIds.toList(), selectedType.key, note, selectedTimestamp)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Kaydet")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
