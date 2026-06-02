package com.barisdincer.circlekeep.data

data class DueContact(
    val person: Person,
    val wave: Wave?,
    val contactTypeKey: String,
    val effectiveFrequencyDays: Int,
    val lastInteractionDate: Long,
    val daysSinceLastInteraction: Long,
    val daysOverdue: Long,
    val snoozedUntilDate: Long? = null
)

object ContactReminderCalculator {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun dueContacts(
        people: List<Person>,
        waves: List<Wave>,
        rhythms: List<PersonContactRhythm> = emptyList(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<DueContact> {
        return people.flatMap { person ->
            if (!person.reminderEnabled) return@flatMap emptyList()

            val (wave, frequencyDays) = cadenceFor(person, waves) ?: return@flatMap emptyList()
            contactRhythmsFor(person, rhythms).mapNotNull { rhythm ->
                val snoozedUntil = rhythm.snoozedUntilDate ?: person.snoozedUntilDate
                if ((snoozedUntil ?: 0L) > currentTimeMillis) return@mapNotNull null

                val effectiveFrequencyDays = rhythm.customFrequencyDays?.takeIf { it > 0 } ?: frequencyDays
                val lastInteractionDate = rhythm.lastInteractionDate ?: person.lastInteractionDate
                val daysSince = ((currentTimeMillis - lastInteractionDate) / DAY_MILLIS).coerceAtLeast(0L)
                val daysOverdue = daysSince - effectiveFrequencyDays
                if (daysOverdue < 0) return@mapNotNull null

                DueContact(
                    person = person,
                    wave = wave,
                    contactTypeKey = rhythm.contactTypeKey,
                    effectiveFrequencyDays = effectiveFrequencyDays,
                    lastInteractionDate = lastInteractionDate,
                    daysSinceLastInteraction = daysSince,
                    daysOverdue = daysOverdue,
                    snoozedUntilDate = snoozedUntil
                )
            }
        }.sortedWith(
            compareByDescending<DueContact> { it.daysOverdue }
                .thenBy { it.lastInteractionDate }
                .thenBy { it.contactTypeKey }
        )
    }

    fun upcomingContacts(
        people: List<Person>,
        waves: List<Wave>,
        rhythms: List<PersonContactRhythm> = emptyList(),
        withinDays: Int = 7,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<DueContact> {
        return nextContacts(
            people = people,
            waves = waves,
            rhythms = rhythms,
            currentTimeMillis = currentTimeMillis,
            limit = Int.MAX_VALUE
        ).filter { -it.daysOverdue in 1..withinDays }
    }

    fun nextContacts(
        people: List<Person>,
        waves: List<Wave>,
        rhythms: List<PersonContactRhythm> = emptyList(),
        currentTimeMillis: Long = System.currentTimeMillis(),
        limit: Int = 10
    ): List<DueContact> {
        return people.flatMap { person ->
            if (!person.reminderEnabled) return@flatMap emptyList()

            val (wave, frequencyDays) = cadenceFor(person, waves) ?: return@flatMap emptyList()
            contactRhythmsFor(person, rhythms).mapNotNull { rhythm ->
                val snoozedUntil = rhythm.snoozedUntilDate ?: person.snoozedUntilDate
                if ((snoozedUntil ?: 0L) > currentTimeMillis) return@mapNotNull null

                val effectiveFrequencyDays = rhythm.customFrequencyDays?.takeIf { it > 0 } ?: frequencyDays
                val lastInteractionDate = rhythm.lastInteractionDate ?: person.lastInteractionDate
                val daysSince = ((currentTimeMillis - lastInteractionDate) / DAY_MILLIS).coerceAtLeast(0L)
                val daysUntilDue = effectiveFrequencyDays - daysSince
                if (daysUntilDue <= 0) return@mapNotNull null

                DueContact(
                    person = person,
                    wave = wave,
                    contactTypeKey = rhythm.contactTypeKey,
                    effectiveFrequencyDays = effectiveFrequencyDays,
                    lastInteractionDate = lastInteractionDate,
                    daysSinceLastInteraction = daysSince,
                    daysOverdue = -daysUntilDue
                )
            }
        }.sortedWith(
            compareBy<DueContact> { -it.daysOverdue }
                .thenBy { it.person.name }
                .thenBy { it.contactTypeKey }
        ).take(limit.coerceAtLeast(0))
    }

    fun snoozedContacts(
        people: List<Person>,
        waves: List<Wave>,
        rhythms: List<PersonContactRhythm> = emptyList(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<DueContact> {
        return people.flatMap { person ->
            if (!person.reminderEnabled) return@flatMap emptyList()

            val (wave, frequencyDays) = cadenceFor(person, waves) ?: return@flatMap emptyList()
            contactRhythmsFor(person, rhythms).mapNotNull { rhythm ->
                val snoozedUntil = rhythm.snoozedUntilDate ?: person.snoozedUntilDate
                if (snoozedUntil == null || snoozedUntil <= currentTimeMillis) return@mapNotNull null

                val lastInteractionDate = rhythm.lastInteractionDate ?: person.lastInteractionDate
                val daysSince = ((currentTimeMillis - lastInteractionDate) / DAY_MILLIS).coerceAtLeast(0L)
                DueContact(
                    person = person,
                    wave = wave,
                    contactTypeKey = rhythm.contactTypeKey,
                    effectiveFrequencyDays = rhythm.customFrequencyDays?.takeIf { it > 0 } ?: frequencyDays,
                    lastInteractionDate = lastInteractionDate,
                    daysSinceLastInteraction = daysSince,
                    daysOverdue = 0,
                    snoozedUntilDate = snoozedUntil
                )
            }
        }.sortedWith(
            compareBy<DueContact> { it.snoozedUntilDate ?: Long.MAX_VALUE }
                .thenBy { it.person.name }
                .thenBy { it.contactTypeKey }
        )
    }

    private fun cadenceFor(person: Person, waves: List<Wave>): Pair<Wave?, Int>? {
        val wave = waves.find { it.id == person.waveId }
        val frequencyDays = person.customFrequencyDays?.takeIf { it > 0 }
            ?: wave?.frequencyDays
            ?: return null
        return wave to frequencyDays
    }

    private fun contactRhythmsFor(person: Person, rhythms: List<PersonContactRhythm>): List<PersonContactRhythm> {
        val personRhythms = rhythms.filter { it.personId == person.id }
        val activeRhythms = personRhythms
            .filter { it.isActive }
            .distinctBy { it.contactTypeKey }
        if (personRhythms.isNotEmpty()) return activeRhythms
        return listOf(
            PersonContactRhythm(
                personId = person.id,
                contactTypeKey = person.preferredContactTypeKey,
                isActive = true,
                customFrequencyDays = person.customFrequencyDays,
                lastInteractionDate = person.lastInteractionDate,
                snoozedUntilDate = person.snoozedUntilDate
            )
        )
    }
}
