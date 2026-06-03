package com.barisdincer.circlekeep.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.barisdincer.circlekeep.data.ContactType
import com.barisdincer.circlekeep.data.ContactReminderCalculator
import com.barisdincer.circlekeep.data.DefaultContactTypes
import com.barisdincer.circlekeep.data.InteractionLog
import com.barisdincer.circlekeep.data.NetworkRepository
import com.barisdincer.circlekeep.data.NetworkBackupCodec
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.PersonContactRhythm
import com.barisdincer.circlekeep.data.Wave
import com.barisdincer.circlekeep.device.CallLogSyncManager
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkViewModel(private val repository: NetworkRepository) : ViewModel() {
    private val _syncState = MutableStateFlow(SyncUiState())
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    private val _managementMessage = MutableStateFlow<String?>(null)
    val managementMessage: StateFlow<String?> = _managementMessage.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    val contactTypes: StateFlow<List<ContactType>> = repository.allContactTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeContactTypes: StateFlow<List<ContactType>> = repository.activeContactTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultContactTypes.all)

    val waves: StateFlow<List<Wave>> = repository.allWaves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val people: StateFlow<List<Person>> = repository.allPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personContactRhythms: StateFlow<List<PersonContactRhythm>> = repository.allPersonContactRhythms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val interactions: StateFlow<List<InteractionLog>> = repository.recentInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInteractions: StateFlow<List<InteractionLog>> = repository.allInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueTags: StateFlow<List<String>> = repository.allPeople.map { people ->
        people.flatMap { it.tags.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingContacts: StateFlow<List<PersonWithWave>> = combine(
        repository.allPeople,
        repository.allWaves,
        repository.allPersonContactRhythms,
        repository.allContactTypes
    ) { p, w, rhythms, types ->
        ContactReminderCalculator.nextContacts(p, w, rhythms).map { dueContact ->
            PersonWithWave(
                person = dueContact.person,
                wave = dueContact.wave,
                contactType = contactTypeFor(dueContact.contactTypeKey, types),
                effectiveFrequencyDays = dueContact.effectiveFrequencyDays,
                lastInteractionDate = dueContact.lastInteractionDate,
                isDue = false,
                daysSinceLastInteraction = dueContact.daysSinceLastInteraction,
                daysOverdue = dueContact.daysOverdue
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardReminders: StateFlow<ReminderDashboardUiState> = combine(
        repository.allPeople,
        repository.allWaves,
        repository.allPersonContactRhythms,
        repository.allContactTypes
    ) { people, waves, rhythms, types ->
        val due = ContactReminderCalculator.dueContacts(people, waves, rhythms).map { dueContact ->
            PersonWithWave(
                person = dueContact.person,
                wave = dueContact.wave,
                contactType = contactTypeFor(dueContact.contactTypeKey, types),
                effectiveFrequencyDays = dueContact.effectiveFrequencyDays,
                lastInteractionDate = dueContact.lastInteractionDate,
                isDue = true,
                daysSinceLastInteraction = dueContact.daysSinceLastInteraction,
                daysOverdue = dueContact.daysOverdue
            )
        }
        val upcoming = ContactReminderCalculator.nextContacts(people, waves, rhythms, limit = 10).map { dueContact ->
            PersonWithWave(
                person = dueContact.person,
                wave = dueContact.wave,
                contactType = contactTypeFor(dueContact.contactTypeKey, types),
                effectiveFrequencyDays = dueContact.effectiveFrequencyDays,
                lastInteractionDate = dueContact.lastInteractionDate,
                isDue = false,
                daysSinceLastInteraction = dueContact.daysSinceLastInteraction,
                daysOverdue = dueContact.daysOverdue
            )
        }
        val snoozed = ContactReminderCalculator.snoozedContacts(people, waves, rhythms).map { dueContact ->
            PersonWithWave(
                person = dueContact.person,
                wave = dueContact.wave,
                contactType = contactTypeFor(dueContact.contactTypeKey, types),
                effectiveFrequencyDays = dueContact.effectiveFrequencyDays,
                lastInteractionDate = dueContact.lastInteractionDate,
                isDue = false,
                daysSinceLastInteraction = dueContact.daysSinceLastInteraction,
                daysOverdue = 0,
                snoozedUntilDate = dueContact.snoozedUntilDate
            )
        }

        ReminderDashboardUiState(
            today = due.filter { it.daysOverdue == 0L },
            overdue = due.filter { it.daysOverdue > 0L },
            upcoming = upcoming,
            snoozed = snoozed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderDashboardUiState())

    fun addContactType(label: String) {
        viewModelScope.launch {
            val trimmedLabel = label.trim()
            if (trimmedLabel.isBlank()) {
                _managementMessage.value = "İletişim türü adı boş olamaz."
                return@launch
            }
            repository.insertContactType(trimmedLabel)
            _managementMessage.value = "$trimmedLabel eklendi."
        }
    }

    fun renameContactType(key: String, label: String) {
        viewModelScope.launch {
            repository.renameContactType(key, label)
        }
    }

    fun setContactTypeActive(key: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.setContactTypeActive(key, isActive)
        }
    }

    fun deleteContactType(key: String) {
        viewModelScope.launch {
            _managementMessage.value = repository.deleteContactTypeIfUnused(key).message
        }
    }

    fun addWave(name: String, frequencyDays: Int) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank() || frequencyDays <= 0) {
                _managementMessage.value = "Grup adı ve gün sayısı geçerli olmalı."
                return@launch
            }
            repository.insertWave(Wave(name = trimmedName, frequencyDays = frequencyDays))
            _managementMessage.value = "$trimmedName eklendi."
        }
    }

    fun updateWave(wave: Wave) {
        viewModelScope.launch {
            _managementMessage.value = repository.updateWave(wave).message
        }
    }

    fun deleteWave(id: Int) {
        viewModelScope.launch {
            _managementMessage.value = repository.deleteWaveIfUnused(id).message
        }
    }

    fun clearManagementMessage() {
        _managementMessage.value = null
    }

    fun addPerson(
        name: String,
        phoneNumber: String,
        waveId: Int?,
        contactLookupKey: String? = null,
        initialInteractionType: String? = null,
        initialInteractionTimestamp: Long? = null,
        initialInteractionNote: String = "",
        customFrequencyDays: Int? = null
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                _uiMessage.value = "Kişi adı boş olamaz."
                return@launch
            }
            repository.insertPerson(
                person = Person(
                    name = trimmedName,
                    phoneNumber = phoneNumber,
                    contactLookupKey = contactLookupKey,
                    waveId = waveId,
                    preferredContactTypeKey = initialInteractionType ?: DefaultContactTypes.CALL,
                    customFrequencyDays = customFrequencyDays?.takeIf { it > 0 }
                ),
                initialInteractionType = initialInteractionType,
                initialInteractionTimestamp = initialInteractionTimestamp,
                initialInteractionNote = initialInteractionNote
            )
            _uiMessage.value = "$trimmedName kaydedildi."
        }
    }

    fun addPeople(people: List<Person>) {
        viewModelScope.launch {
            repository.insertPeople(people)
            _uiMessage.value = "${people.size} kişi içe aktarıldı."
        }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            repository.updatePerson(person)
            _uiMessage.value = "${person.name} güncellendi."
        }
    }

    fun setPersonContactRhythmActive(personId: Int, contactTypeKey: String, isActive: Boolean) {
        viewModelScope.launch {
            _uiMessage.value = repository.setPersonContactRhythmActive(personId, contactTypeKey, isActive).message
        }
    }

    fun deletePerson(id: Int) {
        viewModelScope.launch {
            _uiMessage.value = repository.deletePerson(id).message
        }
    }

    fun movePersonToWave(personId: Int, waveId: Int?) {
        viewModelScope.launch {
            repository.updatePersonWave(personId, waveId)
            _uiMessage.value = "Kişi gruba eklendi."
        }
    }

    fun movePeopleToWave(personIds: List<Int>, waveId: Int?) {
        viewModelScope.launch {
            _uiMessage.value = repository.updatePeopleWave(personIds, waveId).message
        }
    }

    fun logInteraction(
        personId: Int,
        type: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.logInteraction(personId, type, note, timestamp)
            _uiMessage.value = "Temas kaydedildi."
        }
    }

    fun logInteractions(
        personIds: List<Int>,
        type: String,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.logInteractions(personIds, type, note, timestamp)
            _uiMessage.value = "${personIds.distinct().size} kişi için temas kaydedildi."
        }
    }

    fun updateInteractionLog(log: InteractionLog) {
        viewModelScope.launch {
            _uiMessage.value = repository.updateInteractionLog(log).message
        }
    }

    fun updateInteractionLogs(logs: List<InteractionLog>) {
        viewModelScope.launch {
            _uiMessage.value = repository.updateInteractionLogs(logs).message
        }
    }

    fun replaceInteractionEvent(
        existingLogIds: List<Int>,
        personIds: List<Int>,
        type: String,
        note: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            _uiMessage.value = repository.replaceInteractionEvent(
                existingLogIds = existingLogIds,
                personIds = personIds,
                type = type,
                note = note,
                timestamp = timestamp
            ).message
        }
    }

    fun deleteInteractionLog(id: Int) {
        viewModelScope.launch {
            _uiMessage.value = repository.deleteInteractionLog(id).message
        }
    }

    fun deleteInteractionLogs(ids: List<Int>) {
        viewModelScope.launch {
            _uiMessage.value = repository.deleteInteractionLogs(ids).message
        }
    }

    fun snoozePersonForDays(personId: Int, days: Int) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, days)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            repository.snoozePerson(personId, calendar.timeInMillis)
            _uiMessage.value = "Yarın tekrar hatırlatılacak."
        }
    }

    fun snoozePersonUntil(personId: Int, untilDate: Long) {
        viewModelScope.launch {
            repository.snoozePerson(personId, untilDate)
            _uiMessage.value = "Hatırlatma ertelendi."
        }
    }

    fun snoozeContactTypeUntil(personId: Int, contactTypeKey: String, untilDate: Long) {
        viewModelScope.launch {
            repository.snoozePersonContactType(personId, contactTypeKey, untilDate)
            _uiMessage.value = "Hatırlatma ertelendi."
        }
    }

    fun syncCallLog(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = _syncState.value.copy(isSyncing = true, message = "Arama kayıtları kontrol ediliyor...")
            val result = CallLogSyncManager.sync(context.applicationContext, repository)
            _syncState.value = SyncUiState(
                isSyncing = false,
                permissionGranted = result.permissionGranted,
                matchedCalls = result.matchedCalls,
                lastSyncedAt = System.currentTimeMillis(),
                message = if (result.permissionGranted) {
                    "${result.matchedCalls} yeni arama eşleşti."
                } else {
                    "Arama kaydı izni gerekiyor."
                }
            )
        }
    }

    fun createBackupJson(onBackupReady: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                NetworkBackupCodec.encode(
                    waves = repository.getWaveSnapshot(),
                    contactTypes = repository.getContactTypeSnapshot(),
                    people = repository.getPeopleSnapshot(),
                    rhythms = repository.getPersonContactRhythmSnapshot(),
                    logs = repository.getInteractionSnapshot()
                )
            }.onSuccess { json ->
                _backupState.value = BackupUiState(message = "Yedek hazır.")
                onBackupReady(json)
            }.onFailure {
                _backupState.value = BackupUiState(message = "Yedek alınamadı: ${it.message}")
            }
        }
    }

    fun restoreBackupJson(json: String) {
        viewModelScope.launch {
            runCatching {
                val backup = NetworkBackupCodec.decode(json)
                repository.replaceAllData(
                    contactTypes = backup.contactTypes,
                    waves = backup.waves,
                    people = backup.people,
                    rhythms = backup.rhythms,
                    logs = backup.logs
                )
                backup
            }.onSuccess { backup ->
                _backupState.value = BackupUiState(
                    message = "${backup.people.size} kişi ve ${backup.logs.size} temas geri yüklendi."
                )
            }.onFailure {
                _backupState.value = BackupUiState(message = "Geri yükleme başarısız: ${it.message}")
            }
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }
}

private fun contactTypeFor(contactTypeKey: String, types: List<ContactType>): ContactType {
    return types.find { it.key == contactTypeKey }
        ?: DefaultContactTypes.all.first { it.key == DefaultContactTypes.CALL }
}

data class PersonWithWave(
    val person: Person,
    val wave: Wave?,
    val contactType: ContactType = DefaultContactTypes.all.first { it.key == DefaultContactTypes.CALL },
    val effectiveFrequencyDays: Int = wave?.frequencyDays ?: 0,
    val lastInteractionDate: Long = person.lastInteractionDate,
    val isDue: Boolean,
    val daysSinceLastInteraction: Long = 0,
    val daysOverdue: Long = 0,
    val snoozedUntilDate: Long? = null
)

data class ReminderDashboardUiState(
    val today: List<PersonWithWave> = emptyList(),
    val overdue: List<PersonWithWave> = emptyList(),
    val upcoming: List<PersonWithWave> = emptyList(),
    val snoozed: List<PersonWithWave> = emptyList()
)

data class SyncUiState(
    val isSyncing: Boolean = false,
    val permissionGranted: Boolean = true,
    val matchedCalls: Int = 0,
    val lastSyncedAt: Long? = null,
    val message: String? = null
)

data class BackupUiState(
    val message: String? = null
)

class NetworkViewModelFactory(private val repository: NetworkRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NetworkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NetworkViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
