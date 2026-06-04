package com.barisdincer.circlekeep.data.presentation

import com.barisdincer.circlekeep.data.ContactReminderCalculator
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.InteractionLog
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.PersonContactRhythm
import com.barisdincer.circlekeep.data.Wave

data class DashboardPresentation(
    val overdueCount: Int = 0,
    val todayCount: Int = 0,
    val upcomingCount: Int = 0,
    val snoozedCount: Int = 0,
    val recentInteractionCount: Int = 0,
    val focusContacts: List<DashboardContactItem> = emptyList(),
    val nextContact: DashboardContactItem? = null
) {
    val waitingCount: Int = overdueCount + todayCount
    val isCalm: Boolean = waitingCount == 0
}

data class DashboardContactItem(
    val personId: Int,
    val personName: String,
    val waveName: String?,
    val contactTypeKey: String,
    val contactTypeLabel: String,
    val effectiveFrequencyDays: Int,
    val daysSinceLastInteraction: Long,
    val daysOverdue: Long,
    val snoozedUntilDate: Long? = null
)

fun buildDashboardPresentation(
    people: List<Person>,
    waves: List<Wave>,
    rhythms: List<PersonContactRhythm>,
    contactTypes: List<ContactType>,
    interactions: List<InteractionLog>,
    currentTimeMillis: Long = System.currentTimeMillis()
): DashboardPresentation {
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

    val focus = due
        .sortedWith(compareByDescending<com.barisdincer.circlekeep.data.DueContact> { it.daysOverdue }
            .thenBy { it.lastInteractionDate }
            .thenBy { it.person.name })
        .map { dueContact ->
            DashboardContactItem(
                personId = dueContact.person.id,
                personName = dueContact.person.name,
                waveName = dueContact.wave?.name,
                contactTypeKey = dueContact.contactTypeKey,
                contactTypeLabel = labelFor(dueContact.contactTypeKey, contactTypes),
                effectiveFrequencyDays = dueContact.effectiveFrequencyDays,
                daysSinceLastInteraction = dueContact.daysSinceLastInteraction,
                daysOverdue = dueContact.daysOverdue
            )
        }

    return DashboardPresentation(
        overdueCount = due.count { it.daysOverdue > 0 },
        todayCount = due.count { it.daysOverdue == 0L },
        upcomingCount = upcoming.size,
        snoozedCount = snoozed.size,
        recentInteractionCount = interactions.size,
        focusContacts = focus,
        nextContact = upcoming.firstOrNull()?.let { dueContact ->
            DashboardContactItem(
                personId = dueContact.person.id,
                personName = dueContact.person.name,
                waveName = dueContact.wave?.name,
                contactTypeKey = dueContact.contactTypeKey,
                contactTypeLabel = labelFor(dueContact.contactTypeKey, contactTypes),
                effectiveFrequencyDays = dueContact.effectiveFrequencyDays,
                daysSinceLastInteraction = dueContact.daysSinceLastInteraction,
                daysOverdue = dueContact.daysOverdue
            )
        }
    )
}

private fun labelFor(key: String, contactTypes: List<ContactType>): String {
    return contactTypes.find { it.key == key }?.label ?: when (key) {
        DefaultContactTypes.CALL -> "Arama"
        DefaultContactTypes.MESSAGE -> "Mesaj"
        DefaultContactTypes.MEETING -> "Buluşma"
        else -> key
    }
}
