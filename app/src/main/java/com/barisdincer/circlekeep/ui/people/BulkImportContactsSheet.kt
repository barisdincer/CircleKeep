package com.barisdincer.circlekeep.ui.people

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.device.PhonebookContact
import com.barisdincer.circlekeep.ui.design.CircleAvatar
import com.barisdincer.circlekeep.ui.design.CircleEmptyState
import com.barisdincer.circlekeep.ui.design.CirclePrimaryButton
import com.barisdincer.circlekeep.ui.design.CircleRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BulkImportContactsDialog(
    contacts: List<PhonebookContact>,
    waves: List<Wave>,
    onDismiss: () -> Unit,
    onImport: (List<PhonebookContact>, Int?) -> Unit
) {
    var selectedKeys by remember(contacts) {
        mutableStateOf(contacts.take(25).map { it.selectionKey }.toSet())
    }
    var selectedWaveId by remember(waves) { mutableStateOf(waves.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }
    val selectedContacts = contacts.filter { it.selectionKey in selectedKeys }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Rehberden içe aktar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${contacts.size} yeni kişi bulundu; ${selectedContacts.size} kişi seçili.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                val selectedWaveName = waves.find { it.id == selectedWaveId }?.name ?: "Hatırlatma grubu yok"
                OutlinedTextField(
                    value = selectedWaveName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hatırlatma grubu") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(CircleRadius.control)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Hatırlatma grubu yok") },
                        onClick = {
                            selectedWaveId = null
                            expanded = false
                        }
                    )
                    waves.forEach { wave ->
                        DropdownMenuItem(
                            text = { Text("${wave.name} (${wave.frequencyDays} gün)") },
                            onClick = {
                                selectedWaveId = wave.id
                                expanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedKeys = contacts.map { it.selectionKey }.toSet() }) {
                    Text("Tümünü seç")
                }
                TextButton(onClick = { selectedKeys = emptySet() }) {
                    Text("Temizle")
                }
            }

            if (contacts.isEmpty()) {
                CircleEmptyState(
                    title = "Yeni kişi bulunamadı",
                    body = "Rehberdeki kişiler zaten CircleKeep’te olabilir veya telefon numarası eksik olabilir."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(contacts, key = { it.selectionKey }) { contact ->
                        val checked = contact.selectionKey in selectedKeys
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CircleRadius.control))
                                .background(
                                    if (checked) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    selectedKeys = if (checked) {
                                        selectedKeys - contact.selectionKey
                                    } else {
                                        selectedKeys + contact.selectionKey
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedKeys = if (checked) {
                                        selectedKeys - contact.selectionKey
                                    } else {
                                        selectedKeys + contact.selectionKey
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            CircleAvatar(name = contact.name, size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(contact.name, fontWeight = FontWeight.Medium)
                                Text(
                                    contact.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

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
                    text = "İçe aktar",
                    enabled = selectedContacts.isNotEmpty(),
                    onClick = { onImport(selectedContacts, selectedWaveId) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
