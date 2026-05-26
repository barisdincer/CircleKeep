package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {
    // Waves
    @Query("SELECT * FROM waves")
    fun getAllWaves(): Flow<List<Wave>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWave(wave: Wave)

    @Query("DELETE FROM waves WHERE id = :id")
    suspend fun deleteWaveById(id: Int)

    // People
    @Query("SELECT * FROM people ORDER BY name ASC")
    fun getAllPeople(): Flow<List<Person>>
    
    @Query("SELECT * FROM people WHERE waveId = :waveId")
    fun getPeopleInWave(waveId: Int): Flow<List<Person>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: Person)

    @androidx.room.Update
    suspend fun updatePerson(person: Person)

    @Query("UPDATE people SET waveId = :waveId WHERE id = :personId")
    suspend fun updatePersonWave(personId: Int, waveId: Int?)

    @Query("UPDATE people SET lastInteractionDate = :date WHERE id = :personId")
    suspend fun updateLastInteraction(personId: Int, date: Long)

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deletePersonById(id: Int)

    // Interactions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractionLog(log: InteractionLog)

    @Query("SELECT * FROM interaction_logs WHERE personId = :personId ORDER BY timestamp DESC")
    fun getInteractionLogsForPerson(personId: Int): Flow<List<InteractionLog>>
    
    @Query("SELECT * FROM interaction_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentInteractions(): Flow<List<InteractionLog>>

    // Transaction to log interaction and update person
    @Transaction
    suspend fun logInteractionAndUpdatePerson(log: InteractionLog) {
        insertInteractionLog(log)
        updateLastInteraction(log.personId, log.timestamp)
    }
}
