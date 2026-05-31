package com.barisdincer.circlekeep.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {
    // Contact types
    @Query("SELECT * FROM contact_types ORDER BY sortOrder ASC, label ASC")
    fun getAllContactTypes(): Flow<List<ContactType>>

    @Query("SELECT * FROM contact_types WHERE isActive = 1 ORDER BY sortOrder ASC, label ASC")
    fun getActiveContactTypes(): Flow<List<ContactType>>

    @Query("SELECT * FROM contact_types ORDER BY sortOrder ASC, label ASC")
    suspend fun getContactTypeSnapshot(): List<ContactType>

    @Query("SELECT * FROM contact_types WHERE `key` = :key LIMIT 1")
    suspend fun getContactTypeByKey(key: String): ContactType?

    @Query("SELECT COUNT(*) FROM contact_types")
    suspend fun getContactTypeCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContactTypesIfMissing(types: List<ContactType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactType(type: ContactType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactTypes(types: List<ContactType>)

    @Query("UPDATE contact_types SET label = :label WHERE `key` = :key")
    suspend fun renameContactType(key: String, label: String)

    @Query("UPDATE contact_types SET isActive = :isActive WHERE `key` = :key")
    suspend fun setContactTypeActive(key: String, isActive: Boolean)

    @Query("DELETE FROM contact_types WHERE `key` = :key")
    suspend fun deleteContactTypeByKey(key: String)

    // Waves
    @Query("SELECT * FROM waves")
    fun getAllWaves(): Flow<List<Wave>>

    @Query("SELECT * FROM waves")
    suspend fun getWaveSnapshot(): List<Wave>

    @Query("SELECT COUNT(*) FROM waves")
    suspend fun getWaveCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWave(wave: Wave)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaves(waves: List<Wave>)

    @Update
    suspend fun updateWave(wave: Wave)

    @Query("DELETE FROM waves WHERE id = :id")
    suspend fun deleteWaveById(id: Int)

    // People
    @Query("SELECT * FROM people ORDER BY name ASC")
    fun getAllPeople(): Flow<List<Person>>

    @Query("SELECT * FROM people ORDER BY name ASC")
    suspend fun getPeopleSnapshot(): List<Person>

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: Int): Person?

    @Query("SELECT * FROM people WHERE waveId = :waveId")
    fun getPeopleInWave(waveId: Int): Flow<List<Person>>

    @Query("SELECT COUNT(*) FROM people WHERE waveId = :waveId")
    suspend fun countPeopleInWave(waveId: Int): Int

    @Query("SELECT COUNT(*) FROM people WHERE preferredContactTypeKey = :key")
    suspend fun countPeopleWithPreferredContactType(key: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: Person)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<Person>)

    @Update
    suspend fun updatePerson(person: Person)

    @Query("UPDATE people SET waveId = :waveId WHERE id = :personId")
    suspend fun updatePersonWave(personId: Int, waveId: Int?)

    @Query("UPDATE people SET lastInteractionDate = :date WHERE id = :personId")
    suspend fun updateLastInteraction(personId: Int, date: Long)

    @Query("UPDATE people SET snoozedUntilDate = :date WHERE id = :personId")
    suspend fun updateSnoozedUntil(personId: Int, date: Long?)

    @Query(
        """
        UPDATE people
        SET lastInteractionDate = CASE
                WHEN lastInteractionDate < :date THEN :date
                ELSE lastInteractionDate
            END,
            lastCallLogSyncDate = :date
        WHERE id = :personId
        """
    )
    suspend fun updateLastCallLogInteraction(personId: Int, date: Long)

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deletePersonById(id: Int)

    // Interactions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractionLog(log: InteractionLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractionLogs(logs: List<InteractionLog>)

    @Query("SELECT * FROM interaction_logs WHERE personId = :personId ORDER BY timestamp DESC")
    fun getInteractionLogsForPerson(personId: Int): Flow<List<InteractionLog>>

    @Query("SELECT * FROM interaction_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentInteractions(): Flow<List<InteractionLog>>

    @Query("SELECT * FROM interaction_logs ORDER BY timestamp DESC")
    suspend fun getInteractionSnapshot(): List<InteractionLog>

    @Query("SELECT COUNT(*) FROM interaction_logs WHERE type = :type")
    suspend fun countInteractionLogsByType(type: String): Int

    @Query("DELETE FROM interaction_logs")
    suspend fun clearInteractionLogs()

    @Query("DELETE FROM contact_types")
    suspend fun clearContactTypes()

    @Query("DELETE FROM people")
    suspend fun clearPeople()

    @Query("DELETE FROM waves")
    suspend fun clearWaves()

    // Transaction to log interaction and update person
    @Transaction
    suspend fun logInteractionAndUpdatePerson(log: InteractionLog) {
        insertInteractionLog(log)
        updateLastInteraction(log.personId, log.timestamp)
    }

    @Transaction
    suspend fun logCallInteractionAndUpdatePerson(log: InteractionLog) {
        insertInteractionLog(log)
        updateLastCallLogInteraction(log.personId, log.timestamp)
    }

    @Transaction
    suspend fun replaceAllData(
        contactTypes: List<ContactType>,
        waves: List<Wave>,
        people: List<Person>,
        logs: List<InteractionLog>
    ) {
        clearInteractionLogs()
        clearPeople()
        clearWaves()
        clearContactTypes()
        insertContactTypes(contactTypes)
        insertWaves(waves)
        insertPeople(people)
        insertInteractionLogs(logs)
    }
}
