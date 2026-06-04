package com.barisdincer.circlekeep.data.presentation

import com.barisdincer.circlekeep.data.ContactReminderCalculator
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.InteractionLog
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.PersonContactRhythm
import com.barisdincer.circlekeep.data.Wave

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

data class ReportsSummary(
    val peopleCount: Int,
    val totalInteractionCount: Int,
    val recentInteractionCount: Int,
    val reachedPeopleCount: Int,
    val rhythmRows: List<ReportMetricRow>,
    val freshnessRows: List<ReportMetricRow>,
    val contactTypeRows: List<ReportMetricRow>,
    val groupRows: List<ReportMetricRow>
) {
    val recentCoverage: Float = ratio(reachedPeopleCount, peopleCount)
    val waitingCount: Int = rhythmRows
        .filter { it.key == "overdue" || it.key == "today" }
        .sumOf { it.count }
}

data class ReportMetricRow(
    val key: String,
    val label: String,
    val count: Int,
    val progress: Float
)

fun buildReportsSummary(
    people: List<Person>,
    waves: List<Wave>,
    rhythms: List<PersonContactRhythm>,
    contactTypes: List<ContactType>,
    interactions: List<InteractionLog>,
    currentTimeMillis: Long = System.currentTimeMillis()
): ReportsSummary {
    val todayStart = dayStartMillis(currentTimeMillis)
    val last30Start = todayStart - 29 * DAY_MILLIS
    val recentInteractions = interactions.filter { it.timestamp >= last30Start }
    val due = ContactReminderCalculator.dueContacts(
        people = people,
        waves = waves,
        rhythms = rhythms,
        currentTimeMillis = currentTimeMillis
    )
    val upcoming = ContactReminderCalculator.nextContacts(
        people = people,
        waves = waves,
        rhythms = rhythms,
        currentTimeMillis = currentTimeMillis,
        limit = 10
    )
    val snoozed = ContactReminderCalculator.snoozedContacts(
        people = people,
        waves = waves,
        rhythms = rhythms,
        currentTimeMillis = currentTimeMillis
    )

    val rhythmRaw = listOf(
        "overdue" to ("Geciken" to due.count { it.daysOverdue > 0 }),
        "today" to ("Bugün" to due.count { it.daysOverdue == 0L }),
        "upcoming" to ("Sıradaki" to upcoming.size),
        "snoozed" to ("Ertelenen" to snoozed.size)
    )
    val rhythmTotal = rhythmRaw.sumOf { it.second.second }.coerceAtLeast(1)

    val freshnessRaw = listOf(
        "fresh_14" to ("Son 14 gün" to people.count { daysSince(it.lastInteractionDate, currentTimeMillis) <= 14 }),
        "fresh_30" to ("15-30 gün" to people.count { daysSince(it.lastInteractionDate, currentTimeMillis) in 15..30 }),
        "fresh_60" to ("31-60 gün" to people.count { daysSince(it.lastInteractionDate, currentTimeMillis) in 31..60 }),
        "fresh_old" to ("60+ gün" to people.count { daysSince(it.lastInteractionDate, currentTimeMillis) > 60 })
    )
    val peopleTotal = people.size.coerceAtLeast(1)

    val typeCounts = interactions.groupingBy { it.type }.eachCount()
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    val typeTotal = typeCounts.sumOf { it.second }.coerceAtLeast(1)

    val groupCounts = buildList {
        waves.forEach { wave ->
            add(wave.name to people.count { it.waveId == wave.id })
        }
        val ungrouped = people.count { it.waveId == null }
        if (ungrouped > 0 || waves.isEmpty()) add("Grup yok" to ungrouped)
    }.sortedByDescending { it.second }.take(6)

    return ReportsSummary(
        peopleCount = people.size,
        totalInteractionCount = interactions.size,
        recentInteractionCount = recentInteractions.size,
        reachedPeopleCount = recentInteractions.map { it.personId }.distinct().size,
        rhythmRows = rhythmRaw.map { (key, labelAndCount) ->
            ReportMetricRow(key, labelAndCount.first, labelAndCount.second, ratio(labelAndCount.second, rhythmTotal))
        },
        freshnessRows = freshnessRaw.map { (key, labelAndCount) ->
            ReportMetricRow(key, labelAndCount.first, labelAndCount.second, ratio(labelAndCount.second, peopleTotal))
        },
        contactTypeRows = typeCounts.map { (type, count) ->
            ReportMetricRow(type, labelFor(type, contactTypes), count, ratio(count, typeTotal))
        },
        groupRows = groupCounts.map { (label, count) ->
            ReportMetricRow(label, label, count, ratio(count, peopleTotal))
        }
    )
}

private fun daysSince(timestamp: Long, now: Long): Long {
    return ((now - timestamp) / DAY_MILLIS).coerceAtLeast(0L)
}

private fun dayStartMillis(timestamp: Long): Long {
    return (timestamp / DAY_MILLIS) * DAY_MILLIS
}

private fun ratio(value: Int, total: Int): Float {
    return if (total <= 0) 0f else value.toFloat() / total.toFloat()
}

private fun labelFor(key: String, contactTypes: List<ContactType>): String {
    return contactTypes.find { it.key == key }?.label ?: when (key) {
        DefaultContactTypes.CALL -> "Arama"
        DefaultContactTypes.MESSAGE -> "Mesaj"
        DefaultContactTypes.MEETING -> "Buluşma"
        else -> key
    }
}
