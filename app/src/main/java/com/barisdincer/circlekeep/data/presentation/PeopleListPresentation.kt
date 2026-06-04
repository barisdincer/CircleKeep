package com.barisdincer.circlekeep.data.presentation

import com.barisdincer.circlekeep.data.ContactReminderCalculator
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.PersonContactRhythm
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.data.sortedByTurkish

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

enum class PeopleListView {
    ALL,
    OVERDUE,
    UPCOMING,
    UNTAGGED
}

enum class PeopleListSort {
    NAME,
    LAST_CONTACT,
    STATUS
}

data class PeopleListQuery(
    val searchTerm: String = "",
    val view: PeopleListView = PeopleListView.ALL,
    val tag: String? = null,
    val waveId: Int? = null,
    val sort: PeopleListSort = PeopleListSort.NAME
)

data class PeopleListItem(
    val person: Person,
    val wave: Wave?,
    val daysSinceLastInteraction: Long,
    val daysOverdue: Long?,
    val daysUntilDue: Long?,
    val matchingContactTypeKeys: List<String> = emptyList()
) {
    val isOverdue: Boolean = (daysOverdue ?: 0L) > 0
    val isDueToday: Boolean = daysOverdue == 0L
    val isUpcoming: Boolean = daysUntilDue != null && daysUntilDue in 1..7
    val hasTags: Boolean = person.tags.splitTags().isNotEmpty()
}

fun buildPeopleListItems(
    people: List<Person>,
    waves: List<Wave>,
    rhythms: List<PersonContactRhythm>,
    query: PeopleListQuery,
    currentTimeMillis: Long = System.currentTimeMillis()
): List<PeopleListItem> {
    val dueByPerson = ContactReminderCalculator.dueContacts(
        people = people,
        waves = waves,
        rhythms = rhythms,
        currentTimeMillis = currentTimeMillis
    ).groupBy { it.person.id }
    val upcomingByPerson = ContactReminderCalculator.upcomingContacts(
        people = people,
        waves = waves,
        rhythms = rhythms,
        currentTimeMillis = currentTimeMillis
    ).groupBy { it.person.id }

    val items = people.map { person ->
        val dueContacts = dueByPerson[person.id].orEmpty()
        val upcomingContacts = upcomingByPerson[person.id].orEmpty()
        val daysSince = ((currentTimeMillis - person.lastInteractionDate) / DAY_MILLIS).coerceAtLeast(0L)
        PeopleListItem(
            person = person,
            wave = waves.find { it.id == person.waveId },
            daysSinceLastInteraction = daysSince,
            daysOverdue = dueContacts.maxOfOrNull { it.daysOverdue },
            daysUntilDue = upcomingContacts.minOfOrNull { -it.daysOverdue },
            matchingContactTypeKeys = (dueContacts + upcomingContacts).map { it.contactTypeKey }.distinct()
        )
    }

    return items
        .filter { it.matchesSearch(query.searchTerm) }
        .filter { item -> query.tag?.let { it in item.person.tags.splitTags() } ?: true }
        .filter { item -> query.waveId?.let { item.person.waveId == it } ?: true }
        .filter { item ->
            when (query.view) {
                PeopleListView.ALL -> true
                PeopleListView.OVERDUE -> item.isOverdue || item.isDueToday
                PeopleListView.UPCOMING -> item.isUpcoming
                PeopleListView.UNTAGGED -> !item.hasTags
            }
        }
        .sort(query.sort)
}

fun String.splitTags(): List<String> {
    return split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}

private fun PeopleListItem.matchesSearch(searchTerm: String): Boolean {
    val query = searchTerm.trim()
    if (query.isEmpty()) return true
    return listOf(
        person.name,
        person.phoneNumber,
        person.notes,
        person.memoryNotes,
        person.nextConversationHint,
        person.tags,
        wave?.name.orEmpty()
    ).any { it.contains(query, ignoreCase = true) }
}

private fun List<PeopleListItem>.sort(sort: PeopleListSort): List<PeopleListItem> {
    return when (sort) {
        PeopleListSort.NAME -> sortedByTurkish { it.person.name }
        PeopleListSort.LAST_CONTACT -> sortedWith(
            compareByDescending<PeopleListItem> { it.person.lastInteractionDate }
                .thenBy { it.person.name }
        )
        PeopleListSort.STATUS -> sortedWith(
            compareByDescending<PeopleListItem> { it.daysOverdue ?: Long.MIN_VALUE }
                .thenBy { it.daysUntilDue ?: Long.MAX_VALUE }
                .thenBy { it.person.name }
        )
    }
}
