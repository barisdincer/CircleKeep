package com.example.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NetworkViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: NetworkViewModel) {
    val interactions by viewModel.interactions.collectAsState()
    val people by viewModel.people.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val context = LocalContext.current
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingBackupJson ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
        }
        pendingBackupJson = null
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
            if (json != null) {
                viewModel.restoreBackupJson(json)
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Reports", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("INTERACTION FREQUENCY (LAST 7 DAYS)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            // Calculate last 7 days interactions
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis
            
            val last7Days = (6 downTo 0).map { offset ->
                val dayStart = todayStart - offset * 24 * 60 * 60 * 1000L
                val dayEnd = dayStart + 24 * 60 * 60 * 1000L - 1
                val count = interactions.count { it.timestamp in dayStart..dayEnd }
                val label = SimpleDateFormat("EE", Locale.getDefault()).format(Date(dayStart))
                Pair(label, count)
            }

            val maxCount = last7Days.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            val primaryColor = MaterialTheme.colorScheme.primary

            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    val width = size.width
                    val height = size.height
                    val barWidth = width / (last7Days.size * 2f)
                    val spacing = width / last7Days.size

                    last7Days.forEachIndexed { index, (label, count) ->
                        val x = index * spacing + spacing / 2 - barWidth / 2
                        val barHeight = (count.toFloat() / maxCount) * (height - 40.dp.toPx())
                        val y = height - 20.dp.toPx() - barHeight

                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight)
                        )

                        // Draw label
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText(label, x + barWidth / 2, height, paint)
                            if (count > 0) {
                                drawText(count.toString(), x + barWidth / 2, y - 8.dp.toPx(), paint)
                            }
                        }
                    }
                }
            }
            
            Text("NETWORK OVERVIEW", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f).height(100.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Contacts", style = MaterialTheme.typography.bodySmall)
                        Text("${people.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Card(modifier = Modifier.weight(1f).height(100.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val active = people.count { it.waveId != null }
                        Text("Active Tracking", style = MaterialTheme.typography.bodySmall)
                        Text("$active", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("BACKUP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Local JSON backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        backupState.message ?: "Export before changing devices or reinstalling the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                viewModel.createBackupJson { json ->
                                    pendingBackupJson = json
                                    createBackupLauncher.launch("network-manager-backup.json")
                                }
                            }
                        ) {
                            Text("Export")
                        }
                        OutlinedButton(
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("application/json", "text/*"))
                            }
                        ) {
                            Text("Import")
                        }
                    }
                }
            }
        }
    }
}
