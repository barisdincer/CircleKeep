package com.barisdincer.circlekeep.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.InteractionLog
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.ReminderDashboardUiState
import java.util.Calendar

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: NetworkViewModel) {
    val interactions by viewModel.allInteractions.collectAsState()
    val people by viewModel.people.collectAsState()
    val waves by viewModel.waves.collectAsState()
    val contactTypes by viewModel.contactTypes.collectAsState()
    val dashboard by viewModel.dashboardReminders.collectAsState()

    val todayStart = todayStartMillis()
    val last30Start = todayStart - 29 * DAY_MILLIS
    val recentInteractions = interactions.count { it.timestamp >= last30Start }
    val reachedPeople = interactions
        .filter { it.timestamp >= last30Start }
        .map { it.personId }
        .distinct()
        .size

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
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewHeroCard(
                peopleCount = people.size,
                totalInteractions = interactions.size,
                recentInteractions = recentInteractions,
                reachedPeople = reachedPeople
            )

            RhythmHealthCard(dashboard = dashboard)

            ContactFreshnessCard(people = people)

            ContactTypeDistributionCard(
                interactions = interactions,
                contactTypes = contactTypes
            )

            GroupCoverageCard(
                people = people,
                waves = waves
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OverviewHeroCard(
    peopleCount: Int,
    totalInteractions: Int,
    recentInteractions: Int,
    reachedPeople: Int
) {
    val coverage = if (peopleCount == 0) 0f else reachedPeople.toFloat() / peopleCount
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("İlişki ritmi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Son 30 günde $reachedPeople kişiye ulaşıldı; toplam $recentInteractions temas kaydı var.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
            }
            ProgressStrip(
                label = "30 gün kapsamı",
                value = reachedPeople,
                total = peopleCount,
                progress = coverage,
                color = MaterialTheme.colorScheme.primary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Kişi", "$peopleCount", Modifier.weight(1f))
                MetricCard("30 gün", "$recentInteractions", Modifier.weight(1f))
                MetricCard("Toplam", "$totalInteractions", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RhythmHealthCard(dashboard: ReminderDashboardUiState) {
    val dueCount = dashboard.today.size + dashboard.overdue.size
    val rows = listOf(
        StatusMetric("Geciken", dashboard.overdue.size, MaterialTheme.colorScheme.error),
        StatusMetric("Bugün", dashboard.today.size, MaterialTheme.colorScheme.primary),
        StatusMetric("Sıradaki", dashboard.upcoming.size, MaterialTheme.colorScheme.tertiary),
        StatusMetric("Ertelenen", dashboard.snoozed.size, MaterialTheme.colorScheme.outline)
    )
    val total = rows.sumOf { it.count }.coerceAtLeast(1)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Ritim durumu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$dueCount bekleyen ritim var. Sıradakiler ve ertelemeler toplam görünüm içinde izlenir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SegmentedBar(
                segments = rows.map { Segment(it.count.toFloat() / total, it.color) }
            )
            rows.forEach { row ->
                DistributionRow(
                    label = row.label,
                    count = row.count,
                    progress = row.count.toFloat() / total,
                    color = row.color
                )
            }
        }
    }
}

@Composable
private fun ContactFreshnessCard(people: List<Person>) {
    val now = System.currentTimeMillis()
    val rows = listOf(
        FreshnessBucket("Son 14 gün", people.count { daysSince(it.lastInteractionDate, now) <= 14 }, MaterialTheme.colorScheme.primary),
        FreshnessBucket("15-30 gün", people.count { daysSince(it.lastInteractionDate, now) in 15..30 }, MaterialTheme.colorScheme.tertiary),
        FreshnessBucket("31-60 gün", people.count { daysSince(it.lastInteractionDate, now) in 31..60 }, MaterialTheme.colorScheme.secondary),
        FreshnessBucket("60+ gün", people.count { daysSince(it.lastInteractionDate, now) > 60 }, MaterialTheme.colorScheme.error)
    )
    val total = people.size.coerceAtLeast(1)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Son temas tazeliği", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Kişilerin ne kadar zamandır temas beklediğini daha net gösterir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SegmentedBar(
                segments = rows.map { Segment(it.count.toFloat() / total, it.color) }
            )
            rows.forEach { row ->
                DistributionRow(
                    label = row.label,
                    count = row.count,
                    progress = row.count.toFloat() / total,
                    color = row.color
                )
            }
        }
    }
}

@Composable
private fun ContactTypeDistributionCard(
    interactions: List<InteractionLog>,
    contactTypes: List<ContactType>
) {
    val counts = interactions.groupingBy { it.type }.eachCount().toList().sortedByDescending { it.second }
    val total = counts.sumOf { it.second }.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Temas türleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Arama, mesaj ve buluşmaların toplam geçmiş içindeki ağırlığı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (counts.isEmpty()) {
                Text(
                    "Henüz temas kaydı yok.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val visibleCounts = counts.take(5)
                SegmentedBar(
                    segments = visibleCounts.mapIndexed { index, (_, count) ->
                        Segment(count.toFloat() / total, distributionColor(index))
                    }
                )
                visibleCounts.forEachIndexed { index, (type, count) ->
                    DistributionRow(
                        label = interactionTypeLabel(type, contactTypes),
                        count = count,
                        progress = count.toFloat() / total,
                        color = distributionColor(index)
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCoverageCard(people: List<Person>, waves: List<Wave>) {
    val rows = buildList {
        waves.forEach { wave ->
            add(wave.name to people.count { it.waveId == wave.id })
        }
        val ungrouped = people.count { it.waveId == null }
        if (ungrouped > 0 || waves.isEmpty()) {
            add("Grup yok" to ungrouped)
        }
    }.sortedByDescending { it.second }
    val total = people.size.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Grup kapsamı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Kişilerin hangi gruplarda yoğunlaştığını gösterir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val visibleRows = rows.take(6)
            SegmentedBar(
                segments = visibleRows.mapIndexed { index, (_, count) ->
                    Segment(count.toFloat() / total, distributionColor(index))
                }
            )
            visibleRows.forEachIndexed { index, (label, count) ->
                DistributionRow(
                    label = label,
                    count = count,
                    progress = count.toFloat() / total,
                    color = distributionColor(index)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProgressStrip(
    label: String,
    value: Int,
    total: Int,
    progress: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
                "$value / $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun SegmentedBar(segments: List<Segment>) {
    val visibleSegments = segments.filter { it.weight > 0f }
    if (visibleSegments.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        visibleSegments.forEach { segment ->
            Box(
                modifier = Modifier
                    .weight(segment.weight.coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(segment.color)
            )
        }
    }
}

@Composable
private fun DistributionRow(label: String, count: Int, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "$count · ${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun distributionColor(index: Int): Color {
    val colors = reportColors()
    return colors[index % colors.size]
}

@Composable
private fun reportColors(): List<Color> {
    return listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline
    )
}

private fun daysSince(timestamp: Long, now: Long): Long {
    return ((now - timestamp) / DAY_MILLIS).coerceAtLeast(0L)
}

private fun todayStartMillis(): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = System.currentTimeMillis()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun interactionTypeLabel(type: String, contactTypes: List<ContactType>): String {
    return contactTypes.find { it.key == type }?.label ?: when (type) {
        DefaultContactTypes.CALL -> "Arama"
        DefaultContactTypes.MESSAGE -> "Mesaj"
        DefaultContactTypes.MEETING -> "Buluşma"
        else -> type
    }
}

private data class Segment(
    val weight: Float,
    val color: Color
)

private data class StatusMetric(
    val label: String,
    val count: Int,
    val color: Color
)

private data class FreshnessBucket(
    val label: String,
    val count: Int,
    val color: Color
)
