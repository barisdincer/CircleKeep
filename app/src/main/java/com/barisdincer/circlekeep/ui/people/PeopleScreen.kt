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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.barisdincer.circlekeep.data.presentation.PeopleListItem
import com.barisdincer.circlekeep.data.presentation.PeopleListQuery
import com.barisdincer.circlekeep.data.presentation.PeopleListSort
import com.barisdincer.circlekeep.data.presentation.PeopleListView
import com.barisdincer.circlekeep.data.presentation.buildPeopleListItems
import com.barisdincer.circlekeep.data.sortedByTurkish
import com.barisdincer.circlekeep.device.PhonebookContact
import com.barisdincer.circlekeep.device.PhonebookReader
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.components.DatePickerField
import com.barisdincer.circlekeep.ui.design.CircleEmptyState
import com.barisdincer.circlekeep.ui.design.CircleFilterOption
import com.barisdincer.circlekeep.ui.design.CircleFilterRow
import com.barisdincer.circlekeep.ui.design.CircleSearchField
import com.barisdincer.circlekeep.ui.design.CircleSectionHeader
import com.barisdincer.circlekeep.ui.design.CircleStatusPill
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
    val contactRhythms by viewModel.personContactRhythms.collectAsState()
    val uniqueTags by viewModel.uniqueTags.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    var searchTerm by rememberSaveable { mutableStateOf("") }
    var selectedView by rememberSaveable { mutableStateOf(PeopleListView.ALL) }
    var selectedSort by rememberSaveable { mutableStateOf(PeopleListSort.STATUS) }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedWaveId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showBulkImportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val peopleItems = remember(
        people,
        waves,
        contactRhythms,
        searchTerm,
        selectedView,
        selectedSort,
        selectedTag,
        selectedWaveId
    ) {
        buildPeopleListItems(
            people = people,
            waves = waves,
            rhythms = contactRhythms,
            query = PeopleListQuery(
                searchTerm = searchTerm,
                view = selectedView,
                tag = selectedTag,
                waveId = selectedWaveId,
                sort = selectedSort
            )
        )
    }

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
                CircleSectionHeader(
                    title = "İlişki ağı",
                    count = peopleItems.size,
                    subtitle = "${people.size} kişi içinden arama, ritim ve etiketlere göre süzülüyor."
                )
            }

            item {
                CircleSearchField(
                    value = searchTerm,
                    onValueChange = { searchTerm = it },
                    placeholder = "İsim, telefon, not veya etiket ara"
                )
            }

            item {
                CircleFilterRow(
                    options = listOf(
                        CircleFilterOption(PeopleListView.ALL, "Tümü", people.size),
                        CircleFilterOption(PeopleListView.OVERDUE, "Bekleyen"),
                        CircleFilterOption(PeopleListView.UPCOMING, "Yakında"),
                        CircleFilterOption(PeopleListView.UNTAGGED, "Etiketsiz")
                    ),
                    selected = selectedView,
                    onSelected = { selectedView = it }
                )
            }

            item {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedSort == PeopleListSort.STATUS,
                            onClick = { selectedSort = PeopleListSort.STATUS },
                            label = { Text("Öncelik") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSort == PeopleListSort.NAME,
                            onClick = { selectedSort = PeopleListSort.NAME },
                            label = { Text("A-Z") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSort == PeopleListSort.LAST_CONTACT,
                            onClick = { selectedSort = PeopleListSort.LAST_CONTACT },
                            label = { Text("Son temas") }
                        )
                    }
                }
            }

            if (waves.isNotEmpty()) {
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedWaveId == null,
                                onClick = { selectedWaveId = null },
                                label = { Text("Tüm gruplar") }
                            )
                        }
                        items(waves) { wave ->
                            FilterChip(
                                selected = selectedWaveId == wave.id,
                                onClick = { selectedWaveId = wave.id },
                                label = { Text(wave.name) }
                            )
                        }
                    }
                }
            }

            if (uniqueTags.isNotEmpty()) {
                item {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { selectedTag = null },
                                label = { Text("Tüm etiketler") }
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

            if (peopleItems.isEmpty()) {
                item {
                    CircleEmptyState(
                        title = if (people.isEmpty()) "Henüz takip edilen kişi yok" else "Bu görünümde kişi yok",
                        body = if (people.isEmpty()) {
                            "İlk kişiyi ekleyerek özel ritmini oluşturmaya başlayabilirsin."
                        } else {
                            "Aramayı veya filtreleri temizleyerek daha geniş bir görünüm aç."
                        },
                        actionLabel = if (people.isEmpty()) "Kişi ekle" else null,
                        onAction = if (people.isEmpty()) onAddPersonClick else null
                    )
                }
            }
            items(peopleItems, key = { it.person.id }) { item ->
                PersonListCard(
                    item = item,
                    onClick = { onPersonClick(item.person.id) }
                )
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
