package com.barisdincer.circlekeep.ui.people

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.data.sortedByTurkish
import com.barisdincer.circlekeep.device.PhonebookContact
import com.barisdincer.circlekeep.device.PhonebookReader
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.components.DatePickerField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    viewModel: NetworkViewModel,
    onPersonClick: (Int) -> Unit,
    onAddPersonClick: () -> Unit
) {
    val people by viewModel.people.collectAsState()
    val waves by viewModel.waves.collectAsState()
    val uniqueTags by viewModel.uniqueTags.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showBulkImportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredPeople = if (selectedTag != null) {
        people.filter { it.tags.contains(selectedTag!!, ignoreCase = true) }
    } else {
        people
    }.sortedByTurkish { it.name }

    val bulkImportPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showBulkImportDialog = true
        }
    }

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
                    title = { Text("Kişiler", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        IconButton(
                            onClick = {
                                when (PackageManager.PERMISSION_GRANTED) {
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                                        showBulkImportDialog = true
                                    }
                                    else -> {
                                        bulkImportPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = "Rehberi içe aktar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPersonClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Kişi ekle")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                if (uniqueTags.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { selectedTag = null },
                                label = { Text("Tümü") }
                            )
                        }
                        items(uniqueTags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = tag },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
            }

            if (filteredPeople.isEmpty()) {
                item {
                    Text("Henüz takip edilen kişi yok.", style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(filteredPeople) { person ->
                val wave = waves.find { it.id == person.waveId }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onPersonClick(person.id) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = person.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(person.phoneNumber.ifEmpty { "Telefon yok" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(8.dp))
                            val dateStr = SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(person.lastInteractionDate))
                            Text("Son temas: $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (wave != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = wave.name,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showBulkImportDialog) {
            val contacts = remember(people) {
                PhonebookReader.readContacts(context, people).sortedByTurkish { it.name }
            }
            BulkImportContactsDialog(
                contacts = contacts,
                waves = waves,
                onDismiss = { showBulkImportDialog = false },
                onImport = { selectedContacts, waveId ->
                    viewModel.addPeople(
                        selectedContacts.map { contact ->
                            Person(
                                name = contact.name,
                                phoneNumber = contact.phoneNumber,
                                normalizedPhoneNumber = contact.normalizedPhoneNumber,
                                contactLookupKey = contact.lookupKey,
                                waveId = waveId
                            )
                        }
                    )
                    showBulkImportDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkImportContactsDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rehberden içe aktar", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${contacts.size} yeni kişi, ${selectedContacts.size} seçili",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                            .fillMaxWidth()
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            selectedKeys = contacts.map { it.selectionKey }.toSet()
                        }
                    ) {
                        Text("Tümünü seç")
                    }
                    TextButton(onClick = { selectedKeys = emptySet() }) {
                        Text("Temizle")
                    }
                }

                if (contacts.isEmpty()) {
                    Text(
                        "Rehberde içe aktarılacak yeni kişi bulunamadı.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                Spacer(modifier = Modifier.width(8.dp))
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
            }
        },
        confirmButton = {
            Button(
                enabled = selectedContacts.isNotEmpty(),
                onClick = { onImport(selectedContacts, selectedWaveId) }
            ) {
                Text("İçe aktar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPersonScreen(
    waves: List<Wave>,
    contactTypes: List<ContactType>,
    initialWaveId: Int? = null,
    onBack: () -> Unit,
    onAdd: (String, String, Int?, String?, String?, Long?, String, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var contactLookupKey by remember { mutableStateOf<String?>(null) }
    var selectedWaveId by remember(initialWaveId) { mutableStateOf(initialWaveId) }
    val typeOptions = contactTypes.ifEmpty { DefaultContactTypes.all }
    var selectedTypeKey by remember(typeOptions) { mutableStateOf(typeOptions.first().key) }
    var customFrequency by remember { mutableStateOf("") }
    var hasInitialContact by remember { mutableStateOf(true) }
    var initialContactDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var initialNote by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri: Uri? ->
        if (uri != null) {
            val details = getContactDetails(context, uri)
            name = details.name
            phone = details.phoneNumber
            contactLookupKey = details.lookupKey
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            contactPicker.launch(null)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Kişi ekle", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge) },
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
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Klavye kapansa bile bu sayfa açık kalır. Kişinin ritmini ve ilk temasını kaydedip net bir başlangıç yapabilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Button(
                    onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> contactPicker.launch(null)
                            else -> requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rehberden seç", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("İsim") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Telefon") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Ritim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            val selectedWaveName = waves.find { it.id == selectedWaveId }?.name ?: "Grup yok"
                            OutlinedTextField(
                                value = selectedWaveName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Grup") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Grup yok") },
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
                        OutlinedTextField(
                            value = customFrequency,
                            onValueChange = { customFrequency = it.filter { char -> char.isDigit() } },
                            label = { Text("Kişiye özel ritim") },
                            placeholder = { Text("Örn. 90; boşsa grup ritmi") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("İletişim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = !typeExpanded }
                        ) {
                            val selectedType = typeOptions.find { it.key == selectedTypeKey } ?: typeOptions.first()
                            OutlinedTextField(
                                value = selectedType.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Varsayılan iletişim türü") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = hasInitialContact,
                                onClick = { hasInitialContact = true },
                                label = { Text("Son temas var") }
                            )
                            FilterChip(
                                selected = !hasInitialContact,
                                onClick = { hasInitialContact = false },
                                label = { Text("Bugünden takip et") }
                            )
                        }
                        if (hasInitialContact) {
                            DatePickerField(
                                label = "Son temas tarihi",
                                selectedMillis = initialContactDate,
                                onDateSelected = { initialContactDate = it }
                            )
                            OutlinedTextField(
                                value = initialNote,
                                onValueChange = { initialNote = it },
                                label = { Text("İlk temas notu") },
                                placeholder = { Text("İsteğe bağlı") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAdd(
                                name,
                                phone,
                                selectedWaveId,
                                contactLookupKey,
                                selectedTypeKey,
                                if (hasInitialContact) initialContactDate else null,
                                initialNote,
                                customFrequency.toIntOrNull()?.takeIf { it > 0 }
                            )
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Kişiyi kaydet")
                }
            }
        }
    }
}

data class ContactDetails(
    val name: String,
    val phoneNumber: String,
    val lookupKey: String?
)

fun getContactDetails(context: Context, contactUri: Uri): ContactDetails {
    var name = ""
    var phone = ""
    var lookupKey: String? = null
    val cursor = context.contentResolver.query(contactUri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
            val hasPhoneIdx = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
            val lookupKeyIdx = it.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)

            if (nameIdx >= 0) name = it.getString(nameIdx) ?: ""
            if (lookupKeyIdx >= 0) lookupKey = it.getString(lookupKeyIdx)
            val id = if (idIdx >= 0) it.getString(idIdx) else null
            val hasPhone = if (hasPhoneIdx >= 0) it.getInt(hasPhoneIdx) > 0 else false

            if (hasPhone && id != null) {
                val pCursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )
                pCursor?.use { pc ->
                    if (pc.moveToFirst()) {
                        val pIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (pIdx >= 0) phone = pc.getString(pIdx) ?: ""
                    }
                }
            }
        }
    }
    return ContactDetails(name = name, phoneNumber = phone, lookupKey = lookupKey)
}
