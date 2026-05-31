package com.barisdincer.circlekeep.data

data class DueContact(
    val person: Person,
    val wave: Wave,
    val effectiveFrequencyDays: Int,
    val daysSinceLastInteraction: Long,
    val daysOverdue: Long
)

object ContactReminderCalculator {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun dueContacts(
        people: List<Person>,
        waves: List<Wave>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<DueContact> {
        return people.mapNotNull { person ->
            if (!person.reminderEnabled) return@mapNotNull null
            if ((person.snoozedUntilDate ?: 0L) > currentTimeMillis) return@mapNotNull null

            val wave = waves.find { it.id == person.waveId } ?: return@mapNotNull null
            val frequencyDays = person.customFrequencyDays?.takeIf { it > 0 } ?: wave.frequencyDays
            val daysSince = (currentTimeMillis - person.lastInteractionDate) / DAY_MILLIS
            val daysOverdue = daysSince - frequencyDays
            if (daysOverdue < 0) return@mapNotNull null

            DueContact(
                person = person,
                wave = wave,
                effectiveFrequencyDays = frequencyDays,
                daysSinceLastInteraction = daysSince,
                daysOverdue = daysOverdue
            )
        }.sortedWith(
            compareByDescending<DueContact> { it.daysOverdue }
                .thenBy { it.person.lastInteractionDate }
        )
    }

    fun upcomingContacts(
        people: List<Person>,
        waves: List<Wave>,
        withinDays: Int = 7,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<DueContact> {
        return people.mapNotNull { person ->
            if (!person.reminderEnabled) return@mapNotNull null
            if ((person.snoozedUntilDate ?: 0L) > currentTimeMillis) return@mapNotNull null

            val wave = waves.find { it.id == person.waveId } ?: return@mapNotNull null
            val frequencyDays = person.customFrequencyDays?.takeIf { it > 0 } ?: wave.frequencyDays
            val daysSince = (currentTimeMillis - person.lastInteractionDate) / DAY_MILLIS
            val daysUntilDue = frequencyDays - daysSince
            if (daysUntilDue !in 1..withinDays) return@mapNotNull null

            DueContact(
                person = person,
                wave = wave,
                effectiveFrequencyDays = frequencyDays,
                daysSinceLastInteraction = daysSince,
                daysOverdue = -daysUntilDue
            )
        }.sortedByDescending { it.daysOverdue }
    }
}
