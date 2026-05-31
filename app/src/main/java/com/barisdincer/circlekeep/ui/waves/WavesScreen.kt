package com.barisdincer.circlekeep.ui.waves

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.ui.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavesScreen(viewModel: NetworkViewModel) {
    val waves by viewModel.waves.collectAsState()
    val contactTypes by viewModel.contactTypes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var editingType by remember { mutableStateOf<ContactType?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Döngüler", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge) },
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
                Icon(Icons.Default.Add, contentDescription = "Döngü ekle")
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
            if (waves.isEmpty()) {
                item {
                    Text("Henüz döngü yok. Yakınlar için 7 gün, arkadaşlar için 21 gün gibi küçük ritimler ekleyebilirsin.", style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(waves) { wave ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(wave.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${wave.frequencyDays} günde bir", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("İletişim türleri", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Kişilerin hızlı aksiyonlarında kullanılacak türleri düzenle.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { showAddTypeDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Tür ekle")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tür")
                    }
                }
            }

            items(contactTypes) { type ->
                ContactTypeCard(
                    type = type,
                    onEdit = { editingType = type },
                    onActiveChange = { isActive -> viewModel.setContactTypeActive(type.key, isActive) }
                )
            }
        }

        if (showAddDialog) {
            AddWaveDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, freq ->
                    viewModel.addWave(name, freq)
                    showAddDialog = false
                }
            )
        }

        if (showAddTypeDialog) {
            AddContactTypeDialog(
                onDismiss = { showAddTypeDialog = false },
                onAdd = { label ->
                    viewModel.addContactType(label)
                    showAddTypeDialog = false
                }
            )
        }

        editingType?.let { type ->
            RenameContactTypeDialog(
                type = type,
                onDismiss = { editingType = null },
                onRename = { label ->
                    viewModel.renameContactType(type.key, label)
                    editingType = null
                }
            )
        }
    }
}

@Composable
fun ContactTypeCard(type: ContactType, onEdit: () -> Unit, onActiveChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(type.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (type.isDefault) "Varsayılan tür" else "Özel tür",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Türü yeniden adlandır")
            }
            Switch(checked = type.isActive, onCheckedChange = onActiveChange)
        }
    }
}

@Composable
fun AddWaveDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Döngü ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Döngü adı") },
                    placeholder = { Text("Yakınlar") },
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Kaç günde bir?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = frequency.toIntOrNull()
                    if (name.isNotBlank() && days != null && days > 0) {
                        onAdd(name, days)
                    }
                }
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

@Composable
fun AddContactTypeDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("İletişim türü ekle", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Tür adı") },
                placeholder = { Text("Kahve, yürüyüş, oyun") },
                shape = RoundedCornerShape(8.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank()) onAdd(label)
                }
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

@Composable
fun RenameContactTypeDialog(type: ContactType, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var label by remember(type.key) { mutableStateOf(type.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Türü düzenle", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Tür adı") },
                shape = RoundedCornerShape(8.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank()) onRename(label)
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
