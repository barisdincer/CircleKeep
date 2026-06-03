package com.barisdincer.circlekeep.ui.components

import com.barisdincer.circlekeep.data.InteractionLog
import java.util.Calendar

data class InteractionEventGroup(
    val logs: List<InteractionLog>,
    val timestamp: Long,
    val type: String,
    val note: String
) {
    val ids: List<Int> = logs.map { it.id }
    val personIds: List<Int> = logs.map { it.personId }.distinct()
    val participantCount: Int = personIds.size
}

fun interactionEventGroups(logs: List<InteractionLog>): List<InteractionEventGroup> {
    return logs
        .groupBy { EventGroupKey(dayStartMillis(it.timestamp), it.type, it.note.trim()) }
        .map { (key, groupedLogs) ->
            InteractionEventGroup(
                logs = groupedLogs.sortedBy { it.personId },
                timestamp = key.dayStartMillis,
                type = key.type,
                note = key.note
            )
        }
        .sortedByDescending { it.timestamp }
}

private data class EventGroupKey(
    val dayStartMillis: Long,
    val type: String,
    val note: String
)

private fun dayStartMillis(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
