package com.barisdincer.circlekeep.ui.people

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.InteractionLog
import com.barisdincer.circlekeep.ui.components.DatePickerField
import com.barisdincer.circlekeep.ui.design.CircleCard
import com.barisdincer.circlekeep.ui.design.CircleChip
import com.barisdincer.circlekeep.ui.design.CircleDestructiveDialog
import com.barisdincer.circlekeep.ui.design.CirclePrimaryButton
import com.barisdincer.circlekeep.ui.design.CircleRadius
import com.barisdincer.circlekeep.ui.design.CircleScreenScaffold
import com.barisdincer.circlekeep.ui.design.CircleSpacing
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonDetailScreen(
    personId: Int,
    viewModel: NetworkViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val people by viewModel.people.collectAsState()
    val person = people.find { it.id == personId }
    val waves by viewModel.waves.collectAsState()
    val interactions by viewModel.allInteractions.collectAsState()
    val contactTypes by viewModel.contactTypes.collectAsState()
    val activeContactTypes by viewModel.activeContactTypes.collectAsState()
    val contactRhythms by viewModel.personContactRhythms.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val personInteractions = interactions.filter { it.personId == personId }.sortedByDescending { it.timestamp }
    val personRhythms = contactRhythms.filter { it.personId == personId }
    val selectedContactTypeKeys = if (personRhythms.isEmpty()) {
        setOf(person?.preferredContactTypeKey ?: DefaultContactTypes.CALL)
    } else {
        personRhythms.filter { it.isActive }.map { it.contactTypeKey }.toSet()
    }

    var editName by remember { mutableStateOf(person?.name.orEmpty()) }
    var editPhone by remember { mutableStateOf(person?.phoneNumber.orEmpty()) }
    var editWaveId by remember { mutableStateOf(person?.waveId) }
    var editReminderEnabled by remember { mutableStateOf(person?.reminderEnabled ?: true) }
    var editPreferredContactTypeKey by remember { mutableStateOf(person?.preferredContactTypeKey.orEmpty()) }
    var editNotes by remember { mutableStateOf(person?.notes ?: "") }
    var editTags by remember { mutableStateOf(person?.tags ?: "") }
    var editRhythmFrequencies by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var editMemoryNotes by remember { mutableStateOf(person?.memoryNotes ?: "") }
    var editNextHint by remember { mutableStateOf(person?.nextConversationHint ?: "") }
    var editImportantLabel by remember { mutableStateOf(person?.importantDateLabel ?: "") }
    var editImportantDateMillis by remember { mutableStateOf(person?.importantDateMillis) }
    var editingLog by remember { mutableStateOf<InteractionLog?>(null) }
    var deletingLog by remember { mutableStateOf<InteractionLog?>(null) }
    var showDeletePersonDialog by remember { mutableStateOf(false) }
    var waveExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(person, personRhythms, activeContactTypes) {
        if (person != null) {
            val rhythmsByType = personRhythms.associateBy { it.contactTypeKey }
            editName = person.name
            editPhone = person.phoneNumber
            editWaveId = person.waveId
            editReminderEnabled = person.reminderEnabled
            editPreferredContactTypeKey = person.preferredContactTypeKey
            editNotes = person.notes
            editTags = person.tags
            editRhythmFrequencies = activeContactTypes.associate { type ->
                type.key to rhythmsByType[type.key]?.customFrequencyDays?.toString().orEmpty()
            }
            editMemoryNotes = person.memoryNotes
            editNextHint = person.nextConversationHint
            editImportantLabel = person.importantDateLabel
            editImportantDateMillis = person.importantDateMillis
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

    CircleScreenScaffold(
        title = person.name,
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = CircleSpacing.md),
            contentPadding = PaddingValues(top = CircleSpacing.xs, bottom = CircleSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
        ) {
            item {
                PersonHeroCard(
                    name = person.name,
                    phoneNumber = person.phoneNumber,
                    groupName = waves.find { it.id == person.waveId }?.name ?: "Kişiye özel",
                    lastInteractionDate = person.lastInteractionDate,
                    contactTypeCount = selectedContactTypeKeys.size.coerceAtLeast(1),
                    onCall = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phoneNumber}"))
                        context.startActivity(intent)
                    },
                    onMessage = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${person.phoneNumber}"))
                        context.startActivity(intent)
                    },
                    onEmail = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                        context.startActivity(intent)
                    }
                )
            }

            item {
                PersonDetailTabs(selectedTab = selectedTab, onSelect = { selectedTab = it })
            }

            if (selectedTab == 0) {
                item {
                    CircleCard {
                        Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Kişi bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "İsim, telefon ve grup bilgisini buradan düzenle.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("İsim") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(CircleRadius.control)
                            )
                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Telefon") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(CircleRadius.control)
                            )
                            ExposedDropdownMenuBox(
                                expanded = waveExpanded,
                                onExpandedChange = { waveExpanded = !waveExpanded }
                            ) {
                                val selectedWaveName = waves.find { it.id == editWaveId }?.name ?: "Grup yok"
                                OutlinedTextField(
                                    value = selectedWaveName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Grup") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = waveExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                                    shape = RoundedCornerShape(CircleRadius.control)
                                )
                                ExposedDropdownMenu(
                                    expanded = waveExpanded,
                                    onDismissRequest = { waveExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Grup yok") },
                                        onClick = {
                                            editWaveId = null
                                            waveExpanded = false
                                        }
                                    )
                                    waves.forEach { wave ->
                                        DropdownMenuItem(
                                            text = { Text("${wave.name} (${wave.frequencyDays} gün)") },
                                            onClick = {
                                                editWaveId = wave.id
                                                waveExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            CirclePrimaryButton(
                                text = "Kişiyi kaydet",
                                enabled = editName.isNotBlank(),
                                onClick = {
                                    viewModel.updatePerson(
                                        person.copy(
                                            name = editName.trim(),
                                            phoneNumber = editPhone,
                                            waveId = editWaveId
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedButton(
                                onClick = { showDeletePersonDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(CircleRadius.control),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kişiyi sil")
                            }
                        }
                    }
                }
            }

            if (selectedTab == 1) {
                item {
                    CircleCard {
                        Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Hatırlatma ritmi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Bu kişinin hangi temas türleriyle ve kaç günde bir hatırlatılacağını ayarla.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                    label = { Text("Birincil hızlı aksiyon") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                                    shape = RoundedCornerShape(CircleRadius.control)
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
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Takip edilen türler", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    activeContactTypes.forEach { type ->
                                        val selected = type.key in selectedContactTypeKeys
                                        CircleChip(
                                            selected = selected,
                                            label = type.label,
                                            onClick = {
                                                viewModel.setPersonContactRhythmActive(person.id, type.key, !selected)
                                            }
                                        )
                                    }
                                }
                                Text(
                                    "Bir kişide birden fazla tür seçilirse her tür kendi son temasına ve kendi ritmine göre görünür.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Tür bazlı ritimler", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                activeContactTypes
                                    .filter { it.key in selectedContactTypeKeys }
                                    .forEach { type ->
                                        val fallbackDays = person.customFrequencyDays?.takeIf { it > 0 }
                                            ?: waves.find { it.id == person.waveId }?.frequencyDays
                                        OutlinedTextField(
                                            value = editRhythmFrequencies[type.key].orEmpty(),
                                            onValueChange = { value ->
                                                editRhythmFrequencies = editRhythmFrequencies + (type.key to value.filter { char -> char.isDigit() })
                                            },
                                            label = { Text("${type.label} ritmi") },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = {
                                                Text(fallbackDays?.let { "Boşsa $it gün" } ?: "Örn. 7")
                                            },
                                            suffix = { Text("gün") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(CircleRadius.control)
                                        )
                                    }
                                if (selectedContactTypeKeys.isEmpty()) {
                                    Text(
                                        "Ritim vermek için en az bir iletişim türünü takibe al.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        "Örn. Arama 7 gün, Buluşma 30 gün olabilir. Boş alanlar grup veya kişi varsayılanını kullanır.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                                placeholder = { Text("Aile, okul, iş") },
                                shape = RoundedCornerShape(CircleRadius.control)
                            )
                            CirclePrimaryButton(
                                text = "Ritmi kaydet",
                                onClick = {
                                    val selectedKeys = selectedContactTypeKeys.ifEmpty {
                                        setOf(editPreferredContactTypeKey.ifBlank { person.preferredContactTypeKey })
                                    }
                                    val preferredKey = editPreferredContactTypeKey
                                        .ifBlank { person.preferredContactTypeKey }
                                        .takeIf { it in selectedKeys }
                                        ?: selectedKeys.first()
                                    viewModel.updatePersonRhythmSettings(
                                        person.copy(
                                            reminderEnabled = editReminderEnabled,
                                            preferredContactTypeKey = preferredKey,
                                            tags = editTags
                                        ),
                                        selectedKeys.associateWith { key ->
                                            editRhythmFrequencies[key]?.toIntOrNull()?.takeIf { days -> days > 0 }
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            if (selectedTab == 2) {
                item {
                    CircleCard {
                        Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Son temas hafızası", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Bir sonraki konuşma için küçük ipuçları ve kalıcı notlar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedTextField(
                                value = editNextHint,
                                onValueChange = { editNextHint = it },
                                label = { Text("Bir dahaki sefere sor") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Nasıl gidiyor, yeni iş nasıl?") },
                                shape = RoundedCornerShape(CircleRadius.control)
                            )
                            OutlinedTextField(
                                value = editNotes,
                                onValueChange = { editNotes = it },
                                label = { Text("Notlar") },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                maxLines = 5,
                                shape = RoundedCornerShape(CircleRadius.control)
                            )
                            OutlinedTextField(
                                value = editMemoryNotes,
                                onValueChange = { editMemoryNotes = it },
                                label = { Text("Hafıza notu") },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                maxLines = 5,
                                placeholder = { Text("Sevdiği şeyler, hatırlanacak küçük detaylar") },
                                shape = RoundedCornerShape(CircleRadius.control)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = editImportantLabel,
                                    onValueChange = { editImportantLabel = it },
                                    label = { Text("Önemli tarih") },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Doğum günü") },
                                    shape = RoundedCornerShape(CircleRadius.control)
                                )
                                DatePickerField(
                                    label = "Tarih",
                                    selectedMillis = editImportantDateMillis,
                                    onDateSelected = { editImportantDateMillis = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = "Tarih seç",
                                    clearLabel = "Tarihi temizle",
                                    onClear = { editImportantDateMillis = null }
                                )
                            }
                            CirclePrimaryButton(
                                text = "Hafızayı kaydet",
                                onClick = {
                                    viewModel.updatePerson(
                                        person.copy(
                                            nextConversationHint = editNextHint,
                                            notes = editNotes,
                                            memoryNotes = editMemoryNotes,
                                            importantDateLabel = editImportantLabel,
                                            importantDateMillis = editImportantDateMillis
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            if (selectedTab == 3) {
                item {
                    Text("Temas geçmişi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = CircleSpacing.xs))
                }

                if (personInteractions.isEmpty()) {
                    item {
                        Text("Henüz temas kaydı yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                items(personInteractions) { log ->
                    val dateStr = SimpleDateFormat("d MMM yyyy HH:mm", Locale("tr", "TR")).format(Date(log.timestamp))
                    CircleCard(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), border = null, elevation = 0.dp) {
                        Row(
                            modifier = Modifier.padding(CircleSpacing.md).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(interactionTypeLabel(log.type, contactTypes), fontWeight = FontWeight.Bold)
                                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (log.note.isNotBlank()) {
                                    Text(log.note, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            IconButton(onClick = { editingLog = log }) {
                                Icon(Icons.Default.Edit, contentDescription = "Temas kaydını düzenle")
                            }
                            IconButton(onClick = { deletingLog = log }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Temas kaydını sil",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        editingLog?.let { log ->
            EditPersonLogSheet(
                log = log,
                contactTypes = contactTypes,
                onDismiss = { editingLog = null },
                onSave = { updated ->
                    viewModel.updateInteractionLog(updated)
                    editingLog = null
                }
            )
        }

        deletingLog?.let { log ->
            CircleDestructiveDialog(
                title = "Temas kaydı silinsin mi?",
                body = "Bu kayıt geçmişten kaldırılır ve son temas tarihi yeniden hesaplanır.",
                confirmLabel = "Sil",
                onDismiss = { deletingLog = null },
                onConfirm = {
                    viewModel.deleteInteractionLog(log.id)
                    deletingLog = null
                }
            )
        }

        if (showDeletePersonDialog) {
            CircleDestructiveDialog(
                title = "${person.name} silinsin mi?",
                body = "Kişi ve bu kişiye ait tüm temas geçmişi kalıcı olarak kaldırılır.",
                confirmLabel = "Sil",
                onDismiss = { showDeletePersonDialog = false },
                onConfirm = {
                    viewModel.deletePerson(person.id)
                    showDeletePersonDialog = false
                    onDeleted()
                }
            )
        }
    }
}
