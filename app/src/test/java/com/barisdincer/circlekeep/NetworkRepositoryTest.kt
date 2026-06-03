package com.barisdincer.circlekeep

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.barisdincer.circlekeep.data.AppDatabase
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.InteractionLog
import com.barisdincer.circlekeep.data.NetworkRepository
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.Wave
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NetworkRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: NetworkRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NetworkRepository(database.networkDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `used group delete is blocked`() = runBlocking {
        repository.insertWave(Wave(id = 1, name = "Yakınlar", frequencyDays = 7))
        repository.insertPerson(Person(id = 1, name = "Ayse", phoneNumber = "5321111111", waveId = 1))

        val result = repository.deleteWaveIfUnused(1)

        assertFalse(result.success)
        assertTrue(result.message.contains("kişi"))
        assertEquals(listOf("Yakınlar"), repository.getWaveSnapshot().map { it.name })
    }

    @Test
    fun `unused group can be deleted`() = runBlocking {
        repository.insertWave(Wave(id = 1, name = "Boş grup", frequencyDays = 14))

        val result = repository.deleteWaveIfUnused(1)

        assertTrue(result.success)
        assertTrue(repository.getWaveSnapshot().isEmpty())
    }

    @Test
    fun `multiple existing people can be moved into group together`() = runBlocking {
        repository.insertWave(Wave(id = 1, name = "Yakınlar", frequencyDays = 7))
        repository.insertPerson(Person(id = 1, name = "Ayse", phoneNumber = "5321111111", waveId = null))
        repository.insertPerson(Person(id = 2, name = "Mehmet", phoneNumber = "5322222222", waveId = null))
        repository.insertPerson(Person(id = 3, name = "Zeynep", phoneNumber = "5323333333", waveId = null))

        val result = repository.updatePeopleWave(listOf(1, 2, 2), 1)

        val peopleById = repository.getPeopleSnapshot().associateBy { it.id }
        assertTrue(result.success)
        assertEquals(1, peopleById[1]?.waveId)
        assertEquals(1, peopleById[2]?.waveId)
        assertEquals(null, peopleById[3]?.waveId)
    }

    @Test
    fun `used contact type delete is blocked`() = runBlocking {
        val type = ContactType(key = "CUSTOM_COFFEE", label = "Kahve", isActive = true, sortOrder = 10)
        database.networkDao().insertContactType(type)
        repository.insertPerson(
            Person(
                id = 1,
                name = "Ayse",
                phoneNumber = "5321111111",
                waveId = null,
                preferredContactTypeKey = type.key
            )
        )

        val result = repository.deleteContactTypeIfUnused(type.key)

        assertFalse(result.success)
        assertEquals(listOf(type.key), repository.getContactTypeSnapshot().map { it.key })
    }

    @Test
    fun `unused custom contact type is deleted`() = runBlocking {
        val type = ContactType(key = "CUSTOM_WALK", label = "Yürüyüş", isActive = true, sortOrder = 10)
        database.networkDao().insertContactType(type)

        val result = repository.deleteContactTypeIfUnused(type.key)

        assertTrue(result.success)
        assertTrue(repository.getContactTypeSnapshot().none { it.key == type.key })
    }

    @Test
    fun `unused default contact type is hidden and seed does not reactivate it`() = runBlocking {
        repository.ensureDefaultContactTypes()

        val result = repository.deleteContactTypeIfUnused(DefaultContactTypes.MEETING)
        repository.ensureDefaultContactTypes()

        val meeting = repository.getContactTypeSnapshot().first { it.key == DefaultContactTypes.MEETING }
        assertTrue(result.success)
        assertFalse(meeting.isActive)
    }

    @Test
    fun `contact type with old logs cannot be deleted`() = runBlocking {
        val type = ContactType(key = "CUSTOM_GAME", label = "Oyun", isActive = true, sortOrder = 10)
        database.networkDao().insertContactType(type)
        repository.insertPerson(Person(id = 1, name = "Ayse", phoneNumber = "5321111111", waveId = null))
        database.networkDao().insertInteractionLog(
            InteractionLog(id = 1, personId = 1, timestamp = 1000L, type = type.key)
        )

        val result = repository.deleteContactTypeIfUnused(type.key)

        assertFalse(result.success)
        assertEquals(listOf(type.key), repository.getContactTypeSnapshot().map { it.key })
    }

    @Test
    fun `initial interaction is logged when adding person`() = runBlocking {
        val timestamp = 1_700_000_000_000L

        val personId = repository.insertPerson(
            person = Person(name = "Ayse", phoneNumber = "5321111111", waveId = null),
            initialInteractionType = DefaultContactTypes.MESSAGE,
            initialInteractionTimestamp = timestamp,
            initialInteractionNote = "Hoş geldin konuştuk"
        ).toInt()

        val person = repository.getPeopleSnapshot().single()
        val log = repository.getInteractionSnapshot().single()
        assertEquals(personId, person.id)
        assertEquals(timestamp, person.lastInteractionDate)
        assertEquals(DefaultContactTypes.MESSAGE, log.type)
        assertEquals("Hoş geldin konuştuk", log.note)
    }

    @Test
    fun `batch interaction logs update every selected person`() = runBlocking {
        val timestamp = 1_700_000_100_000L
        repository.insertPerson(Person(id = 1, name = "Ayse", phoneNumber = "5321111111", waveId = null, lastInteractionDate = 1L))
        repository.insertPerson(Person(id = 2, name = "Mehmet", phoneNumber = "5322222222", waveId = null, lastInteractionDate = 1L))

        repository.logInteractions(
            personIds = listOf(1, 2, 2),
            type = DefaultContactTypes.MEETING,
            note = "Kahvede buluştuk",
            timestamp = timestamp
        )

        val people = repository.getPeopleSnapshot().sortedBy { it.id }
        val logs = repository.getInteractionSnapshot().sortedBy { it.personId }
        assertEquals(listOf(timestamp, timestamp), people.map { it.lastInteractionDate })
        assertEquals(listOf(1, 2), logs.map { it.personId })
        assertEquals(listOf(DefaultContactTypes.MEETING, DefaultContactTypes.MEETING), logs.map { it.type })
        assertEquals(listOf("Kahvede buluştuk", "Kahvede buluştuk"), logs.map { it.note })
    }

    @Test
    fun `interaction logs update only matching contact rhythm`() = runBlocking {
        val callTime = 1_700_000_000_000L
        val meetingTime = 1_700_100_000_000L
        repository.insertPerson(Person(id = 1, name = "Ayse", phoneNumber = "5321111111", waveId = null, lastInteractionDate = 1L))

        repository.logInteraction(personId = 1, type = DefaultContactTypes.CALL, timestamp = callTime)
        repository.logInteraction(personId = 1, type = DefaultContactTypes.MEETING, timestamp = meetingTime)

        val rhythms = repository.getPersonContactRhythmSnapshot().associateBy { it.contactTypeKey }
        assertEquals(callTime, rhythms[DefaultContactTypes.CALL]?.lastInteractionDate)
        assertEquals(meetingTime, rhythms[DefaultContactTypes.MEETING]?.lastInteractionDate)
        assertEquals(meetingTime, repository.getPeopleSnapshot().single().lastInteractionDate)
    }

    @Test
    fun `updating person custom frequency syncs all selected contact rhythms`() = runBlocking {
        repository.insertPerson(
            Person(
                id = 1,
                name = "Ayse",
                phoneNumber = "5321111111",
                waveId = null,
                customFrequencyDays = 90
            )
        )
        repository.setPersonContactRhythmActive(1, DefaultContactTypes.MEETING, true)

        val person = repository.getPeopleSnapshot().single()
        repository.updatePerson(person.copy(customFrequencyDays = 45))

        val rhythms = repository.getPersonContactRhythmSnapshot().sortedBy { it.contactTypeKey }
        assertEquals(listOf(45, 45), rhythms.map { it.customFrequencyDays })
    }

    @Test
    fun `backdated interaction does not replace newer last interaction`() = runBlocking {
        repository.insertPerson(Person(id = 1, name = "Ayse", phoneNumber = "5321111111", waveId = null, addedDate = 10L, lastInteractionDate = 10L))
        repository.logInteraction(personId = 1, type = DefaultContactTypes.CALL, timestamp = 300L)
        repository.logInteraction(personId = 1, type = DefaultContactTypes.MEETING, timestamp = 100L)

        assertEquals(300L, repository.getPeopleSnapshot().single().lastInteractionDate)
        assertEquals(listOf(300L, 100L), repository.getInteractionSnapshot().map { it.timestamp })
    }

    @Test
    fun `updating latest log refreshes person last interaction`() = runBlocking {
        repository.insertPerson(Person(id = 1, name = "Ayse", phoneNumber = "5321111111", waveId = null, addedDate = 10L, lastInteractionDate = 10L))
        repository.logInteraction(personId = 1, type = DefaultContactTypes.CALL, timestamp = 100L)
        val log = repository.getInteractionSnapshot().single()

        val result = repository.updateInteractionLog(log.copy(timestamp = 250L, type = DefaultContactTypes.MESSAGE, note = "Güncellendi"))

        val person = repository.getPeopleSnapshot().single()
        val updatedLog = repository.getInteractionSnapshot().single()
        assertTrue(result.success)
        assertEquals(250L, person.lastInteractionDate)
        assertEquals(DefaultContactTypes.MESSAGE, updatedLog.type)
        assertEquals("Güncellendi", updatedLog.note)
    }

    @Test
    fun `updating grouped event logs refreshes every participant`() = runBlocking {
        repository.insertPerson(Person(id = 1, name = "Ayse", phoneNumber = "5321111111", waveId = null, addedDate = 10L, lastInteractionDate = 10L))
        repository.insertPerson(Person(id = 2, name = "Mehmet", phoneNumber = "5322222222", waveId = null, addedDate = 20L, lastInteractionDate = 20L))
        repository.logInteractions(
            personIds = listOf(1, 2),
            type = DefaultContactTypes.MEETING,
            note = "Eski not",
            timestamp = 100L
        )
        val updatedLogs = repository.getInteractionSnapshot().map {
            it.copy(type = DefaultContactTypes.CALL, note = "Yeni not", timestamp = 300L)
        }

        val result = repository.updateInteractionLogs(updatedLogs)

        val people = repository.getPeopleSnapshot().sortedBy { it.id }
        val logs = repository.getInteractionSnapshot().sortedBy { it.personId }
        assertTrue(result.success)
        assertEquals(listOf(300L, 300L), people.map { it.lastInteractionDate })
        assertEquals(listOf(DefaultContactTypes.CALL, DefaultContactTypes.CALL), logs.map { it.type })
        assertEquals(listOf("Yeni not", "Yeni not"), logs.map { it.note })
    }

    @Test
    fun `deleting latest log falls back to previous log or added date`() = runBlocking {
        repository.insertPerson(Person(id = 1, name = "Ayse", phoneNumber = "5321111111", waveId = null, addedDate = 10L, lastInteractionDate = 10L))
        repository.logInteraction(personId = 1, type = DefaultContactTypes.CALL, timestamp = 100L)
        repository.logInteraction(personId = 1, type = DefaultContactTypes.MESSAGE, timestamp = 200L)
        val latestLog = repository.getInteractionSnapshot().first { it.timestamp == 200L }

        val result = repository.deleteInteractionLog(latestLog.id)

        assertTrue(result.success)
        assertEquals(100L, repository.getPeopleSnapshot().single().lastInteractionDate)

        val remainingLog = repository.getInteractionSnapshot().single()
        repository.deleteInteractionLog(remainingLog.id)

        assertEquals(10L, repository.getPeopleSnapshot().single().lastInteractionDate)
        assertTrue(repository.getInteractionSnapshot().isEmpty())
    }

    @Test
    fun `deleting grouped event logs refreshes every participant`() = runBlocking {
        repository.insertPerson(Person(id = 1, name = "Ayse", phoneNumber = "5321111111", waveId = null, addedDate = 10L, lastInteractionDate = 10L))
        repository.insertPerson(Person(id = 2, name = "Mehmet", phoneNumber = "5322222222", waveId = null, addedDate = 20L, lastInteractionDate = 20L))
        repository.logInteractions(
            personIds = listOf(1, 2),
            type = DefaultContactTypes.MEETING,
            note = "Kahve",
            timestamp = 300L
        )
        val ids = repository.getInteractionSnapshot().map { it.id }

        val result = repository.deleteInteractionLogs(ids)

        val people = repository.getPeopleSnapshot().sortedBy { it.id }
        assertTrue(result.success)
        assertTrue(repository.getInteractionSnapshot().isEmpty())
        assertEquals(listOf(10L, 20L), people.map { it.lastInteractionDate })
    }
}
