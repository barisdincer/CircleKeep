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
}
