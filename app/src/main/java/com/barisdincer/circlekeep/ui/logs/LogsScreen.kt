package com.barisdincer.circlekeep.ui.logs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.components.DatePickerField
import com.barisdincer.circlekeep.ui.components.InteractionEventGroup
import com.barisdincer.circlekeep.ui.components.interactionEventGroups
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: NetworkViewModel, onBack: (() -> Unit)? = null) {
    val logs by viewModel.allInteractions.collectAsState()
    val people by viewModel.people.collectAsState()
    val waves by viewModel.waves.collectAsState()
    val contactTypes by viewModel.contactTypes.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var sheetState by remember { mutableStateOf<LogSheetState?>(null) }
    val eventGroups = remember(logs) { interactionEventGroups(logs) }

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
                    title = { Text("Temas logları", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                            }
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null)
                        Text(
                            "${eventGroups.size} etkinlik · ${logs.size} temas kaydı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (logs.isEmpty()) {
                item {
                    Text(
                        "Henüz temas kaydı yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(eventGroups, key = { "${it.timestamp}-${it.type}-${it.note}-${it.ids.joinToString("-")}" }) { group ->
                EventLogCard(
                    group = group,
                    people = people,
                    waves = waves,
                    contactTypes = contactTypes,
                    onEdit = { sheetState = LogSheetState.Edit(group) },
                    onDelete = { sheetState = LogSheetState.Delete(group) }
                )
            }
        }

        sheetState?.let { sheet ->
            when (sheet) {
                is LogSheetState.Edit -> EditEventSheet(
                    group = sheet.group,
                    contactTypes = contactTypes,
                    onDismiss = { sheetState = null },
                    onSave = { updatedLogs ->
                        viewModel.updateInteractionLogs(updatedLogs)
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
    group: InteractionEventGroup,
    people: List<Person>,
    waves: List<Wave>,
    contactTypes: List<ContactType>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val participants = group.personIds.mapNotNull { id -> people.find { it.id == id } }
    val participantText = participants.joinToString(", ") { it.name }.ifBlank { "Silinmiş kişiler" }
    val groupNames = participants
        .map { person -> waves.find { it.id == person.waveId }?.name ?: "Grup yok" }
        .distinct()
        .joinToString(", ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${interactionTypeLabel(group.type, contactTypes)} · ${group.participantCount} kişi",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEventSheet(
    group: InteractionEventGroup,
    contactTypes: List<ContactType>,
    onDismiss: () -> Unit,
    onSave: (List<InteractionLog>) -> Unit
) {
    val typeOptions = contactTypes.ifEmpty { DefaultContactTypes.all }
    var selectedTypeKey by remember(group.ids, typeOptions) { mutableStateOf(group.type) }
    var timestamp by remember(group.ids) { mutableStateOf(group.timestamp) }
    var note by remember(group.ids) { mutableStateOf(group.note) }
    var typeExpanded by remember { mutableStateOf(false) }
    val selectedType = typeOptions.find { it.key == selectedTypeKey }
        ?: DefaultContactTypes.all.first { it.key == DefaultContactTypes.CALL }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Etkinliği düzenle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "${group.participantCount} kişi için tarih, tür ve not birlikte güncellenir.",
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
                selectedMillis = timestamp,
                onDateSelected = { timestamp = it }
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Not") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(group.logs.map { it.copy(type = selectedTypeKey, timestamp = timestamp, note = note) })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteEventSheet(
    group: InteractionEventGroup,
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
                OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(8.dp)) {
                    Text("Sil")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private sealed interface LogSheetState {
    data class Edit(val group: InteractionEventGroup) : LogSheetState
    data class Delete(val group: InteractionEventGroup) : LogSheetState
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
