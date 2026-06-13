package com.barisdincer.circlekeep.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.data.contactActionLabel
import com.barisdincer.circlekeep.data.sortedByTurkish
import com.barisdincer.circlekeep.ui.PersonWithWave
import com.barisdincer.circlekeep.ui.components.DatePickerField
import com.barisdincer.circlekeep.ui.design.CircleAvatar
import com.barisdincer.circlekeep.ui.design.CirclePrimaryButton
import com.barisdincer.circlekeep.ui.design.CircleRadius
import com.barisdincer.circlekeep.ui.design.CircleSearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactLogSheet(
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
    var selectedTimestamp by remember(sheet) { mutableStateOf(System.currentTimeMillis()) }
    var note by remember(sheet) { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    val typeOptions = contactTypes.ifEmpty { DefaultContactTypes.all }
    val selectedType = typeOptions.find { it.key == selectedTypeKey } ?: typeOptions.first()
    val editableParticipants = sheet is ContactLogSheetState.Batch
    val selectedPeople = people.filter { it.id in selectedPersonIds }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (editableParticipants) "Etkinlik ekle" else "${selectedPeople.firstOrNull()?.name ?: "Kişi"} ile temas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (editableParticipants) {
                        "Katılan kişileri seç; kayıt sadece seçilen kişilerin son temasını günceller."
                    } else {
                        "Türü, tarihi ve gerekiyorsa notu kontrol edip kaydet."
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
                    shape = RoundedCornerShape(CircleRadius.control)
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    typeOptions.forEach { type ->
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

            DatePickerField(
                label = "Temas tarihi",
                selectedMillis = selectedTimestamp,
                onDateSelected = { selectedTimestamp = it }
            )

            if (editableParticipants) {
                ParticipantPicker(
                    people = people,
                    waves = waves,
                    selectedPersonIds = selectedPersonIds,
                    groupExpanded = groupExpanded,
                    onGroupExpandedChange = { groupExpanded = it },
                    onSelectionChange = { selectedPersonIds = it }
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CircleRadius.control),
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
                placeholder = { Text("Ne oldu, kim katıldı, ne konuşuldu?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(CircleRadius.control)
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
                CirclePrimaryButton(
                    text = contactActionLabel(selectedType.key, selectedType.label),
                    enabled = selectedPersonIds.isNotEmpty(),
                    onClick = {
                        onSave(selectedPersonIds.toList(), selectedType.key, note, selectedTimestamp)
                    },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParticipantPicker(
    people: List<Person>,
    waves: List<Wave>,
    selectedPersonIds: Set<Int>,
    groupExpanded: Boolean,
    onGroupExpandedChange: (Boolean) -> Unit,
    onSelectionChange: (Set<Int>) -> Unit
) {
    val selectedPeople = people.filter { it.id in selectedPersonIds }
    var searchQuery by remember { mutableStateOf("") }
    val visiblePeople = remember(people, searchQuery) {
        val sorted = people.sortedByTurkish { it.name }
        if (searchQuery.isBlank()) {
            sorted
        } else {
            val query = searchQuery.trim()
            sorted.filter { person ->
                person.name.contains(query, ignoreCase = true) ||
                    person.phoneNumber.contains(query, ignoreCase = true)
            }
        }
    }
    ExposedDropdownMenuBox(
        expanded = groupExpanded,
        onExpandedChange = { onGroupExpandedChange(!groupExpanded) }
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
            shape = RoundedCornerShape(CircleRadius.control)
        )
        ExposedDropdownMenu(
            expanded = groupExpanded,
            onDismissRequest = { onGroupExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Tüm seçimi temizle") },
                onClick = {
                    onSelectionChange(emptySet())
                    onGroupExpandedChange(false)
                }
            )
            waves.forEach { wave ->
                val groupIds = people.filter { it.waveId == wave.id }.map { it.id }.toSet()
                DropdownMenuItem(
                    text = { Text("${wave.name} (${groupIds.size} kişi)") },
                    onClick = {
                        onSelectionChange(selectedPersonIds + groupIds)
                        onGroupExpandedChange(false)
                    }
                )
            }
        }
    }

    CircleSearchField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = "Katılımcı ara"
    )

    LazyColumn(
        modifier = Modifier.heightIn(max = 260.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (visiblePeople.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CircleRadius.control),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        "Eşleşen kişi yok",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(visiblePeople, key = { it.id }) { person ->
            val checked = person.id in selectedPersonIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CircleRadius.control))
                    .clickable {
                        onSelectionChange(
                            if (checked) selectedPersonIds - person.id else selectedPersonIds + person.id
                        )
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = {
                        onSelectionChange(
                            if (checked) selectedPersonIds - person.id else selectedPersonIds + person.id
                        )
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                CircleAvatar(name = person.name, size = 36.dp)
                Spacer(modifier = Modifier.width(10.dp))
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
}

internal sealed interface ContactLogSheetState {
    data class Single(val contact: PersonWithWave) : ContactLogSheetState
    data object Batch : ContactLogSheetState
}
