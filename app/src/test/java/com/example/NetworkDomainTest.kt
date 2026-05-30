package com.example

import com.example.data.ContactReminderCalculator
import com.example.data.Person
import com.example.data.PhoneNumberNormalizer
import com.example.data.Wave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDomainTest {
    @Test
    fun `normalizes Turkish phone number formats`() {
        assertEquals("905321234567", PhoneNumberNormalizer.normalize("0532 123 45 67"))
        assertEquals("905321234567", PhoneNumberNormalizer.normalize("5321234567"))
        assertTrue(PhoneNumberNormalizer.matches("+90 532 123 45 67", "0532 123 45 67"))
    }

    @Test
    fun `due contacts excludes paused people and sorts by overdue days`() {
        val now = 100L * DAY_MILLIS
        val closeFriends = Wave(id = 1, name = "Close Friends", frequencyDays = 7)
        val monthly = Wave(id = 2, name = "Monthly", frequencyDays = 30)

        val dueLater = Person(
            id = 1,
            name = "A",
            phoneNumber = "5321111111",
            waveId = closeFriends.id,
            lastInteractionDate = now - 8L * DAY_MILLIS
        )
        val dueFirst = Person(
            id = 2,
            name = "B",
            phoneNumber = "5322222222",
            waveId = monthly.id,
            lastInteractionDate = now - 45L * DAY_MILLIS
        )
        val paused = Person(
            id = 3,
            name = "C",
            phoneNumber = "5323333333",
            waveId = closeFriends.id,
            reminderEnabled = false,
            lastInteractionDate = now - 20L * DAY_MILLIS
        )

        val dueContacts = ContactReminderCalculator.dueContacts(
            people = listOf(dueLater, dueFirst, paused),
            waves = listOf(closeFriends, monthly),
            currentTimeMillis = now
        )

        assertEquals(listOf("B", "A"), dueContacts.map { it.person.name })
        assertEquals(15, dueContacts.first().daysOverdue)
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
