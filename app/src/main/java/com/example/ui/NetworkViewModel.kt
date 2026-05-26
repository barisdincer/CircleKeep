package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.InteractionLog
import com.example.data.NetworkRepository
import com.example.data.Person
import com.example.data.Wave
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetworkViewModel(private val repository: NetworkRepository) : ViewModel() {

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

    // Combine people and waves to determine who needs to be contacted
    val upcomingContacts: StateFlow<List<PersonWithWave>> = combine(repository.allPeople, repository.allWaves) { p, w ->
        val currentTime = System.currentTimeMillis()
        val onedayMillis = 24L * 60 * 60 * 1000
        
        p.map { person ->
            val wave = w.find { it.id == person.waveId }
            val isDue = if (wave != null) {
                val daysSince = (currentTime - person.lastInteractionDate) / onedayMillis
                daysSince >= wave.frequencyDays
            } else {
                false
            }
            PersonWithWave(person, wave, isDue)
        }.filter { it.isDue }.sortedBy { it.person.lastInteractionDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWave(name: String, frequencyDays: Int) {
        viewModelScope.launch {
            repository.insertWave(Wave(name = name, frequencyDays = frequencyDays))
        }
    }

    fun addPerson(name: String, phoneNumber: String, waveId: Int?) {
        viewModelScope.launch {
            repository.insertPerson(Person(name = name, phoneNumber = phoneNumber, waveId = waveId))
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
}

data class PersonWithWave(val person: Person, val wave: Wave?, val isDue: Boolean)

class NetworkViewModelFactory(private val repository: NetworkRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NetworkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NetworkViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
