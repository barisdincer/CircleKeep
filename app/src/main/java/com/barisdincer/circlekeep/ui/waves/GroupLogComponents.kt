package com.barisdincer.circlekeep.ui.waves

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.barisdincer.circlekeep.ui.components.InteractionEventGroup
import com.barisdincer.circlekeep.ui.components.DatePickerField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun GroupEventCard(
    group: InteractionEventGroup,
    people: List<Person>,
    contactTypes: List<ContactType>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val participantNames = group.personIds
        .mapNotNull { id -> people.find { it.id == id }?.name }
        .joinToString(", ")
        .ifBlank { "Silinmiş kişiler" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${interactionTypeLabel(group.type, contactTypes)} · ${formatShortDate(group.timestamp)} · ${group.participantCount} kişi",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    participantNames,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    group.note.ifBlank { "Not eklenmemiş." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (group.note.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Etkinliği düzenle")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Etkinliği sil", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditGroupEventSheet(
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
                "${group.participantCount} kişinin temas kaydı birlikte güncellenir.",
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
internal fun DeleteGroupEventSheet(
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
                "${formatShortDate(group.timestamp)} tarihli ${group.participantCount} kişilik etkinlik silinir; ilgili kişi ritimleri yeniden hesaplanır.",
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

internal fun interactionTypeLabel(type: String, contactTypes: List<ContactType>): String {
    return contactTypes.find { it.key == type }?.label ?: when (type) {
        DefaultContactTypes.CALL -> "Arama"
        DefaultContactTypes.MESSAGE -> "Mesaj"
        DefaultContactTypes.MEETING -> "Buluşma"
        else -> type
    }
}

internal fun formatShortDate(timestamp: Long): String {
    return SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(timestamp))
}
