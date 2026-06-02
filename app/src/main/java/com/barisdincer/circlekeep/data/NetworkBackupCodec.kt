package com.barisdincer.circlekeep.data

import org.json.JSONArray
import org.json.JSONObject

object NetworkBackupCodec {
    private const val VERSION = 3

    fun encode(
        contactTypes: List<ContactType>,
        waves: List<Wave>,
        people: List<Person>,
        rhythms: List<PersonContactRhythm> = emptyList(),
        logs: List<InteractionLog>
    ): String {
        return JSONObject()
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("contactTypes", JSONArray(contactTypes.map { it.toJson() }))
            .put("waves", JSONArray(waves.map { it.toJson() }))
            .put("people", JSONArray(people.map { it.toJson() }))
            .put("personContactRhythms", JSONArray(rhythms.map { it.toJson() }))
            .put("interactionLogs", JSONArray(logs.map { it.toJson() }))
            .toString(2)
    }

    fun decode(json: String): NetworkBackup {
        val root = JSONObject(json)
        val contactTypes = root.optJSONArray("contactTypes")?.mapObjects { it.toContactType() }
            ?: DefaultContactTypes.all
        val waves = root.getJSONArray("waves").mapObjects { it.toWave() }
        val logs = root.getJSONArray("interactionLogs").mapObjects { it.toInteractionLog() }
        val people = root.getJSONArray("people").mapObjects { it.toPerson() }
        val rhythms = root.optJSONArray("personContactRhythms")?.mapObjects { it.toPersonContactRhythm() }
            ?: fallbackRhythmsFromLegacyBackup(people, logs)
        return NetworkBackup(contactTypes = contactTypes, waves = waves, people = people, rhythms = rhythms, logs = logs)
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

    private fun PersonContactRhythm.toJson(): JSONObject {
        return JSONObject()
            .put("personId", personId)
            .put("contactTypeKey", contactTypeKey)
            .put("isActive", isActive)
            .putNullable("customFrequencyDays", customFrequencyDays)
            .putNullable("lastInteractionDate", lastInteractionDate)
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

    private fun JSONObject.toPersonContactRhythm(): PersonContactRhythm {
        return PersonContactRhythm(
            personId = getInt("personId"),
            contactTypeKey = getString("contactTypeKey"),
            isActive = optBoolean("isActive", true),
            customFrequencyDays = optNullableInt("customFrequencyDays"),
            lastInteractionDate = optNullableLong("lastInteractionDate"),
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

    private fun fallbackRhythmsFromLegacyBackup(
        people: List<Person>,
        logs: List<InteractionLog>
    ): List<PersonContactRhythm> {
        val peopleById = people.associateBy { it.id }
        val rhythmsByKey = linkedMapOf<Pair<Int, String>, PersonContactRhythm>()

        people.forEach { person ->
            rhythmsByKey[person.id to person.preferredContactTypeKey] = PersonContactRhythm(
                personId = person.id,
                contactTypeKey = person.preferredContactTypeKey,
                isActive = true,
                customFrequencyDays = person.customFrequencyDays,
                lastInteractionDate = person.lastInteractionDate,
                snoozedUntilDate = person.snoozedUntilDate
            )
        }

        logs.groupBy { it.personId to it.type }.forEach { (key, typedLogs) ->
            val person = peopleById[key.first] ?: return@forEach
            rhythmsByKey[key] = PersonContactRhythm(
                personId = key.first,
                contactTypeKey = key.second,
                isActive = true,
                customFrequencyDays = if (key.second == person.preferredContactTypeKey) person.customFrequencyDays else null,
                lastInteractionDate = typedLogs.maxOfOrNull { it.timestamp },
                snoozedUntilDate = if (key.second == person.preferredContactTypeKey) person.snoozedUntilDate else null
            )
        }

        return rhythmsByKey.values.toList()
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
    val rhythms: List<PersonContactRhythm>,
    val logs: List<InteractionLog>
)
