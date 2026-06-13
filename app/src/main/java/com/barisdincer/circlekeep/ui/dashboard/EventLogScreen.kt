package com.barisdincer.circlekeep.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.barisdincer.circlekeep.ui.components.DatePickerField
import com.barisdincer.circlekeep.ui.design.CircleAvatar
import com.barisdincer.circlekeep.ui.design.CircleCard
import com.barisdincer.circlekeep.ui.design.CircleChip
import com.barisdincer.circlekeep.ui.design.CircleEmptyState
import com.barisdincer.circlekeep.ui.design.CircleHeroCard
import com.barisdincer.circlekeep.ui.design.CirclePrimaryButton
import com.barisdincer.circlekeep.ui.design.CircleRadius
import com.barisdincer.circlekeep.ui.design.CircleScreenScaffold
import com.barisdincer.circlekeep.ui.design.CircleSearchField
import com.barisdincer.circlekeep.ui.design.CircleSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventLogScreen(
    people: List<Person>,
    waves: List<Wave>,
    contactTypes: List<ContactType>,
    onBack: () -> Unit,
    onSave: (List<Int>, String, String, Long) -> Unit
) {
    val typeOptions = contactTypes.ifEmpty { DefaultContactTypes.all }
    val sortedPeople = remember(people) { people.sortedByTurkish { it.name } }
    val waveNamesById = remember(waves) { waves.associate { it.id to it.name } }
    var selectedPersonIds by remember(people) { mutableStateOf(emptySet<Int>()) }
    var selectedTypeKey by remember(typeOptions) { mutableStateOf(typeOptions.first().key) }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var participantSearch by remember { mutableStateOf("") }
    val selectedType = typeOptions.find { it.key == selectedTypeKey } ?: typeOptions.first()
    val visiblePeople = remember(sortedPeople, waveNamesById, participantSearch) {
        if (participantSearch.isBlank()) {
            sortedPeople
        } else {
            val query = participantSearch.trim()
            sortedPeople.filter { person ->
                person.name.contains(query, ignoreCase = true) ||
                    person.phoneNumber.contains(query, ignoreCase = true) ||
                    waveNamesById[person.waveId].orEmpty().contains(query, ignoreCase = true)
            }
        }
    }

    CircleScreenScaffold(title = "Etkinlik ekle", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = CircleSpacing.md),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = CircleSpacing.xs, bottom = CircleSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
        ) {
            item {
                EventIntroCard(
                    selectedCount = selectedPersonIds.size,
                    selectedTypeLabel = selectedType.label
                )
            }

            item {
                CircleCard {
                    Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)) {
                        Text("Kayıt bilgisi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    }
                }
            }

            item {
                ParticipantControls(
                    people = people,
                    waves = waves,
                    searchQuery = participantSearch,
                    onSearchQueryChange = { participantSearch = it },
                    selectedPersonIds = selectedPersonIds,
                    onSelectionChange = { selectedPersonIds = it }
                )
            }

            if (visiblePeople.isEmpty()) {
                item {
                    CircleEmptyState(
                        title = "Eşleşen kişi yok",
                        body = "Aramayı temizleyerek tüm kişileri yeniden görebilirsin."
                    )
                }
            }

            items(visiblePeople, key = { it.id }) { person ->
                val checked = person.id in selectedPersonIds
                ParticipantRow(
                    person = person,
                    groupName = waves.find { it.id == person.waveId }?.name ?: "Grup yok",
                    checked = checked,
                    onToggle = {
                        selectedPersonIds = if (checked) {
                            selectedPersonIds - person.id
                        } else {
                            selectedPersonIds + person.id
                        }
                    }
                )
            }

            item {
                CircleCard {
                    Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Kısa not", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Not") },
                            placeholder = { Text("Ne oldu, kim katıldı, ne konuşuldu?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            shape = RoundedCornerShape(CircleRadius.control)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onBack) {
                                Text("Vazgeç")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            CirclePrimaryButton(
                                text = contactActionLabel(selectedType.key, selectedType.label),
                                icon = Icons.Default.Group,
                                enabled = selectedPersonIds.isNotEmpty(),
                                onClick = {
                                    onSave(selectedPersonIds.toList(), selectedType.key, note, selectedTimestamp)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventIntroCard(selectedCount: Int, selectedTypeLabel: String) {
    CircleHeroCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(CircleRadius.control))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Toplu temas kaydı",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Bir buluşma, arama veya mesajı katılan herkese tek seferde işle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventPill("$selectedCount kişi")
            EventPill(selectedTypeLabel)
        }
    }
}

@Composable
private fun EventPill(text: String) {
    Surface(shape = RoundedCornerShape(CircleRadius.pill), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParticipantControls(
    people: List<Person>,
    waves: List<Wave>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedPersonIds: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit
) {
    CircleCard {
        Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Katılımcılar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (selectedPersonIds.isEmpty()) "En az bir kişi seç." else "${selectedPersonIds.size} kişi seçildi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    enabled = selectedPersonIds.isNotEmpty(),
                    onClick = { onSelectionChange(emptySet()) }
                ) {
                    Text("Temizle")
                }
            }
            CircleSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Katılımcı, telefon veya grup ara"
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CircleChip(
                    selected = selectedPersonIds.size == people.size && people.isNotEmpty(),
                    label = "Herkes",
                    onClick = {
                        onSelectionChange(
                            if (selectedPersonIds.size == people.size) emptySet() else people.map { it.id }.toSet()
                        )
                    }
                )
                waves.forEach { wave ->
                    val groupIds = people.filter { it.waveId == wave.id }.map { it.id }.toSet()
                    if (groupIds.isNotEmpty()) {
                        CircleChip(
                            selected = groupIds.all { it in selectedPersonIds },
                            label = "${wave.name} (${groupIds.size})",
                            onClick = {
                                onSelectionChange(
                                    if (groupIds.all { it in selectedPersonIds }) {
                                        selectedPersonIds - groupIds
                                    } else {
                                        selectedPersonIds + groupIds
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(
    person: Person,
    groupName: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(CircleRadius.control)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape),
        onClick = onToggle,
        shape = shape,
        color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircleAvatar(name = person.name, size = 38.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(person.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(groupName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}
