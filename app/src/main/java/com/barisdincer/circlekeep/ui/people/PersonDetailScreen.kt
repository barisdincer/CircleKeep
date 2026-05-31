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
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.ui.NetworkViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(personId: Int, viewModel: NetworkViewModel, onBack: () -> Unit) {
    val people by viewModel.people.collectAsState()
    val person = people.find { it.id == personId }
    val interactions by viewModel.interactions.collectAsState()
    val personInteractions = interactions.filter { it.personId == personId }.sortedByDescending { it.timestamp }

    var editNotes by remember { mutableStateOf(person?.notes ?: "") }
    var editTags by remember { mutableStateOf(person?.tags ?: "") }
    val context = LocalContext.current

    LaunchedEffect(person) {
        if (person != null) {
            editNotes = person.notes
            editTags = person.tags
        }
    }

    if (person == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Kişi bulunamadı")
        }
        return
    }

    Scaffold(
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    IconButton(onClick = {
                         val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phoneNumber}"))
                         context.startActivity(intent)
                    }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {
                         val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${person.phoneNumber}"))
                         context.startActivity(intent)
                    }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.Sms, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {
                         val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                         context.startActivity(intent)
                    }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hatırlatma ve notlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hatırlatmalar", fontWeight = FontWeight.Medium)
                                Text(
                                    if (person.reminderEnabled) "Hal hatır listesinde" else "Bu kişi için duraklatıldı",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = person.reminderEnabled,
                                onCheckedChange = {
                                    viewModel.updatePerson(person.copy(reminderEnabled = it))
                                }
                            )
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
                            onValueChange = {
                                editTags = it
                                viewModel.updatePerson(person.copy(tags = it))
                            },
                            label = { Text("Etiketler") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Aile, okul, iş") }
                        )
                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = {
                                editNotes = it
                                viewModel.updatePerson(person.copy(notes = it))
                            },
                            label = { Text("Notlar") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 5
                        )
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
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(interactionTypeLabel(log.type), fontWeight = FontWeight.Bold)
                        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun interactionTypeLabel(type: String): String {
    return when (type) {
        "CALL" -> "Arama"
        "MEETING" -> "Görüşme"
        else -> type
    }
}
