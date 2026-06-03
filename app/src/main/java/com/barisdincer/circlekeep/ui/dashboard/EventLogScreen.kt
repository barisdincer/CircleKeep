package com.barisdincer.circlekeep.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
    var selectedPersonIds by remember(people) { mutableStateOf(emptySet<Int>()) }
    var selectedTypeKey by remember(typeOptions) { mutableStateOf(typeOptions.first().key) }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    val selectedType = typeOptions.find { it.key == selectedTypeKey } ?: typeOptions.first()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Etkinlik ekle", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        androidx.compose.material3.IconButton(onClick = onBack) {
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EventIntroCard(
                    selectedCount = selectedPersonIds.size,
                    selectedTypeLabel = selectedType.label
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                shape = RoundedCornerShape(8.dp)
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
                    selectedPersonIds = selectedPersonIds,
                    onSelectionChange = { selectedPersonIds = it }
                )
            }

            items(sortedPeople, key = { it.id }) { person ->
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            shape = RoundedCornerShape(8.dp)
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
                            Button(
                                enabled = selectedPersonIds.isNotEmpty(),
                                onClick = {
                                    onSave(selectedPersonIds.toList(), selectedType.key, note, selectedTimestamp)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(contactActionLabel(selectedType.key, selectedType.label))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventIntroCard(selectedCount: Int, selectedTypeLabel: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Toplu temas kaydı",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Bir buluşma, arama veya mesajı katılan herkese tek seferde işle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventPill("$selectedCount kişi")
                EventPill(selectedTypeLabel)
            }
        }
    }
}

@Composable
private fun EventPill(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParticipantControls(
    people: List<Person>,
    waves: List<Wave>,
    selectedPersonIds: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedPersonIds.size == people.size && people.isNotEmpty(),
                    onClick = {
                        onSelectionChange(
                            if (selectedPersonIds.size == people.size) emptySet() else people.map { it.id }.toSet()
                        )
                    },
                    label = { Text("Herkes") }
                )
                waves.forEach { wave ->
                    val groupIds = people.filter { it.waveId == wave.id }.map { it.id }.toSet()
                    FilterChip(
                        enabled = groupIds.isNotEmpty(),
                        selected = groupIds.isNotEmpty() && groupIds.all { it in selectedPersonIds },
                        onClick = {
                            onSelectionChange(
                                if (groupIds.all { it in selectedPersonIds }) {
                                    selectedPersonIds - groupIds
                                } else {
                                    selectedPersonIds + groupIds
                                }
                            )
                        },
                        label = { Text("${wave.name} (${groupIds.size})") }
                    )
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        color = if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(person.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(groupName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
