package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {
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

    @Query("DELETE FROM waves WHERE id = :id")
    suspend fun deleteWaveById(id: Int)

    // People
    @Query("SELECT * FROM people ORDER BY name ASC")
    fun getAllPeople(): Flow<List<Person>>

    @Query("SELECT * FROM people ORDER BY name ASC")
    suspend fun getPeopleSnapshot(): List<Person>
    
    @Query("SELECT * FROM people WHERE waveId = :waveId")
    fun getPeopleInWave(waveId: Int): Flow<List<Person>>

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

    @Query("DELETE FROM interaction_logs")
    suspend fun clearInteractionLogs()

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
    suspend fun replaceAllData(waves: List<Wave>, people: List<Person>, logs: List<InteractionLog>) {
        clearInteractionLogs()
        clearPeople()
        clearWaves()
        insertWaves(waves)
        insertPeople(people)
        insertInteractionLogs(logs)
    }
}
