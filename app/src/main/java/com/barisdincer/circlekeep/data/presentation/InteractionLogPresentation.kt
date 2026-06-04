package com.barisdincer.circlekeep.data.presentation

import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.InteractionLog
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.Wave

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

enum class InteractionLogView {
    ALL,
    LAST_30_DAYS,
    CALLS,
    MESSAGES,
    MEETINGS
}

data class InteractionLogQuery(
    val searchTerm: String = "",
    val view: InteractionLogView = InteractionLogView.ALL,
    val contactTypeKey: String? = null,
    val waveId: Int? = null,
    val startMillis: Long? = null,
    val endMillis: Long? = null
)

data class InteractionLogPresentation(
    val totalRecordCount: Int,
    val filteredRecordCount: Int,
    val eventCount: Int,
    val events: List<InteractionLogEvent>
)

data class InteractionLogEvent(
    val logs: List<InteractionLog>,
    val timestamp: Long,
    val type: String,
    val typeLabel: String,
    val note: String,
    val personIds: List<Int>,
    val participantNames: List<String>,
    val waveNames: List<String>
) {
    val ids: List<Int> = logs.map { it.id }
    val participantCount: Int = personIds.size
}

fun buildInteractionLogPresentation(
    logs: List<InteractionLog>,
    people: List<Person>,
    waves: List<Wave>,
    contactTypes: List<ContactType>,
    query: InteractionLogQuery,
    currentTimeMillis: Long = System.currentTimeMillis()
): InteractionLogPresentation {
    val peopleById = people.associateBy { it.id }
    val wavesById = waves.associateBy { it.id }
    val last30Start = dayStartMillis(currentTimeMillis) - 29 * DAY_MILLIS
    val filteredLogs = logs.filter { log ->
        val person = peopleById[log.personId]
        val wave = person?.waveId?.let { wavesById[it] }
        log.matchesView(query.view, last30Start) &&
            query.contactTypeKey?.let { log.type == it } != false &&
            query.waveId?.let { person?.waveId == it } != false &&
            query.startMillis?.let { log.timestamp >= it } != false &&
            query.endMillis?.let { log.timestamp <= it } != false &&
            log.matchesSearch(query.searchTerm, person, wave, labelFor(log.type, contactTypes))
    }

    val events = filteredLogs
        .groupBy { EventGroupKey(dayStartMillis(it.timestamp), it.type, it.note.trim()) }
        .map { (key, groupedLogs) ->
            val sortedLogs = groupedLogs.sortedBy { it.personId }
            val participants = sortedLogs.mapNotNull { peopleById[it.personId] }
            InteractionLogEvent(
                logs = sortedLogs,
                timestamp = key.dayStartMillis,
                type = key.type,
                typeLabel = labelFor(key.type, contactTypes),
                note = key.note,
                personIds = sortedLogs.map { it.personId }.distinct(),
                participantNames = participants.map { it.name }.distinct(),
                waveNames = participants
                    .map { person -> person.waveId?.let { wavesById[it]?.name } ?: "Grup yok" }
                    .distinct()
            )
        }
        .sortedByDescending { it.timestamp }

    return InteractionLogPresentation(
        totalRecordCount = logs.size,
        filteredRecordCount = filteredLogs.size,
        eventCount = events.size,
        events = events
    )
}

private data class EventGroupKey(
    val dayStartMillis: Long,
    val type: String,
    val note: String
)

private fun InteractionLog.matchesView(view: InteractionLogView, last30Start: Long): Boolean {
    return when (view) {
        InteractionLogView.ALL -> true
        InteractionLogView.LAST_30_DAYS -> timestamp >= last30Start
        InteractionLogView.CALLS -> type == DefaultContactTypes.CALL
        InteractionLogView.MESSAGES -> type == DefaultContactTypes.MESSAGE
        InteractionLogView.MEETINGS -> type == DefaultContactTypes.MEETING
    }
}

private fun InteractionLog.matchesSearch(
    searchTerm: String,
    person: Person?,
    wave: Wave?,
    typeLabel: String
): Boolean {
    val query = searchTerm.trim()
    if (query.isEmpty()) return true
    return listOf(
        person?.name.orEmpty(),
        person?.phoneNumber.orEmpty(),
        person?.tags.orEmpty(),
        wave?.name.orEmpty(),
        note,
        type,
        typeLabel
    ).any { it.contains(query, ignoreCase = true) }
}

private fun dayStartMillis(timestamp: Long): Long {
    return (timestamp / DAY_MILLIS) * DAY_MILLIS
}

private fun labelFor(key: String, contactTypes: List<ContactType>): String {
    return contactTypes.find { it.key == key }?.label ?: when (key) {
        DefaultContactTypes.CALL -> "Arama"
        DefaultContactTypes.MESSAGE -> "Mesaj"
        DefaultContactTypes.MEETING -> "Buluşma"
        else -> key
    }
}
