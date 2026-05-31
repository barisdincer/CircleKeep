package com.barisdincer.circlekeep.data

data class GroupMemberRhythm(
    val effectiveFrequencyDays: Int,
    val daysSinceLastInteraction: Long,
    val daysUntilDue: Long
) {
    val isOverdue: Boolean = daysUntilDue < 0
    val isDueToday: Boolean = daysUntilDue == 0L
}

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

fun groupMemberRhythm(
    person: Person,
    wave: Wave,
    nowMillis: Long = System.currentTimeMillis()
): GroupMemberRhythm {
    val frequencyDays = person.customFrequencyDays?.takeIf { it > 0 } ?: wave.frequencyDays
    val daysSince = ((nowMillis - person.lastInteractionDate) / DAY_MILLIS).coerceAtLeast(0L)
    return GroupMemberRhythm(
        effectiveFrequencyDays = frequencyDays,
        daysSinceLastInteraction = daysSince,
        daysUntilDue = frequencyDays - daysSince
    )
}

fun GroupMemberRhythm.statusLabel(): String {
    return when {
        isOverdue -> "${-daysUntilDue} gün gecikti"
        isDueToday -> "Bugün"
        else -> "$daysUntilDue gün kaldı"
    }
}
