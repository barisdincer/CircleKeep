package com.barisdincer.circlekeep.ui.waves

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.ui.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavesScreen(viewModel: NetworkViewModel) {
    val waves by viewModel.waves.collectAsState()
    val people by viewModel.people.collectAsState()
    val interactions by viewModel.interactions.collectAsState()
    val contactTypes by viewModel.activeContactTypes.collectAsState()
    val managementMessage by viewModel.managementMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var sheetState by remember { mutableStateOf<ManagementSheet?>(null) }

    LaunchedEffect(managementMessage) {
        val message = managementMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearManagementMessage()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Gruplar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge) },
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
                onClick = {
                    sheetState = if (selectedTab == 0) ManagementSheet.EditGroup(null) else ManagementSheet.EditContactType(null)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) "Grup ekle" else "Tür ekle")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "İlişki ritmini iki yerden yönet: kişilerin ait olduğu gruplar ve onlarla temas kurma türlerin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Gruplar") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("İletişim türleri") }
                    )
                }
            }

            if (selectedTab == 0) {
                if (waves.isEmpty()) {
                    item {
                        EmptyManagementState(
                            title = "Henüz grup yok",
                            body = "Yakınlar için 7 gün, arkadaşlar için 21 gün gibi kişisel ritimler ekleyebilirsin."
                        )
                    }
                }
                items(waves, key = { it.id }) { wave ->
                    GroupCard(
                        wave = wave,
                        personCount = people.count { it.waveId == wave.id },
                        onEdit = { sheetState = ManagementSheet.EditGroup(wave) },
                        onDelete = { sheetState = ManagementSheet.DeleteGroup(wave) }
                    )
                }
            } else {
                if (contactTypes.isEmpty()) {
                    item {
                        EmptyManagementState(
                            title = "Aktif iletişim türü yok",
                            body = "Arama, mesaj, buluşma gibi hızlı aksiyon türlerini buradan ekleyebilirsin."
                        )
                    }
                }
                items(contactTypes, key = { it.key }) { type ->
                    ContactTypeCard(
                        type = type,
                        usageCount = people.count { it.preferredContactTypeKey == type.key } + interactions.count { it.type == type.key },
                        onEdit = { sheetState = ManagementSheet.EditContactType(type) },
                        onDelete = { sheetState = ManagementSheet.DeleteContactType(type) }
                    )
                }
            }
        }

        sheetState?.let { sheet ->
            ModalBottomSheet(
                onDismissRequest = { sheetState = null },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                when (sheet) {
                    is ManagementSheet.EditGroup -> GroupSheetContent(
                        wave = sheet.wave,
                        onDismiss = { sheetState = null },
                        onSave = { name, days ->
                            if (sheet.wave == null) {
                                viewModel.addWave(name, days)
                            } else {
                                viewModel.updateWave(sheet.wave.copy(name = name, frequencyDays = days))
                            }
                            sheetState = null
                        }
                    )
                    is ManagementSheet.EditContactType -> ContactTypeSheetContent(
                        type = sheet.type,
                        onDismiss = { sheetState = null },
                        onSave = { label ->
                            if (sheet.type == null) {
                                viewModel.addContactType(label)
                            } else {
                                viewModel.renameContactType(sheet.type.key, label)
                            }
                            sheetState = null
                        }
                    )
                    is ManagementSheet.DeleteGroup -> DeleteSheetContent(
                        title = "${sheet.wave.name} kaldırılsın mı?",
                        body = "Bu işlem sadece grup boşsa tamamlanır. İçinde kişi varsa önce kişileri başka gruba taşıman gerekir.",
                        confirmLabel = "Grubu kaldır",
                        onDismiss = { sheetState = null },
                        onConfirm = {
                            viewModel.deleteWave(sheet.wave.id)
                            sheetState = null
                        }
                    )
                    is ManagementSheet.DeleteContactType -> DeleteSheetContent(
                        title = "${sheet.type.label} kaldırılsın mı?",
                        body = "Bu tür kişi tercihlerinde veya temas geçmişinde kullanılıyorsa işlem engellenir.",
                        confirmLabel = "Türü kaldır",
                        onDismiss = { sheetState = null },
                        onConfirm = {
                            viewModel.deleteContactType(sheet.type.key)
                            sheetState = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    wave: Wave,
    personCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(wave.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${wave.frequencyDays} günde bir · $personCount kişi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Grubu düzenle")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Grubu kaldır")
            }
        }
    }
}

@Composable
private fun ContactTypeCard(
    type: ContactType,
    usageCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(type.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    buildString {
                        append(if (type.isDefault) "Varsayılan tür" else "Özel tür")
                        append(" · ")
                        append("$usageCount kullanım")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Türü düzenle")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Türü kaldır")
            }
        }
    }
}

@Composable
private fun EmptyManagementState(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GroupSheetContent(
    wave: Wave?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var name by remember(wave?.id) { mutableStateOf(wave?.name.orEmpty()) }
    var frequency by remember(wave?.id) { mutableStateOf(wave?.frequencyDays?.toString().orEmpty()) }
    val days = frequency.toIntOrNull()
    val canSave = name.isNotBlank() && days != null && days > 0

    SheetContainer(
        title = if (wave == null) "Grup ekle" else "Grubu düzenle",
        body = "Bu grup, kişilerin varsayılan hatırlatma ritmini belirler."
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Grup adı") },
            placeholder = { Text("Yakınlar") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        OutlinedTextField(
            value = frequency,
            onValueChange = { frequency = it.filter { char -> char.isDigit() } },
            label = { Text("Kaç günde bir?") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        SheetActions(
            confirmLabel = if (wave == null) "Ekle" else "Kaydet",
            confirmEnabled = canSave,
            onDismiss = onDismiss,
            onConfirm = { onSave(name, days ?: 0) }
        )
    }
}

@Composable
private fun ContactTypeSheetContent(
    type: ContactType?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var label by remember(type?.key) { mutableStateOf(type?.label.orEmpty()) }

    SheetContainer(
        title = if (type == null) "İletişim türü ekle" else "Türü düzenle",
        body = "Kişi kartlarındaki hızlı aksiyon etiketini belirler."
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Tür adı") },
            placeholder = { Text("Kahve, yürüyüş, oyun") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        SheetActions(
            confirmLabel = if (type == null) "Ekle" else "Kaydet",
            confirmEnabled = label.isNotBlank(),
            onDismiss = onDismiss,
            onConfirm = { onSave(label) }
        )
    }
}

@Composable
private fun DeleteSheetContent(
    title: String,
    body: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SheetContainer(title = title, body = body) {
        SheetActions(
            confirmLabel = confirmLabel,
            confirmEnabled = true,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun SheetContainer(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SheetActions(
    confirmLabel: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) {
            Text("Vazgeç")
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (confirmLabel.contains("kaldır", ignoreCase = true)) {
            OutlinedButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmLabel)
            }
        } else {
            Button(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmLabel)
            }
        }
    }
}

private sealed interface ManagementSheet {
    data class EditGroup(val wave: Wave?) : ManagementSheet
    data class EditContactType(val type: ContactType?) : ManagementSheet
    data class DeleteGroup(val wave: Wave) : ManagementSheet
    data class DeleteContactType(val type: ContactType) : ManagementSheet
}
