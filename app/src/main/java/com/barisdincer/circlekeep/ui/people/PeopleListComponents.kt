package com.barisdincer.circlekeep.ui.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barisdincer.circlekeep.data.presentation.PeopleListItem
import com.barisdincer.circlekeep.ui.design.CircleAvatar
import com.barisdincer.circlekeep.ui.design.CircleCard
import com.barisdincer.circlekeep.ui.design.CircleSpacing
import com.barisdincer.circlekeep.ui.design.CircleStatusPill
import com.barisdincer.circlekeep.ui.design.RhythmState
import com.barisdincer.circlekeep.ui.design.rhythmContainerColor
import com.barisdincer.circlekeep.ui.design.rhythmContentColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PersonListCard(
    item: PeopleListItem,
    onClick: () -> Unit
) {
    val person = item.person
    val state = item.rhythmState()
    CircleCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CircleSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircleAvatar(name = person.name, size = 48.dp)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                    CircleStatusPill(
                        label = item.statusText(),
                        containerColor = rhythmContainerColor(state),
                        contentColor = rhythmContentColor(state)
                    )
                }
                Text(
                    person.phoneNumber.ifEmpty { "Telefon yok" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val dateStr = SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(person.lastInteractionDate))
                Text(
                    "Son temas: $dateStr · ${item.daysSinceLastInteraction} gün önce",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    item.wave?.let { wave ->
                        CircleStatusPill(
                            label = wave.name,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    if (person.tags.isNotBlank()) {
                        Text(
                            person.tags,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun PeopleListItem.rhythmState(): RhythmState = when {
    isOverdue -> RhythmState.OVERDUE
    isDueToday -> RhythmState.TODAY
    isUpcoming -> RhythmState.UPCOMING
    else -> RhythmState.ON_TRACK
}

private fun PeopleListItem.statusText(): String {
    return when {
        isOverdue -> "${daysOverdue ?: 0} gün gecikti"
        isDueToday -> "Bugün"
        isUpcoming -> "${daysUntilDue ?: 0} gün kaldı"
        else -> "Takipte"
    }
}
