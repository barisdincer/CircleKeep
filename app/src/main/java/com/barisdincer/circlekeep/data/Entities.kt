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
    tableName = "contact_types",
    indices = [Index(value = ["key"], unique = true)]
)
data class ContactType(
    @PrimaryKey val key: String,
    val label: String,
    @ColumnInfo(defaultValue = "0")
    val isDefault: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

object DefaultContactTypes {
    const val CALL = "CALL"
    const val MESSAGE = "MESSAGE"
    const val MEETING = "MEETING"

    val all = listOf(
        ContactType(key = CALL, label = "Arama", isDefault = true, sortOrder = 0),
        ContactType(key = MESSAGE, label = "Mesaj", isDefault = true, sortOrder = 1),
        ContactType(key = MEETING, label = "Buluşma", isDefault = true, sortOrder = 2)
    )
}

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
    @ColumnInfo(defaultValue = "'CALL'")
    val preferredContactTypeKey: String = DefaultContactTypes.CALL,
    val customFrequencyDays: Int? = null,
    @ColumnInfo(defaultValue = "''")
    val memoryNotes: String = "",
    @ColumnInfo(defaultValue = "''")
    val nextConversationHint: String = "",
    @ColumnInfo(defaultValue = "''")
    val importantDateLabel: String = "",
    val importantDateMillis: Long? = null,
    @ColumnInfo(defaultValue = "1")
    val reminderEnabled: Boolean = true,
    val addedDate: Long = System.currentTimeMillis(),
    val lastInteractionDate: Long = System.currentTimeMillis(),
    val lastCallLogSyncDate: Long? = null,
    val snoozedUntilDate: Long? = null
)

@Entity(
    tableName = "person_contact_rhythms",
    primaryKeys = ["personId", "contactTypeKey"],
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
        Index("contactTypeKey")
    ]
)
data class PersonContactRhythm(
    val personId: Int,
    val contactTypeKey: String,
    @ColumnInfo(defaultValue = "1")
    val isActive: Boolean = true,
    val customFrequencyDays: Int? = null,
    val lastInteractionDate: Long? = null,
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
    val type: String,
    @ColumnInfo(defaultValue = "''")
    val note: String = ""
)
