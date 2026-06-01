package com.barisdincer.circlekeep.data

data class DueContact(
    val person: Person,
    val wave: Wave?,
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

            val (wave, frequencyDays) = cadenceFor(person, waves) ?: return@mapNotNull null
            val daysSince = ((currentTimeMillis - person.lastInteractionDate) / DAY_MILLIS).coerceAtLeast(0L)
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
        return nextContacts(
            people = people,
            waves = waves,
            currentTimeMillis = currentTimeMillis,
            limit = Int.MAX_VALUE
        ).filter { -it.daysOverdue in 1..withinDays }
    }

    fun nextContacts(
        people: List<Person>,
        waves: List<Wave>,
        currentTimeMillis: Long = System.currentTimeMillis(),
        limit: Int = 10
    ): List<DueContact> {
        return people.mapNotNull { person ->
            if (!person.reminderEnabled) return@mapNotNull null
            if ((person.snoozedUntilDate ?: 0L) > currentTimeMillis) return@mapNotNull null

            val (wave, frequencyDays) = cadenceFor(person, waves) ?: return@mapNotNull null
            val daysSince = ((currentTimeMillis - person.lastInteractionDate) / DAY_MILLIS).coerceAtLeast(0L)
            val daysUntilDue = frequencyDays - daysSince
            if (daysUntilDue <= 0) return@mapNotNull null

            DueContact(
                person = person,
                wave = wave,
                effectiveFrequencyDays = frequencyDays,
                daysSinceLastInteraction = daysSince,
                daysOverdue = -daysUntilDue
            )
        }.sortedWith(
            compareBy<DueContact> { -it.daysOverdue }
                .thenBy { it.person.name }
        ).take(limit.coerceAtLeast(0))
    }

    private fun cadenceFor(person: Person, waves: List<Wave>): Pair<Wave?, Int>? {
        val wave = waves.find { it.id == person.waveId }
        val frequencyDays = person.customFrequencyDays?.takeIf { it > 0 }
            ?: wave?.frequencyDays
            ?: return null
        return wave to frequencyDays
    }
}
