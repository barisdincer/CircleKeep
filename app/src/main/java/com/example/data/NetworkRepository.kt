package com.example.data

import kotlinx.coroutines.flow.Flow

class NetworkRepository(private val networkDao: NetworkDao) {

    val allWaves: Flow<List<Wave>> = networkDao.getAllWaves()
    val allPeople: Flow<List<Person>> = networkDao.getAllPeople()
    val recentInteractions: Flow<List<InteractionLog>> = networkDao.getRecentInteractions()

    suspend fun insertWave(wave: Wave) {
        networkDao.insertWave(wave)
    }

    suspend fun ensureDefaultWaves() {
        if (networkDao.getWaveCount() > 0) return

        networkDao.insertWaves(
            listOf(
                Wave(name = "Yakınlar", frequencyDays = 7),
                Wave(name = "Arkadaşlar", frequencyDays = 21),
                Wave(name = "Tanıdıklar", frequencyDays = 60)
            )
        )
    }

    suspend fun deleteWave(id: Int) {
        networkDao.deleteWaveById(id)
    }

    suspend fun insertPerson(person: Person) {
        networkDao.insertPerson(person.withNormalizedPhone())
    }

    suspend fun insertPeople(people: List<Person>) {
        if (people.isEmpty()) return
        networkDao.insertPeople(people.map { it.withNormalizedPhone() })
    }

    suspend fun updatePerson(person: Person) {
        networkDao.updatePerson(person.withNormalizedPhone())
    }

    suspend fun updatePersonWave(personId: Int, waveId: Int?) {
        networkDao.updatePersonWave(personId, waveId)
    }

    suspend fun logInteraction(personId: Int, type: String, timestamp: Long = System.currentTimeMillis()) {
        val log = InteractionLog(personId = personId, timestamp = timestamp, type = type)
        networkDao.logInteractionAndUpdatePerson(log)
    }

    suspend fun snoozePerson(personId: Int, untilDate: Long) {
        networkDao.updateSnoozedUntil(personId, untilDate)
    }

    suspend fun logCallInteraction(person: Person, timestamp: Long) {
        val lastSyncedCall = person.lastCallLogSyncDate ?: 0L
        if (timestamp <= lastSyncedCall) return

        val log = InteractionLog(personId = person.id, timestamp = timestamp, type = "CALL")
        networkDao.logCallInteractionAndUpdatePerson(log)
    }

    suspend fun getPeopleSnapshot(): List<Person> {
        return networkDao.getPeopleSnapshot()
    }

    suspend fun getWaveSnapshot(): List<Wave> {
        return networkDao.getWaveSnapshot()
    }

    suspend fun getInteractionSnapshot(): List<InteractionLog> {
        return networkDao.getInteractionSnapshot()
    }

    suspend fun replaceAllData(waves: List<Wave>, people: List<Person>, logs: List<InteractionLog>) {
        networkDao.replaceAllData(
            waves = waves,
            people = people.map { it.withNormalizedPhone() },
            logs = logs
        )
    }

    fun getLogsForPerson(personId: Int): Flow<List<InteractionLog>> {
        return networkDao.getInteractionLogsForPerson(personId)
    }

    private fun Person.withNormalizedPhone(): Person {
        return copy(normalizedPhoneNumber = PhoneNumberNormalizer.normalize(phoneNumber))
    }
}
