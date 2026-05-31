package com.barisdincer.circlekeep.data

import org.json.JSONArray
import org.json.JSONObject

object NetworkBackupCodec {
    private const val VERSION = 2

    fun encode(
        contactTypes: List<ContactType>,
        waves: List<Wave>,
        people: List<Person>,
        logs: List<InteractionLog>
    ): String {
        return JSONObject()
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("contactTypes", JSONArray(contactTypes.map { it.toJson() }))
            .put("waves", JSONArray(waves.map { it.toJson() }))
            .put("people", JSONArray(people.map { it.toJson() }))
            .put("interactionLogs", JSONArray(logs.map { it.toJson() }))
            .toString(2)
    }

    fun decode(json: String): NetworkBackup {
        val root = JSONObject(json)
        val contactTypes = root.optJSONArray("contactTypes")?.mapObjects { it.toContactType() }
            ?: DefaultContactTypes.all
        val waves = root.getJSONArray("waves").mapObjects { it.toWave() }
        val people = root.getJSONArray("people").mapObjects { it.toPerson() }
        val logs = root.getJSONArray("interactionLogs").mapObjects { it.toInteractionLog() }
        return NetworkBackup(contactTypes = contactTypes, waves = waves, people = people, logs = logs)
    }

    private fun ContactType.toJson(): JSONObject {
        return JSONObject()
            .put("key", key)
            .put("label", label)
            .put("isDefault", isDefault)
            .put("isActive", isActive)
            .put("sortOrder", sortOrder)
    }

    private fun Wave.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("frequencyDays", frequencyDays)
    }

    private fun Person.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("phoneNumber", phoneNumber)
            .put("normalizedPhoneNumber", normalizedPhoneNumber)
            .putNullable("contactLookupKey", contactLookupKey)
            .putNullable("waveId", waveId)
            .put("notes", notes)
            .put("tags", tags)
            .put("preferredContactTypeKey", preferredContactTypeKey)
            .putNullable("customFrequencyDays", customFrequencyDays)
            .put("memoryNotes", memoryNotes)
            .put("nextConversationHint", nextConversationHint)
            .put("importantDateLabel", importantDateLabel)
            .putNullable("importantDateMillis", importantDateMillis)
            .put("reminderEnabled", reminderEnabled)
            .put("addedDate", addedDate)
            .put("lastInteractionDate", lastInteractionDate)
            .putNullable("lastCallLogSyncDate", lastCallLogSyncDate)
            .putNullable("snoozedUntilDate", snoozedUntilDate)
    }

    private fun InteractionLog.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("personId", personId)
            .put("timestamp", timestamp)
            .put("type", type)
            .put("note", note)
    }

    private fun JSONObject.toContactType(): ContactType {
        return ContactType(
            key = getString("key"),
            label = getString("label"),
            isDefault = optBoolean("isDefault", false),
            isActive = optBoolean("isActive", true),
            sortOrder = optInt("sortOrder", 0)
        )
    }

    private fun JSONObject.toWave(): Wave {
        return Wave(
            id = getInt("id"),
            name = getString("name"),
            frequencyDays = getInt("frequencyDays")
        )
    }

    private fun JSONObject.toPerson(): Person {
        val phoneNumber = getString("phoneNumber")
        return Person(
            id = getInt("id"),
            name = getString("name"),
            phoneNumber = phoneNumber,
            normalizedPhoneNumber = optString("normalizedPhoneNumber")
                .ifBlank { PhoneNumberNormalizer.normalize(phoneNumber) },
            contactLookupKey = optNullableString("contactLookupKey"),
            waveId = optNullableInt("waveId"),
            notes = optString("notes"),
            tags = optString("tags"),
            preferredContactTypeKey = optString("preferredContactTypeKey", DefaultContactTypes.CALL),
            customFrequencyDays = optNullableInt("customFrequencyDays"),
            memoryNotes = optString("memoryNotes"),
            nextConversationHint = optString("nextConversationHint"),
            importantDateLabel = optString("importantDateLabel"),
            importantDateMillis = optNullableLong("importantDateMillis"),
            reminderEnabled = optBoolean("reminderEnabled", true),
            addedDate = optLong("addedDate", System.currentTimeMillis()),
            lastInteractionDate = optLong("lastInteractionDate", System.currentTimeMillis()),
            lastCallLogSyncDate = optNullableLong("lastCallLogSyncDate"),
            snoozedUntilDate = optNullableLong("snoozedUntilDate")
        )
    }

    private fun JSONObject.toInteractionLog(): InteractionLog {
        return InteractionLog(
            id = getInt("id"),
            personId = getInt("personId"),
            timestamp = getLong("timestamp"),
            type = getString("type"),
            note = optString("note")
        )
    }

    private fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> {
        return (0 until length()).map { index -> mapper(getJSONObject(index)) }
    }

    private fun JSONObject.putNullable(name: String, value: Any?): JSONObject {
        return put(name, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optNullableString(name: String): String? {
        return if (isNull(name)) null else optString(name)
    }

    private fun JSONObject.optNullableInt(name: String): Int? {
        return if (isNull(name)) null else optInt(name)
    }

    private fun JSONObject.optNullableLong(name: String): Long? {
        return if (isNull(name)) null else optLong(name)
    }
}

data class NetworkBackup(
    val contactTypes: List<ContactType>,
    val waves: List<Wave>,
    val people: List<Person>,
    val logs: List<InteractionLog>
)
