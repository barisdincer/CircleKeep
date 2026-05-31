package com.barisdincer.circlekeep.data

import kotlinx.coroutines.flow.Flow

class NetworkRepository(private val networkDao: NetworkDao) {

    val allContactTypes: Flow<List<ContactType>> = networkDao.getAllContactTypes()
    val activeContactTypes: Flow<List<ContactType>> = networkDao.getActiveContactTypes()
    val allWaves: Flow<List<Wave>> = networkDao.getAllWaves()
    val allPeople: Flow<List<Person>> = networkDao.getAllPeople()
    val recentInteractions: Flow<List<InteractionLog>> = networkDao.getRecentInteractions()

    suspend fun ensureDefaultContactTypes() {
        networkDao.insertContactTypesIfMissing(DefaultContactTypes.all)
    }

    suspend fun insertContactType(label: String) {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isBlank()) return
        val nextSortOrder = (networkDao.getContactTypeSnapshot().maxOfOrNull { it.sortOrder } ?: 2) + 1
        networkDao.insertContactType(
            ContactType(
                key = "CUSTOM_${System.currentTimeMillis()}_$nextSortOrder",
                label = trimmedLabel,
                isDefault = false,
                isActive = true,
                sortOrder = nextSortOrder
            )
        )
    }

    suspend fun renameContactType(key: String, label: String) {
        val trimmedLabel = label.trim()
        if (key.isBlank() || trimmedLabel.isBlank()) return
        networkDao.renameContactType(key, trimmedLabel)
    }

    suspend fun setContactTypeActive(key: String, isActive: Boolean) {
        if (key.isBlank()) return
        networkDao.setContactTypeActive(key, isActive)
    }

    suspend fun deleteContactTypeIfUnused(key: String): RepositoryActionResult {
        if (key.isBlank()) {
            return RepositoryActionResult(false, "İletişim türü bulunamadı.")
        }

        val type = networkDao.getContactTypeByKey(key)
            ?: return RepositoryActionResult(false, "İletişim türü bulunamadı.")
        val peopleCount = networkDao.countPeopleWithPreferredContactType(key)
        val logCount = networkDao.countInteractionLogsByType(key)
        if (peopleCount > 0 || logCount > 0) {
            return RepositoryActionResult(
                success = false,
                message = "Bu tür kişi tercihlerinde veya geçmişte kullanılıyor; önce kullanımı kaldırmalısın."
            )
        }

        if (type.isDefault) {
            networkDao.setContactTypeActive(key, false)
        } else {
            networkDao.deleteContactTypeByKey(key)
        }
        return RepositoryActionResult(true, "${type.label} kaldırıldı.")
    }

    suspend fun insertWave(wave: Wave) {
        val trimmedName = wave.name.trim()
        if (trimmedName.isBlank() || wave.frequencyDays <= 0) return
        networkDao.insertWave(wave.copy(name = trimmedName))
    }

    suspend fun updateWave(wave: Wave): RepositoryActionResult {
        val trimmedName = wave.name.trim()
        if (trimmedName.isBlank() || wave.frequencyDays <= 0) {
            return RepositoryActionResult(false, "Grup adı ve gün sayısı geçerli olmalı.")
        }
        networkDao.updateWave(wave.copy(name = trimmedName))
        return RepositoryActionResult(true, "$trimmedName güncellendi.")
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

    suspend fun deleteWaveIfUnused(id: Int): RepositoryActionResult {
        val peopleCount = networkDao.countPeopleInWave(id)
        if (peopleCount > 0) {
            return RepositoryActionResult(
                success = false,
                message = "Bu grupta $peopleCount kişi var; silmeden önce kişileri başka gruba taşımalısın."
            )
        }
        networkDao.deleteWaveById(id)
        return RepositoryActionResult(true, "Grup kaldırıldı.")
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

    suspend fun logInteraction(
        personId: Int,
        type: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        val log = InteractionLog(personId = personId, timestamp = timestamp, type = type, note = note.trim())
        networkDao.logInteractionAndUpdatePerson(log)
    }

    suspend fun logPreferredInteraction(personId: Int, note: String = "") {
        val person = networkDao.getPersonById(personId) ?: return
        logInteraction(personId, person.preferredContactTypeKey, note)
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

    suspend fun getContactTypeSnapshot(): List<ContactType> {
        return networkDao.getContactTypeSnapshot()
    }

    suspend fun getInteractionSnapshot(): List<InteractionLog> {
        return networkDao.getInteractionSnapshot()
    }

    suspend fun replaceAllData(
        contactTypes: List<ContactType>,
        waves: List<Wave>,
        people: List<Person>,
        logs: List<InteractionLog>
    ) {
        networkDao.replaceAllData(
            contactTypes = contactTypes.ifEmpty { DefaultContactTypes.all },
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
