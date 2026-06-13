package com.barisdincer.circlekeep.ui.people

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.ui.components.DatePickerField
import com.barisdincer.circlekeep.ui.design.CircleCard
import com.barisdincer.circlekeep.ui.design.CircleChip
import com.barisdincer.circlekeep.ui.design.CircleFormSection
import com.barisdincer.circlekeep.ui.design.CirclePrimaryButton
import com.barisdincer.circlekeep.ui.design.CircleRadius
import com.barisdincer.circlekeep.ui.design.CircleScreenScaffold
import com.barisdincer.circlekeep.ui.design.CircleSpacing
import com.barisdincer.circlekeep.ui.design.CircleTonalButton

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

    CircleScreenScaffold(title = "Kişi ekle", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = CircleSpacing.md),
            contentPadding = PaddingValues(top = CircleSpacing.xs, bottom = CircleSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
        ) {
            item {
                Text(
                    "Kişinin ritmini ve ilk temasını kaydedip net bir başlangıç yapabilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                CircleTonalButton(
                    text = "Rehberden seç",
                    icon = Icons.Default.Contacts,
                    onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> contactPicker.launch(null)
                            else -> requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                CircleCard {
                    Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("İsim") },
                            shape = RoundedCornerShape(CircleRadius.control),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Telefon") },
                            shape = RoundedCornerShape(CircleRadius.control),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                CircleFormSection(title = "Ritim", subtitle = "Bu kişiye hangi sıklıkta ulaşmak istiyorsun?") {
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
                            shape = RoundedCornerShape(CircleRadius.control),
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
                        shape = RoundedCornerShape(CircleRadius.control),
                        singleLine = true
                    )
                }
            }

            item {
                CircleFormSection(title = "İletişim", subtitle = "Varsayılan temas türü ve başlangıç noktası.") {
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
                            shape = RoundedCornerShape(CircleRadius.control),
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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircleChip(hasInitialContact, "Son temas var") { hasInitialContact = true }
                        CircleChip(!hasInitialContact, "Bugünden takip et") { hasInitialContact = false }
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
                            shape = RoundedCornerShape(CircleRadius.control)
                        )
                    }
                }
            }

            item {
                CirclePrimaryButton(
                    text = "Kişiyi kaydet",
                    enabled = name.isNotBlank(),
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
                    modifier = Modifier.fillMaxWidth(),
                )
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
