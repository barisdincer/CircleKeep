package com.barisdincer.circlekeep.ui.logs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.data.presentation.InteractionLogEvent
import com.barisdincer.circlekeep.data.presentation.InteractionLogQuery
import com.barisdincer.circlekeep.data.presentation.InteractionLogView
import com.barisdincer.circlekeep.data.presentation.buildInteractionLogPresentation
import com.barisdincer.circlekeep.data.sortedByTurkish
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.components.DatePickerField
import com.barisdincer.circlekeep.ui.design.CircleCard
import com.barisdincer.circlekeep.ui.design.CircleChip
import com.barisdincer.circlekeep.ui.design.CircleEmptyState
import com.barisdincer.circlekeep.ui.design.CircleFilterOption
import com.barisdincer.circlekeep.ui.design.CircleFilterRow
import com.barisdincer.circlekeep.ui.design.CirclePrimaryButton
import com.barisdincer.circlekeep.ui.design.CircleRadius
import com.barisdincer.circlekeep.ui.design.CircleScreenScaffold
import com.barisdincer.circlekeep.ui.design.CircleSearchField
import com.barisdincer.circlekeep.ui.design.CircleSectionHeader
import com.barisdincer.circlekeep.ui.design.CircleSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogsScreen(viewModel: NetworkViewModel, onBack: (() -> Unit)? = null) {
    val logs by viewModel.allInteractions.collectAsState()
    val people by viewModel.people.collectAsState()
    val waves by viewModel.waves.collectAsState()
    val contactTypes by viewModel.contactTypes.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var sheetState by remember { mutableStateOf<LogSheetState?>(null) }
    var searchTerm by rememberSaveable { mutableStateOf("") }
    var selectedView by rememberSaveable { mutableStateOf(InteractionLogView.ALL) }
    val logPresentation = remember(logs, people, waves, contactTypes, searchTerm, selectedView) {
        buildInteractionLogPresentation(
            logs = logs,
            people = people,
            waves = waves,
            contactTypes = contactTypes,
            query = InteractionLogQuery(searchTerm = searchTerm, view = selectedView)
        )
    }

    LaunchedEffect(uiMessage) {
        val message = uiMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearUiMessage()
    }

    CircleScreenScaffold(
        title = "Temas logları",
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = CircleSpacing.md),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = CircleSpacing.xs, bottom = CircleSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
        ) {
            item {
                CircleCard(containerColor = MaterialTheme.colorScheme.secondaryContainer, border = null) {
                    Row(
                        modifier = Modifier.padding(CircleSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            "${logPresentation.eventCount} etkinlik · ${logPresentation.filteredRecordCount}/${logPresentation.totalRecordCount} temas kaydı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            item {
                CircleSearchField(
                    value = searchTerm,
                    onValueChange = { searchTerm = it },
                    placeholder = "Kişi, grup, tür veya not ara"
                )
            }

            item {
                CircleFilterRow(
                    options = listOf(
                        CircleFilterOption(InteractionLogView.ALL, "Tümü"),
                        CircleFilterOption(InteractionLogView.LAST_30_DAYS, "30 gün"),
                        CircleFilterOption(InteractionLogView.CALLS, "Arama"),
                        CircleFilterOption(InteractionLogView.MESSAGES, "Mesaj"),
                        CircleFilterOption(InteractionLogView.MEETINGS, "Buluşma")
                    ),
                    selected = selectedView,
                    onSelected = { selectedView = it }
                )
            }

            item {
                CircleSectionHeader(
                    title = "Etkinlikler",
                    count = logPresentation.eventCount,
                    subtitle = "Aynı gün, tür ve notla eklenen temaslar tek etkinlik olarak gruplanır."
                )
            }

            if (logPresentation.events.isEmpty()) {
                item {
                    CircleEmptyState(
                        title = if (logs.isEmpty()) "Henüz temas kaydı yok" else "Bu filtrede etkinlik yok",
                        body = if (logs.isEmpty()) {
                            "Dashboard’daki etkinlik ekleme akışıyla ilk teması kaydedebilirsin."
                        } else {
                            "Aramayı veya hazır görünümü değiştirerek kayıtları genişlet."
                        }
                    )
                }
            }

            items(logPresentation.events, key = { "${it.timestamp}-${it.type}-${it.note}-${it.ids.joinToString("-")}" }) { group ->
                EventLogCard(
                    group = group,
                    onEdit = { sheetState = LogSheetState.Edit(group) },
                    onDelete = { sheetState = LogSheetState.Delete(group) }
                )
            }
        }

        sheetState?.let { sheet ->
            when (sheet) {
                is LogSheetState.Edit -> EditEventSheet(
                    group = sheet.group,
                    people = people,
                    contactTypes = contactTypes,
                    onDismiss = { sheetState = null },
                    onSave = { personIds, type, note, timestamp ->
                        viewModel.replaceInteractionEvent(sheet.group.ids, personIds, type, note, timestamp)
                        sheetState = null
                    }
                )

                is LogSheetState.Delete -> DeleteEventSheet(
                    group = sheet.group,
                    onDismiss = { sheetState = null },
                    onDelete = {
                        viewModel.deleteInteractionLogs(sheet.group.ids)
                        sheetState = null
                    }
                )
            }
        }
    }
}

@Composable
private fun EventLogCard(
    group: InteractionLogEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val participantText = group.participantNames.joinToString(", ").ifBlank { "Silinmiş kişiler" }
    val groupNames = group.waveNames.joinToString(", ")

    CircleCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CircleSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${group.typeLabel} · ${group.participantCount} kişi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatLogDate(group.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    participantText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (groupNames.isNotBlank()) {
                    Text(
                        groupNames,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (group.note.isNotBlank()) {
                    Text(group.note, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Etkinliği düzenle")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Etkinliği sil")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditEventSheet(
    group: InteractionLogEvent,
    people: List<Person>,
    contactTypes: List<ContactType>,
    onDismiss: () -> Unit,
    onSave: (List<Int>, String, String, Long) -> Unit
) {
    val typeOptions = contactTypes.ifEmpty { DefaultContactTypes.all }
    var selectedTypeKey by remember(group.ids, typeOptions) { mutableStateOf(group.type) }
    var timestamp by remember(group.ids) { mutableStateOf(group.timestamp) }
    var note by remember(group.ids) { mutableStateOf(group.note) }
    var selectedPersonIds by remember(group.ids) { mutableStateOf(group.personIds.toSet()) }
    var typeExpanded by remember { mutableStateOf(false) }
    val selectedType = typeOptions.find { it.key == selectedTypeKey }
        ?: DefaultContactTypes.all.first { it.key == DefaultContactTypes.CALL }
    val selectablePeople = remember(people) { people.sortedByTurkish { it.name } }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Etkinliği düzenle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Kişi, tarih, tür ve not birlikte güncellenir. Yanlışlıkla eklenenleri çıkarabilir, eksik kalan kişileri ekleyebilirsin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${selectedPersonIds.size} kişi seçili",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectablePeople.forEach { person ->
                        val selected = person.id in selectedPersonIds
                        CircleChip(
                            selected = selected,
                            label = person.name,
                            onClick = {
                                selectedPersonIds = if (selected) {
                                    selectedPersonIds - person.id
                                } else {
                                    selectedPersonIds + person.id
                                }
                            },
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                OutlinedTextField(
                    value = selectedType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("İletişim türü") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                    shape = RoundedCornerShape(CircleRadius.control)
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
                selectedMillis = timestamp,
                onDateSelected = { timestamp = it }
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Not") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(CircleRadius.control)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç")
                }
                Spacer(modifier = Modifier.width(8.dp))
                CirclePrimaryButton(
                    text = "Kaydet",
                    enabled = selectedPersonIds.isNotEmpty(),
                    onClick = {
                        onSave(selectedPersonIds.toList(), selectedTypeKey, note, timestamp)
                    },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteEventSheet(
    group: InteractionLogEvent,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Etkinlik silinsin mi?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "${formatLogDate(group.timestamp)} tarihli ${group.participantCount} kişilik etkinlik silinecek. İlgili son temas tarihleri yeniden hesaplanır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(CircleRadius.control),
                    modifier = Modifier.height(48.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private sealed interface LogSheetState {
    data class Edit(val group: InteractionLogEvent) : LogSheetState
    data class Delete(val group: InteractionLogEvent) : LogSheetState
}

private fun interactionTypeLabel(type: String, contactTypes: List<ContactType>): String {
    return contactTypes.find { it.key == type }?.label ?: when (type) {
        DefaultContactTypes.CALL -> "Arama"
        DefaultContactTypes.MESSAGE -> "Mesaj"
        DefaultContactTypes.MEETING -> "Buluşma"
        else -> type
    }
}

private fun formatLogDate(timestamp: Long): String {
    return SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(timestamp))
}
