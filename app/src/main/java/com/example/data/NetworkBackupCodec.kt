package com.example.data

import org.json.JSONArray
import org.json.JSONObject

object NetworkBackupCodec {
    private const val VERSION = 1

    fun encode(waves: List<Wave>, people: List<Person>, logs: List<InteractionLog>): String {
        return JSONObject()
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("waves", JSONArray(waves.map { it.toJson() }))
            .put("people", JSONArray(people.map { it.toJson() }))
            .put("interactionLogs", JSONArray(logs.map { it.toJson() }))
            .toString(2)
    }

    fun decode(json: String): NetworkBackup {
        val root = JSONObject(json)
        val waves = root.getJSONArray("waves").mapObjects { it.toWave() }
        val people = root.getJSONArray("people").mapObjects { it.toPerson() }
        val logs = root.getJSONArray("interactionLogs").mapObjects { it.toInteractionLog() }
        return NetworkBackup(waves = waves, people = people, logs = logs)
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
            type = getString("type")
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
    val waves: List<Wave>,
    val people: List<Person>,
    val logs: List<InteractionLog>
)
