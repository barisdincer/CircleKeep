package com.example.data

import kotlinx.coroutines.flow.Flow

class NetworkRepository(private val networkDao: NetworkDao) {

    val allWaves: Flow<List<Wave>> = networkDao.getAllWaves()
    val allPeople: Flow<List<Person>> = networkDao.getAllPeople()
    val recentInteractions: Flow<List<InteractionLog>> = networkDao.getRecentInteractions()

    suspend fun insertWave(wave: Wave) {
        networkDao.insertWave(wave)
    }

    suspend fun deleteWave(id: Int) {
        networkDao.deleteWaveById(id)
    }

    suspend fun insertPerson(person: Person) {
        networkDao.insertPerson(person)
    }

    suspend fun updatePerson(person: Person) {
        networkDao.updatePerson(person)
    }

    suspend fun updatePersonWave(personId: Int, waveId: Int?) {
        networkDao.updatePersonWave(personId, waveId)
    }

    suspend fun logInteraction(personId: Int, type: String) {
        val log = InteractionLog(personId = personId, type = type)
        networkDao.logInteractionAndUpdatePerson(log)
    }

    fun getLogsForPerson(personId: Int): Flow<List<InteractionLog>> {
        return networkDao.getInteractionLogsForPerson(personId)
    }
}
