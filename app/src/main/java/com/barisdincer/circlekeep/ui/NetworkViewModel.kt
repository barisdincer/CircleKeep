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

    val contactTypes: StateFlow<List<ContactType>> = repository.allContactTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeContactTypes: StateFlow<List<ContactType>> = repository.activeContactTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultContactTypes.all)

    val waves: StateFlow<List<Wave>> = repository.allWaves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val people: StateFlow<List<Person>> = repository.allPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val interactions: StateFlow<List<InteractionLog>> = repository.recentInteractions
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
        repository.allContactTypes
    ) { p, w, types ->
        ContactReminderCalculator.nextContacts(p, w).map { dueContact ->
            PersonWithWave(
                person = dueContact.person,
                wave = dueContact.wave,
                contactType = contactTypeFor(dueContact.person, types),
                effectiveFrequencyDays = dueContact.effectiveFrequencyDays,
                isDue = true,
                daysSinceLastInteraction = dueContact.daysSinceLastInteraction,
                daysOverdue = dueContact.daysOverdue
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardReminders: StateFlow<ReminderDashboardUiState> = combine(
        repository.allPeople,
        repository.allWaves,
        repository.allContactTypes
    ) { people, waves, types ->
        val due = ContactReminderCalculator.dueContacts(people, waves).map { dueContact ->
            PersonWithWave(
                person = dueContact.person,
                wave = dueContact.wave,
                contactType = contactTypeFor(dueContact.person, types),
                effectiveFrequencyDays = dueContact.effectiveFrequencyDays,
                isDue = true,
                daysSinceLastInteraction = dueContact.daysSinceLastInteraction,
                daysOverdue = dueContact.daysOverdue
            )
        }
        val upcoming = ContactReminderCalculator.nextContacts(people, waves, limit = 10).map { dueContact ->
            PersonWithWave(
                person = dueContact.person,
                wave = dueContact.wave,
                contactType = contactTypeFor(dueContact.person, types),
                effectiveFrequencyDays = dueContact.effectiveFrequencyDays,
                isDue = false,
                daysSinceLastInteraction = dueContact.daysSinceLastInteraction,
                daysOverdue = dueContact.daysOverdue
            )
        }
        val snoozed = people.mapNotNull { person ->
            val snoozedUntil = person.snoozedUntilDate ?: return@mapNotNull null
            if (snoozedUntil <= System.currentTimeMillis()) return@mapNotNull null
            val wave = waves.find { it.id == person.waveId } ?: return@mapNotNull null
            PersonWithWave(
                person = person,
                wave = wave,
                contactType = contactTypeFor(person, types),
                effectiveFrequencyDays = person.customFrequencyDays?.takeIf { it > 0 } ?: wave.frequencyDays,
                isDue = false,
                daysSinceLastInteraction = 0,
                daysOverdue = 0,
                snoozedUntilDate = snoozedUntil
            )
        }.sortedBy { it.snoozedUntilDate }

        ReminderDashboardUiState(
            today = due.filter { it.daysOverdue == 0L },
            overdue = due.filter { it.daysOverdue > 0L },
            upcoming = upcoming,
            snoozed = snoozed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderDashboardUiState())

    fun addContactType(label: String) {
        viewModelScope.launch {
            repository.insertContactType(label)
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
            repository.insertWave(Wave(name = name.trim(), frequencyDays = frequencyDays))
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

    fun addPerson(name: String, phoneNumber: String, waveId: Int?, contactLookupKey: String? = null) {
        viewModelScope.launch {
            repository.insertPerson(
                Person(
                    name = name,
                    phoneNumber = phoneNumber,
                    contactLookupKey = contactLookupKey,
                    waveId = waveId
                )
            )
        }
    }

    fun addPeople(people: List<Person>) {
        viewModelScope.launch {
            repository.insertPeople(people)
        }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            repository.updatePerson(person)
        }
    }

    fun logInteraction(personId: Int, type: String, note: String = "") {
        viewModelScope.launch {
            repository.logInteraction(personId, type, note)
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
}

private fun contactTypeFor(person: Person, types: List<ContactType>): ContactType {
    return types.find { it.key == person.preferredContactTypeKey }
        ?: DefaultContactTypes.all.first { it.key == DefaultContactTypes.CALL }
}

data class PersonWithWave(
    val person: Person,
    val wave: Wave?,
    val contactType: ContactType = DefaultContactTypes.all.first { it.key == DefaultContactTypes.CALL },
    val effectiveFrequencyDays: Int = wave?.frequencyDays ?: 0,
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
