package com.barisdincer.circlekeep.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.barisdincer.circlekeep.data.NetworkRepository
import com.barisdincer.circlekeep.data.Person
import com.barisdincer.circlekeep.data.PhoneNumberNormalizer

data class CallLogSyncResult(
    val permissionGranted: Boolean,
    val matchedCalls: Int
)

object CallLogSyncManager {
    private const val MAX_ROWS_TO_SCAN = 500

    suspend fun sync(context: Context, repository: NetworkRepository): CallLogSyncResult {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return CallLogSyncResult(permissionGranted = false, matchedCalls = 0)
        }

        val people = repository.getPeopleSnapshot()
            .filter { it.normalizedPhoneNumber.isNotBlank() || it.phoneNumber.isNotBlank() }
        if (people.isEmpty()) return CallLogSyncResult(permissionGranted = true, matchedCalls = 0)

        val exactMatches = people
            .mapNotNull { person ->
                val normalized = PhoneNumberNormalizer.normalize(
                    person.normalizedPhoneNumber.ifBlank { person.phoneNumber }
                )
                normalized.takeIf { it.isNotBlank() }?.let { it to person }
            }
            .toMap()

        val tailMatches = exactMatches.entries
            .filter { it.key.length >= 10 }
            .groupBy({ it.key.takeLast(10) }, { it.value })

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE
        )

        var matchedCalls = 0
        val processedPeople = mutableSetOf<Int>()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            val numberIndex = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateIndex = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val typeIndex = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            var scannedRows = 0

            while (it.moveToNext() && scannedRows < MAX_ROWS_TO_SCAN) {
                scannedRows += 1

                val callType = it.getInt(typeIndex)
                if (callType != CallLog.Calls.INCOMING_TYPE && callType != CallLog.Calls.OUTGOING_TYPE) {
                    continue
                }

                val phoneNumber = it.getString(numberIndex).orEmpty()
                val normalizedPhone = PhoneNumberNormalizer.normalize(phoneNumber)
                val person = findPerson(normalizedPhone, exactMatches, tailMatches) ?: continue
                if (!processedPeople.add(person.id)) continue

                val timestamp = it.getLong(dateIndex)
                repository.logCallInteraction(person, timestamp)
                matchedCalls += 1
            }
        }

        return CallLogSyncResult(permissionGranted = true, matchedCalls = matchedCalls)
    }

    private fun findPerson(
        normalizedPhone: String,
        exactMatches: Map<String, Person>,
        tailMatches: Map<String, List<Person>>
    ): Person? {
        if (normalizedPhone.isBlank()) return null

        exactMatches[normalizedPhone]?.let { return it }

        val phoneTail = normalizedPhone.takeLast(10)
        val possibleTailMatches = tailMatches[phoneTail].orEmpty()
        return possibleTailMatches.singleOrNull()
    }
}
