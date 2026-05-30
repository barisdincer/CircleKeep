package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ContactReminderCalculator
import com.example.data.InteractionLog
import com.example.data.NetworkRepository
import com.example.data.NetworkBackupCodec
import com.example.data.Person
import com.example.data.Wave
import com.example.device.CallLogSyncManager
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

    // Combine people and waves to determine who needs to be contacted.
    val upcomingContacts: StateFlow<List<PersonWithWave>> = combine(repository.allPeople, repository.allWaves) { p, w ->
        ContactReminderCalculator.dueContacts(p, w).map { dueContact ->
            PersonWithWave(
                person = dueContact.person,
                wave = dueContact.wave,
                isDue = true,
                daysSinceLastInteraction = dueContact.daysSinceLastInteraction,
                daysOverdue = dueContact.daysOverdue
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWave(name: String, frequencyDays: Int) {
        viewModelScope.launch {
            repository.insertWave(Wave(name = name, frequencyDays = frequencyDays))
        }
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

    fun logInteraction(personId: Int, type: String) {
        viewModelScope.launch {
            repository.logInteraction(personId, type)
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

data class PersonWithWave(
    val person: Person,
    val wave: Wave?,
    val isDue: Boolean,
    val daysSinceLastInteraction: Long = 0,
    val daysOverdue: Long = 0
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
