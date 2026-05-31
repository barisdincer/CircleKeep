package com.barisdincer.circlekeep.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

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
    ],
    indices = [
        Index("waveId"),
        Index("normalizedPhoneNumber"),
        Index("contactLookupKey")
    ]
)
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    @ColumnInfo(defaultValue = "")
    val normalizedPhoneNumber: String = PhoneNumberNormalizer.normalize(phoneNumber),
    val contactLookupKey: String? = null,
    val waveId: Int?,
    val notes: String = "",
    val tags: String = "",
    @ColumnInfo(defaultValue = "1")
    val reminderEnabled: Boolean = true,
    val addedDate: Long = System.currentTimeMillis(),
    val lastInteractionDate: Long = System.currentTimeMillis(),
    val lastCallLogSyncDate: Long? = null,
    val snoozedUntilDate: Long? = null
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
    ],
    indices = [
        Index("personId"),
        Index(value = ["personId", "timestamp", "type"], unique = true)
    ]
)
data class InteractionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String // "MEETING", "CALL" etc.
)
