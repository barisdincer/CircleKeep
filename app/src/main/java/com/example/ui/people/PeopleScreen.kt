package com.example.ui.people

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.Person
import com.example.data.Wave
import com.example.device.PhonebookContact
import com.example.device.PhonebookReader
import com.example.ui.NetworkViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(viewModel: NetworkViewModel, onPersonClick: (Int) -> Unit) {
    val people by viewModel.people.collectAsState()
    val waves by viewModel.waves.collectAsState()
    val uniqueTags by viewModel.uniqueTags.collectAsState()
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBulkImportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredPeople = if (selectedTag != null) {
        people.filter { it.tags.contains(selectedTag!!, ignoreCase = true) }
    } else {
        people
    }

    val bulkImportPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showBulkImportDialog = true
        }
    }

    Scaffold(
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
                onClick = { showAddDialog = true },
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
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (uniqueTags.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { selectedTag = null },
                                label = { Text("All") }
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
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = person.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(person.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(person.phoneNumber.ifEmpty { "Telefon yok" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(person.lastInteractionDate))
                            Text("Son temas: $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (wave != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = wave.name,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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

        if (showAddDialog) {
            AddPersonDialog(
                waves = waves,
                onDismiss = { showAddDialog = false },
                onAdd = { name, phone, waveId, contactLookupKey ->
                    viewModel.addPerson(name, phone, waveId, contactLookupKey)
                    showAddDialog = false
                }
            )
        }

        if (showBulkImportDialog) {
            val contacts = remember(people) {
                PhonebookReader.readContacts(context, people)
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
                    val selectedWaveName = waves.find { it.id == selectedWaveId }?.name ?: "Hatırlatma döngüsü yok"
                    OutlinedTextField(
                        value = selectedWaveName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hatırlatma döngüsü") },
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
                            text = { Text("Hatırlatma döngüsü yok") },
                            onClick = {
                                selectedWaveId = null
                                expanded = false
                            }
                        )
                        waves.forEach { wave ->
                            DropdownMenuItem(
                                text = { Text("${wave.name} (${wave.frequencyDays}d)") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonDialog(waves: List<Wave>, onDismiss: () -> Unit, onAdd: (String, String, Int?, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var contactLookupKey by remember { mutableStateOf<String?>(null) }
    var selectedWaveId by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kişi ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                                contactPicker.launch(null)
                            }
                            else -> {
                                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rehberden seç", fontWeight = FontWeight.Bold)
                }
                
                HorizontalDivider()

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İsim") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Reminder cycle selection dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val selectedWaveName = waves.find { it.id == selectedWaveId }?.name ?: "Döngü seç"
                    OutlinedTextField(
                        value = selectedWaveName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Döngü") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Döngü yok") },
                            onClick = {
                                selectedWaveId = null
                                expanded = false
                            }
                        )
                        waves.forEach { wave ->
                            DropdownMenuItem(
                                text = { Text(wave.name) },
                                onClick = {
                                    selectedWaveId = wave.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, phone, selectedWaveId, contactLookupKey)
                    }
                }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
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
