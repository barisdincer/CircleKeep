package com.barisdincer.circlekeep.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.presentation.ReportMetricRow
import com.barisdincer.circlekeep.ui.NetworkViewModel
import com.barisdincer.circlekeep.ui.design.CircleCard
import com.barisdincer.circlekeep.ui.design.CircleHeroCard
import com.barisdincer.circlekeep.ui.design.CircleProgressBar
import com.barisdincer.circlekeep.ui.design.CircleScreenScaffold
import com.barisdincer.circlekeep.ui.design.CircleSegmentedBar
import com.barisdincer.circlekeep.ui.design.CircleSpacing
import com.barisdincer.circlekeep.ui.design.CircleStatChip

@Composable
fun ReportsScreen(viewModel: NetworkViewModel) {
    val summary by viewModel.reportsSummary.collectAsState()

    CircleScreenScaffold(
        title = "Özet",
        subtitle = "İlişki ritminin genel sağlığı",
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CircleSpacing.md),
            verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)
        ) {
            OverviewHeroCard(
                peopleCount = summary.peopleCount,
                totalInteractions = summary.totalInteractionCount,
                recentInteractions = summary.recentInteractionCount,
                reachedPeople = summary.reachedPeopleCount
            )

            SummaryDistributionCard(
                title = "Ritim durumu",
                body = "${summary.waitingCount} bekleyen ritim var. Sıradakiler ve ertelemeler toplam görünüm içinde izlenir.",
                rows = summary.rhythmRows
            )

            SummaryDistributionCard(
                title = "Son temas tazeliği",
                body = "Kişilerin ne kadar zamandır temas beklediğini daha net gösterir.",
                rows = summary.freshnessRows
            )

            SummaryDistributionCard(
                title = "Temas türleri",
                body = "Arama, mesaj ve buluşmaların toplam geçmiş içindeki ağırlığı.",
                rows = summary.contactTypeRows,
                emptyText = "Henüz temas kaydı yok."
            )

            SummaryDistributionCard(
                title = "Grup kapsamı",
                body = "Kişilerin hangi gruplarda yoğunlaştığını gösterir.",
                rows = summary.groupRows
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = CircleSpacing.lg))
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
    CircleHeroCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("İlişki ritmi", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
            Text(
                "Son 30 günde $reachedPeople kişiye ulaşıldı; toplam $recentInteractions temas kaydı var.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("30 gün kapsamı", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                Text("$reachedPeople / $peopleCount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            }
            CircleProgressBar(
                progress = coverage,
                color = MaterialTheme.colorScheme.onPrimary,
                track = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
                height = 10.dp,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleStatChip(label = "Kişi", value = peopleCount, modifier = Modifier.weight(1f))
            CircleStatChip(label = "30 gün", value = recentInteractions, modifier = Modifier.weight(1f))
            CircleStatChip(label = "Toplam", value = totalInteractions, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryDistributionCard(
    title: String,
    body: String,
    rows: List<ReportMetricRow>,
    emptyText: String = "Henüz veri yok."
) {
    CircleCard {
        Column(modifier = Modifier.padding(CircleSpacing.md), verticalArrangement = Arrangement.spacedBy(CircleSpacing.sm)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (rows.isEmpty()) {
                Text(
                    emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CircleSegmentedBar(
                    segments = rows.mapIndexed { index, row -> row.progress to distributionColor(index) }
                )
                rows.forEachIndexed { index, row ->
                    DistributionRow(
                        label = row.label,
                        count = row.count,
                        progress = row.progress,
                        color = distributionColor(index)
                    )
                }
            }
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
        CircleProgressBar(progress = progress, color = color)
    }
}

@Composable
private fun distributionColor(index: Int): Color {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline
    )
    return colors[index % colors.size]
}
