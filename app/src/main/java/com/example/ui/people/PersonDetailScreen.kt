package com.example.ui.people

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
import com.example.ui.NetworkViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(personId: Int, viewModel: NetworkViewModel, onBack: () -> Unit) {
    val people by viewModel.people.collectAsState()
    val person = people.find { it.id == personId }
    val interactions by viewModel.interactions.collectAsState()
    val personInteractions = interactions.filter { it.personId == personId }.sortedByDescending { it.timestamp }
    val waves by viewModel.waves.collectAsState()

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
            Text("Person not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CRM & Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reminders", fontWeight = FontWeight.Medium)
                                Text(
                                    if (person.reminderEnabled) "Included in catch-up reminders" else "Paused for this person",
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
                            val syncDateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(syncDate))
                            Text(
                                "Last phone-call sync: $syncDateStr",
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
                            label = { Text("Tags (comma separated)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Work, Tech, Colleague") }
                        )
                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { 
                                editNotes = it
                                viewModel.updatePerson(person.copy(notes = it))
                            },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 5
                        )
                    }
                }
            }

            item {
                Text("Interaction History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            }

            if (personInteractions.isEmpty()) {
                item {
                    Text("No interactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(personInteractions) { log ->
                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.type, fontWeight = FontWeight.Bold)
                        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
