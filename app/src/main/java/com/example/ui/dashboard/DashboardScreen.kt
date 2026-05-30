package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NetworkViewModel
import com.example.ui.PersonWithWave
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: NetworkViewModel) {
    val upcomingContacts by viewModel.upcomingContacts.collectAsState()
    val people by viewModel.people.collectAsState()
    val waves by viewModel.waves.collectAsState()
    val interactions by viewModel.interactions.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Hal Hatır", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                            val currentDate = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
                            Text(currentDate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("HH", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard("Takipte", "${people.size} kişi", Icons.Default.Group, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                    StatCard("Bugün", "${upcomingContacts.size} kişi", Icons.Default.Waves, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f))
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Temas ritmi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Toplam temas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                Text("${interactions.size}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
                                val recentCount = interactions.count { it.timestamp > oneWeekAgo }
                                Text("Son 7 gün", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                Text("$recentCount", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                syncState.message ?: "Arama kayıtları uygulama açıldığında kontrol edilir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                enabled = !syncState.isSyncing,
                                onClick = { viewModel.syncCallLog(context) }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Aramaları eşle")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (syncState.isSyncing) "Kontrol" else "Eşle")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "BUGÜN HATIRLANACAKLAR",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 0.dp)
                )
            }
            
            if (upcomingContacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Bugün bekleyen kimse yok", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Yakın çevrenin ritmi şimdilik yolunda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(upcomingContacts) { contact ->
                ContactReminderCard(
                    contact = contact,
                    onLogInteraction = { type ->
                        viewModel.logInteraction(contact.person.id, type)
                    },
                    onSnooze = {
                        viewModel.snoozePersonForDays(contact.person.id, 1)
                    }
                )
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, containerColor: androidx.compose.ui.graphics.Color, contentColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(130.dp), 
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = contentColor)
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 2.dp), color = contentColor.copy(alpha = 0.7f))
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = contentColor)
            }
        }
    }
}

@Composable
fun ContactReminderCard(
    contact: PersonWithWave,
    onLogInteraction: (String) -> Unit,
    onSnooze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(contact.person.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Döngü: ${contact.wave?.name ?: "Yok"} (${contact.wave?.frequencyDays ?: 0} gün)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    val status = if (contact.daysOverdue > 0) {
                        "${contact.daysOverdue} gün geçti"
                    } else {
                        "Bugün"
                    }
                    Text(status, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(contact.person.lastInteractionDate))
            Text(
                "Son temas: $dateStr (${contact.daysSinceLastInteraction} gün önce)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { onLogInteraction("CALL") }, 
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Aradım")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aradım")
                }
                FilledTonalButton(
                    onClick = { onLogInteraction("MEETING") }, 
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Handshake, contentDescription = "Görüştüm")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Görüştüm")
                }
            }
            TextButton(
                onClick = onSnooze,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Yarın hatırlat")
            }
        }
    }
}
