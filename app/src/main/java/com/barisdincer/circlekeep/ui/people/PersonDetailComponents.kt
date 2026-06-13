package com.barisdincer.circlekeep.ui.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.barisdincer.circlekeep.ui.components.DatePickerField
import com.barisdincer.circlekeep.ui.design.CircleAvatar
import com.barisdincer.circlekeep.ui.design.CircleCard
import com.barisdincer.circlekeep.ui.design.CirclePrimaryButton
import com.barisdincer.circlekeep.ui.design.CircleRadius
import com.barisdincer.circlekeep.ui.design.CircleSpacing
import com.barisdincer.circlekeep.ui.design.CircleStatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PersonDetailTabs(selectedTab: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("Genel", "Ritim", "Hafıza", "Geçmiş")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CircleRadius.control),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onSelect(index) },
                    text = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
    }
}

@Composable
internal fun PersonHeroCard(
    name: String,
    phoneNumber: String,
    groupName: String,
    lastInteractionDate: Long,
    contactTypeCount: Int,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onEmail: () -> Unit
) {
    CircleCard {
        Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircleAvatar(name = name, size = 60.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        phoneNumber.ifBlank { "Telefon eklenmemiş" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircleStatusPill(
                            label = groupName,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        CircleStatusPill(
                            label = "$contactTypeCount tür",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CircleRadius.control),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Son temas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatPersonDate(lastInteractionDate),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroActionButton(Icons.Default.Call, "Ara", onCall, Modifier.weight(1f))
                HeroActionButton(Icons.Default.Sms, "Mesaj", onMessage, Modifier.weight(1f))
                HeroActionButton(Icons.Default.Email, "E-posta", onEmail, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(CircleRadius.control),
        contentPadding = PaddingValues(horizontal = 6.dp),
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditPersonLogSheet(
    log: InteractionLog,
    contactTypes: List<ContactType>,
    onDismiss: () -> Unit,
    onSave: (InteractionLog) -> Unit
) {
    val typeOptions = contactTypes.ifEmpty { DefaultContactTypes.all }
    var selectedTypeKey by remember(log.id, typeOptions) { mutableStateOf(log.type) }
    var timestamp by remember(log.id) { mutableStateOf(log.timestamp) }
    var note by remember(log.id) { mutableStateOf(log.note) }
    var typeExpanded by remember { mutableStateOf(false) }
    val selectedType = typeOptions.find { it.key == selectedTypeKey }
        ?: DefaultContactTypes.all.first { it.key == DefaultContactTypes.CALL }

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
            Text("Temas kaydını düzenle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

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
                    onClick = { onSave(log.copy(type = selectedTypeKey, timestamp = timestamp, note = note)) },
                )
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

private fun formatPersonDate(timestamp: Long): String {
    return SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(timestamp))
}
