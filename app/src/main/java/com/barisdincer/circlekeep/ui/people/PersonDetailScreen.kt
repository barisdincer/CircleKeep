package com.barisdincer.circlekeep.ui.people

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.ui.NetworkViewModel
import java.text.SimpleDateFormat
import androidx.compose.foundation.text.KeyboardOptions
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(personId: Int, viewModel: NetworkViewModel, onBack: () -> Unit) {
    val people by viewModel.people.collectAsState()
    val person = people.find { it.id == personId }
    val interactions by viewModel.allInteractions.collectAsState()
    val contactTypes by viewModel.contactTypes.collectAsState()
    val activeContactTypes by viewModel.activeContactTypes.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val personInteractions = interactions.filter { it.personId == personId }.sortedByDescending { it.timestamp }

    var editReminderEnabled by remember { mutableStateOf(person?.reminderEnabled ?: true) }
    var editPreferredContactTypeKey by remember { mutableStateOf(person?.preferredContactTypeKey.orEmpty()) }
    var editNotes by remember { mutableStateOf(person?.notes ?: "") }
    var editTags by remember { mutableStateOf(person?.tags ?: "") }
    var editCustomFrequency by remember { mutableStateOf(person?.customFrequencyDays?.toString().orEmpty()) }
    var editMemoryNotes by remember { mutableStateOf(person?.memoryNotes ?: "") }
    var editNextHint by remember { mutableStateOf(person?.nextConversationHint ?: "") }
    var editImportantLabel by remember { mutableStateOf(person?.importantDateLabel ?: "") }
    var editImportantDate by remember { mutableStateOf(person?.importantDateMillis?.let { dateInputFormat().format(Date(it)) }.orEmpty()) }
    var typeExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(person) {
        if (person != null) {
            editReminderEnabled = person.reminderEnabled
            editPreferredContactTypeKey = person.preferredContactTypeKey
            editNotes = person.notes
            editTags = person.tags
            editCustomFrequency = person.customFrequencyDays?.toString().orEmpty()
            editMemoryNotes = person.memoryNotes
            editNextHint = person.nextConversationHint
            editImportantLabel = person.importantDateLabel
            editImportantDate = person.importantDateMillis?.let { dateInputFormat().format(Date(it)) }.orEmpty()
        }
    }

    LaunchedEffect(uiMessage) {
        val message = uiMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearUiMessage()
    }

    if (person == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Kişi bulunamadı")
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(person.name) },
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    IconButton(onClick = {
                         val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phoneNumber}"))
                         context.startActivity(intent)
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = {
                         val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${person.phoneNumber}"))
                         context.startActivity(intent)
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Sms, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = {
                         val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                         context.startActivity(intent)
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hatırlatma ritmi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hatırlatmalar", fontWeight = FontWeight.Medium)
                                Text(
                                    if (editReminderEnabled) "Hal hatır listesinde" else "Bu kişi için duraklatıldı",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = editReminderEnabled,
                                onCheckedChange = { editReminderEnabled = it }
                            )
                        }
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = !typeExpanded }
                        ) {
                            val currentType = contactTypes.find { it.key == editPreferredContactTypeKey }
                                ?: activeContactTypes.firstOrNull()
                            OutlinedTextField(
                                value = currentType?.label ?: "Arama",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tercih edilen temas") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                activeContactTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.label) },
                                        onClick = {
                                            editPreferredContactTypeKey = type.key
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = editCustomFrequency,
                            onValueChange = {
                                editCustomFrequency = it.filter { char -> char.isDigit() }
                            },
                            label = { Text("Kişiye özel ritim") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Boşsa grup gün sayısı kullanılır") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        person.lastCallLogSyncDate?.let { syncDate ->
                            val syncDateStr = SimpleDateFormat("d MMM yyyy HH:mm", Locale("tr", "TR")).format(Date(syncDate))
                            Text(
                                "Son arama eşleşmesi: $syncDateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = editTags,
                            onValueChange = { editTags = it },
                            label = { Text("Etiketler") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Aile, okul, iş") }
                        )
                        Button(
                            onClick = {
                                viewModel.updatePerson(
                                    person.copy(
                                        reminderEnabled = editReminderEnabled,
                                        preferredContactTypeKey = editPreferredContactTypeKey.ifBlank { person.preferredContactTypeKey },
                                        customFrequencyDays = editCustomFrequency.toIntOrNull()?.takeIf { days -> days > 0 },
                                        tags = editTags
                                    )
                                )
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ritmi kaydet")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Son temas hafızası", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = editNextHint,
                            onValueChange = { editNextHint = it },
                            label = { Text("Bir dahaki sefere sor") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Nasıl gidiyor, yeni iş nasıl?") }
                        )
                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("Notlar") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 5
                        )
                        OutlinedTextField(
                            value = editMemoryNotes,
                            onValueChange = { editMemoryNotes = it },
                            label = { Text("Hafıza notu") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 5,
                            placeholder = { Text("Sevdiği şeyler, hatırlanacak küçük detaylar") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = editImportantLabel,
                                onValueChange = { editImportantLabel = it },
                                label = { Text("Önemli tarih") },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Doğum günü") }
                            )
                            OutlinedTextField(
                                value = editImportantDate,
                                onValueChange = { editImportantDate = it },
                                label = { Text("Tarih") },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("2026-06-15") }
                            )
                        }
                        val parsedImportantDate = parseDateInput(editImportantDate)
                        val canSaveMemory = editImportantDate.isBlank() || parsedImportantDate != null
                        Button(
                            enabled = canSaveMemory,
                            onClick = {
                                viewModel.updatePerson(
                                    person.copy(
                                        nextConversationHint = editNextHint,
                                        notes = editNotes,
                                        memoryNotes = editMemoryNotes,
                                        importantDateLabel = editImportantLabel,
                                        importantDateMillis = parsedImportantDate
                                    )
                                )
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Hafızayı kaydet")
                        }
                        if (!canSaveMemory) {
                            Text(
                                "Tarih biçimi 2026-06-15 gibi olmalı.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                Text("Temas geçmişi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            }

            if (personInteractions.isEmpty()) {
                item {
                    Text("Henüz temas kaydı yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(personInteractions) { log ->
                val dateStr = SimpleDateFormat("d MMM yyyy HH:mm", Locale("tr", "TR")).format(Date(log.timestamp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(interactionTypeLabel(log.type, contactTypes), fontWeight = FontWeight.Bold)
                            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (log.note.isNotBlank()) {
                            Text(log.note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun interactionTypeLabel(type: String, contactTypes: List<com.barisdincer.circlekeep.data.ContactType>): String {
    return contactTypes.find { it.key == type }?.label ?: when (type) {
        "CALL" -> "Arama"
        "MESSAGE" -> "Mesaj"
        "MEETING" -> "Buluşma"
        else -> type
    }
}

private fun dateInputFormat(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
}

private fun parseDateInput(input: String): Long? {
    return runCatching { dateInputFormat().parse(input)?.time }.getOrNull()
}
