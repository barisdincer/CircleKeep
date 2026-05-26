package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(tableName = "waves")
data class Wave(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val frequencyDays: Int
)

@Entity(
    tableName = "people",
    foreignKeys = [
        ForeignKey(
            entity = Wave::class,
            parentColumns = ["id"],
            childColumns = ["waveId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val waveId: Int?,
    val notes: String = "",
    val tags: String = "",
    val addedDate: Long = System.currentTimeMillis(),
    val lastInteractionDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "interaction_logs",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InteractionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String // "MEETING", "CALL" etc.
)
