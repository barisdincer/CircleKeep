package com.barisdincer.circlekeep.ui.people

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.presentation.PeopleListQuery
import com.barisdincer.circlekeep.data.presentation.PeopleListSort
import com.barisdincer.circlekeep.data.presentation.PeopleListView
import com.barisdincer.circlekeep.data.presentation.buildPeopleListItems
import com.barisdincer.circlekeep.data.sortedByTurkish
import com.barisdincer.circlekeep.device.PhonebookReader
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.design.CircleChip
import com.barisdincer.circlekeep.ui.design.CircleEmptyState
import com.barisdincer.circlekeep.ui.design.CircleFilterOption
import com.barisdincer.circlekeep.ui.design.CircleFilterRow
import com.barisdincer.circlekeep.ui.design.CircleMotion
import com.barisdincer.circlekeep.ui.design.CircleRadius
import com.barisdincer.circlekeep.ui.design.CircleScreenScaffold
import com.barisdincer.circlekeep.ui.design.CircleSearchField
import com.barisdincer.circlekeep.ui.design.CircleSpacing

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
        people, waves, contactRhythms, searchTerm, selectedView, selectedSort, selectedTag, selectedWaveId
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

    CircleScreenScaffold(
        title = "Kişiler",
        subtitle = "${people.size} kişi · ritim, etiket ve gruba göre süz",
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        floatingActionButton = {
            com.barisdincer.circlekeep.ui.design.CircleFab(
                onClick = onAddPersonClick,
                icon = Icons.Default.Add,
                contentDescription = "Kişi ekle",
            )
        },
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { CircleChip(selectedSort == PeopleListSort.STATUS, "Öncelik") { selectedSort = PeopleListSort.STATUS } }
                    item { CircleChip(selectedSort == PeopleListSort.NAME, "A-Z") { selectedSort = PeopleListSort.NAME } }
                    item { CircleChip(selectedSort == PeopleListSort.LAST_CONTACT, "Son temas") { selectedSort = PeopleListSort.LAST_CONTACT } }
                }
            }

            if (waves.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { CircleChip(selectedWaveId == null, "Tüm gruplar") { selectedWaveId = null } }
                        items(waves) { wave ->
                            CircleChip(selectedWaveId == wave.id, wave.name) { selectedWaveId = wave.id }
                        }
                    }
                }
            }

            if (uniqueTags.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { CircleChip(selectedTag == null, "Tüm etiketler") { selectedTag = null } }
                        items(uniqueTags) { tag ->
                            CircleChip(selectedTag == tag, tag) { selectedTag = tag }
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
                        icon = Icons.Default.Contacts,
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

