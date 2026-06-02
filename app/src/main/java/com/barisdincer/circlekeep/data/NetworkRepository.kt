package com.barisdincer.circlekeep.data

import kotlinx.coroutines.flow.Flow

class NetworkRepository(private val networkDao: NetworkDao) {

    val allContactTypes: Flow<List<ContactType>> = networkDao.getAllContactTypes()
    val activeContactTypes: Flow<List<ContactType>> = networkDao.getActiveContactTypes()
    val allWaves: Flow<List<Wave>> = networkDao.getAllWaves()
    val allPeople: Flow<List<Person>> = networkDao.getAllPeople()
    val allPersonContactRhythms: Flow<List<PersonContactRhythm>> = networkDao.getAllPersonContactRhythms()
    val allInteractions: Flow<List<InteractionLog>> = networkDao.getAllInteractionLogs()
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
        val rhythmCount = networkDao.countPersonContactRhythmsByType(key)
        val logCount = networkDao.countInteractionLogsByType(key)
        if (peopleCount > 0 || rhythmCount > 0 || logCount > 0) {
            return RepositoryActionResult(
                success = false,
                message = "Bu tür kişi tercihlerinde veya geçmişte kullanılıyor; önce kullanımı kaldırmalısın."
            )
        }

        if (type.isDefault) {
            networkDao.setContactTypeActive(key, false)
        } else {
            networkDao.deleteInactivePersonContactRhythmsByType(key)
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

    suspend fun insertPerson(person: Person): Long {
        val id = networkDao.insertPerson(person.withNormalizedPhone()).toInt()
        if (id > 0) {
            networkDao.insertPersonContactRhythmIfMissing(id, person.preferredContactTypeKey)
        }
        return id.toLong()
    }

    suspend fun insertPerson(
        person: Person,
        initialInteractionType: String?,
        initialInteractionTimestamp: Long?,
        initialInteractionNote: String = ""
    ): Long {
        val personId = insertPerson(
            person.withNormalizedPhone().copy(
                lastInteractionDate = initialInteractionTimestamp ?: person.lastInteractionDate
            )
        ).toInt()
        if (personId > 0 && initialInteractionType != null && initialInteractionTimestamp != null) {
            logInteraction(
                personId = personId,
                type = initialInteractionType,
                note = initialInteractionNote,
                timestamp = initialInteractionTimestamp
            )
        }
        return personId.toLong()
    }

    suspend fun insertPeople(people: List<Person>) {
        if (people.isEmpty()) return
        networkDao.insertPeople(people.map { it.withNormalizedPhone() })
    }

    suspend fun updatePerson(person: Person) {
        networkDao.updatePerson(person.withNormalizedPhone())
        if (person.id > 0) {
            networkDao.insertPersonContactRhythmIfMissing(person.id, person.preferredContactTypeKey)
            networkDao.updatePersonContactRhythmsCustomFrequency(person.id, person.customFrequencyDays?.takeIf { it > 0 })
        }
    }

    suspend fun deletePerson(id: Int): RepositoryActionResult {
        val person = networkDao.getPersonById(id)
            ?: return RepositoryActionResult(false, "Kişi bulunamadı.")
        networkDao.deletePersonById(id)
        return RepositoryActionResult(true, "${person.name} silindi.")
    }

    suspend fun updatePersonWave(personId: Int, waveId: Int?) {
        networkDao.updatePersonWave(personId, waveId)
    }

    suspend fun updatePeopleWave(personIds: List<Int>, waveId: Int?): RepositoryActionResult {
        val distinctIds = personIds.distinct().filter { it > 0 }
        if (distinctIds.isEmpty()) {
            return RepositoryActionResult(false, "Kişi seçilmedi.")
        }
        networkDao.updatePeopleWave(distinctIds, waveId)
        return RepositoryActionResult(true, "${distinctIds.size} kişi gruba eklendi.")
    }

    suspend fun setPersonContactRhythmActive(
        personId: Int,
        contactTypeKey: String,
        isActive: Boolean
    ): RepositoryActionResult {
        val person = networkDao.getPersonById(personId)
            ?: return RepositoryActionResult(false, "Kişi bulunamadı.")
        if (contactTypeKey.isBlank()) {
            return RepositoryActionResult(false, "İletişim türü bulunamadı.")
        }
        networkDao.insertPersonContactRhythmIfMissing(personId, contactTypeKey)
        networkDao.updatePersonContactRhythmActive(personId, contactTypeKey, isActive)
        if (isActive) {
            networkDao.updatePreferredContactType(personId, contactTypeKey)
        }
        return RepositoryActionResult(
            success = true,
            message = if (isActive) "İletişim türü takibe alındı." else "İletişim türü takipten çıkarıldı."
        )
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

    suspend fun logInteractions(
        personIds: List<Int>,
        type: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        val logs = personIds.distinct().filter { it > 0 }.map { personId ->
            InteractionLog(personId = personId, timestamp = timestamp, type = type, note = note.trim())
        }
        if (logs.isEmpty()) return
        networkDao.logInteractionsAndUpdatePeople(logs)
    }

    suspend fun updateInteractionLog(log: InteractionLog): RepositoryActionResult {
        val existing = networkDao.getInteractionLogById(log.id)
            ?: return RepositoryActionResult(false, "Temas kaydı bulunamadı.")
        val updated = log.copy(note = log.note.trim())
        networkDao.updateInteractionLog(updated)
        refreshLastInteraction(existing.personId)
        refreshPersonContactRhythm(existing.personId, existing.type)
        refreshPersonContactRhythm(updated.personId, updated.type)
        networkDao.updatePersonContactRhythmActive(updated.personId, updated.type, true)
        if (existing.personId != updated.personId) {
            refreshLastInteraction(updated.personId)
        }
        return RepositoryActionResult(true, "Temas kaydı güncellendi.")
    }

    suspend fun deleteInteractionLog(id: Int): RepositoryActionResult {
        val existing = networkDao.getInteractionLogById(id)
            ?: return RepositoryActionResult(false, "Temas kaydı bulunamadı.")
        networkDao.deleteInteractionLogById(id)
        refreshLastInteraction(existing.personId)
        refreshPersonContactRhythm(existing.personId, existing.type)
        return RepositoryActionResult(true, "Temas kaydı silindi.")
    }

    suspend fun logPreferredInteraction(personId: Int, note: String = "") {
        val person = networkDao.getPersonById(personId) ?: return
        logInteraction(personId, person.preferredContactTypeKey, note)
    }

    suspend fun snoozePerson(personId: Int, untilDate: Long) {
        networkDao.updateSnoozedUntil(personId, untilDate)
    }

    suspend fun snoozePersonContactType(personId: Int, type: String, untilDate: Long) {
        networkDao.insertPersonContactRhythmIfMissing(personId, type)
        networkDao.updatePersonContactRhythmSnooze(personId, type, untilDate)
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

    suspend fun getPersonContactRhythmSnapshot(): List<PersonContactRhythm> {
        return networkDao.getPersonContactRhythmSnapshot()
    }

    suspend fun replaceAllData(
        contactTypes: List<ContactType>,
        waves: List<Wave>,
        people: List<Person>,
        rhythms: List<PersonContactRhythm>,
        logs: List<InteractionLog>
    ) {
        networkDao.replaceAllData(
            contactTypes = contactTypes.ifEmpty { DefaultContactTypes.all },
            waves = waves,
            people = people.map { it.withNormalizedPhone() },
            rhythms = rhythms,
            logs = logs
        )
        if (rhythms.isEmpty()) {
            networkDao.getPeopleSnapshot().forEach { person ->
                networkDao.insertPersonContactRhythmIfMissing(person.id, person.preferredContactTypeKey)
            }
        }
    }

    fun getLogsForPerson(personId: Int): Flow<List<InteractionLog>> {
        return networkDao.getInteractionLogsForPerson(personId)
    }

    private suspend fun refreshLastInteraction(personId: Int) {
        val fallback = networkDao.getPersonById(personId)?.addedDate ?: return
        val latest = networkDao.getLatestInteractionTimestampForPerson(personId) ?: fallback
        networkDao.updateLastInteraction(personId, latest)
    }

    private suspend fun refreshPersonContactRhythm(personId: Int, type: String) {
        val person = networkDao.getPersonById(personId) ?: return
        networkDao.insertPersonContactRhythmIfMissing(personId, type)
        val latest = networkDao.getLatestInteractionTimestampForPersonAndType(personId, type)
            ?: person.addedDate
        networkDao.updatePersonContactRhythmLastInteraction(personId, type, latest)
    }

    private fun Person.withNormalizedPhone(): Person {
        return copy(normalizedPhoneNumber = PhoneNumberNormalizer.normalize(phoneNumber))
    }
}
