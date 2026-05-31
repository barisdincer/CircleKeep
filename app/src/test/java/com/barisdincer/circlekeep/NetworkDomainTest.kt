package com.barisdincer.circlekeep

import com.barisdincer.circlekeep.data.ContactReminderCalculator
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.InteractionLog
import com.barisdincer.circlekeep.data.NetworkBackupCodec
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.PhoneNumberNormalizer
import com.barisdincer.circlekeep.data.Wave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
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

    @Test
    fun `person custom frequency overrides wave rhythm`() {
        val now = 100L * DAY_MILLIS
        val monthly = Wave(id = 1, name = "Monthly", frequencyDays = 30)
        val customDue = Person(
            id = 1,
            name = "A",
            phoneNumber = "5321111111",
            waveId = monthly.id,
            customFrequencyDays = 10,
            lastInteractionDate = now - 12L * DAY_MILLIS
        )
        val groupNotDue = Person(
            id = 2,
            name = "B",
            phoneNumber = "5322222222",
            waveId = monthly.id,
            lastInteractionDate = now - 20L * DAY_MILLIS
        )

        val dueContacts = ContactReminderCalculator.dueContacts(
            people = listOf(customDue, groupNotDue),
            waves = listOf(monthly),
            currentTimeMillis = now
        )

        assertEquals(listOf("A"), dueContacts.map { it.person.name })
        assertEquals(10, dueContacts.first().effectiveFrequencyDays)
        assertEquals(2, dueContacts.first().daysOverdue)
    }

    @Test
    fun `backup includes relationship rhythm and memory fields`() {
        val wave = Wave(id = 1, name = "Friends", frequencyDays = 21)
        val person = Person(
            id = 1,
            name = "Ayse",
            phoneNumber = "5321111111",
            waveId = wave.id,
            preferredContactTypeKey = DefaultContactTypes.MESSAGE,
            customFrequencyDays = 14,
            memoryNotes = "Kahveyi sever",
            nextConversationHint = "Yeni işi sor",
            importantDateLabel = "Dogum gunu",
            importantDateMillis = 1234L
        )
        val log = InteractionLog(id = 1, personId = 1, timestamp = 2000L, type = DefaultContactTypes.MEETING, note = "Kisa bir kahve")

        val decoded = NetworkBackupCodec.decode(
            NetworkBackupCodec.encode(
                contactTypes = DefaultContactTypes.all,
                waves = listOf(wave),
                people = listOf(person),
                logs = listOf(log)
            )
        )

        assertEquals(DefaultContactTypes.all.map { it.key }, decoded.contactTypes.map { it.key })
        assertEquals(DefaultContactTypes.MESSAGE, decoded.people.first().preferredContactTypeKey)
        assertEquals(14, decoded.people.first().customFrequencyDays)
        assertEquals("Kahveyi sever", decoded.people.first().memoryNotes)
        assertEquals("Yeni işi sor", decoded.people.first().nextConversationHint)
        assertEquals("Dogum gunu", decoded.people.first().importantDateLabel)
        assertEquals(1234L, decoded.people.first().importantDateMillis)
        assertEquals("Kisa bir kahve", decoded.logs.first().note)
    }

    @Test
    fun `old backup decodes with safe defaults`() {
        val oldBackup = """
            {
              "version": 1,
              "waves": [{"id": 1, "name": "Friends", "frequencyDays": 21}],
              "people": [{
                "id": 1,
                "name": "Ayse",
                "phoneNumber": "5321111111",
                "normalizedPhoneNumber": "905321111111",
                "waveId": 1,
                "notes": "",
                "tags": "",
                "reminderEnabled": true,
                "addedDate": 1000,
                "lastInteractionDate": 1000
              }],
              "interactionLogs": [{"id": 1, "personId": 1, "timestamp": 1000, "type": "CALL"}]
            }
        """.trimIndent()

        val decoded = NetworkBackupCodec.decode(oldBackup)

        assertEquals(DefaultContactTypes.all.map { it.key }, decoded.contactTypes.map { it.key })
        assertEquals(DefaultContactTypes.CALL, decoded.people.first().preferredContactTypeKey)
        assertEquals(null, decoded.people.first().customFrequencyDays)
        assertEquals("", decoded.people.first().memoryNotes)
        assertEquals("", decoded.logs.first().note)
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
