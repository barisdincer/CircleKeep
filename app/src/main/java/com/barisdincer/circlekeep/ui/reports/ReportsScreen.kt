package com.barisdincer.circlekeep.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barisdincer.circlekeep.ui.NetworkViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: NetworkViewModel) {
    val interactions by viewModel.interactions.collectAsState()
    val people by viewModel.people.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Özet", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge) },
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
            Text("Son 7 gün temasları", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                val label = SimpleDateFormat("EE", Locale("tr", "TR")).format(Date(dayStart))
                Pair(label, count)
            }

            val maxCount = last7Days.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            val primaryColor = MaterialTheme.colorScheme.primary

            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
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

            Text("Hal hatır özeti", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Kişi", style = MaterialTheme.typography.bodySmall)
                        Text("${people.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).height(100.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val active = people.count { it.waveId != null }
                        Text("Grupta", style = MaterialTheme.typography.bodySmall)
                        Text("$active", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
