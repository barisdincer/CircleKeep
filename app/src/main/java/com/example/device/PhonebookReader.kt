package com.example.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.Person
import com.example.data.PhoneNumberNormalizer

data class PhonebookContact(
    val name: String,
    val phoneNumber: String,
    val normalizedPhoneNumber: String,
    val lookupKey: String?
) {
    val selectionKey: String
        get() = lookupKey ?: normalizedPhoneNumber
}

object PhonebookReader {
    fun readContacts(context: Context, existingPeople: List<Person>): List<PhonebookContact> {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val existingLookupKeys = existingPeople.mapNotNull { it.contactLookupKey }.toSet()
        val existingPhones = existingPeople
            .map { it.normalizedPhoneNumber.ifBlank { PhoneNumberNormalizer.normalize(it.phoneNumber) } }
            .filter { it.isNotBlank() }
            .toSet()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
        )

        val contacts = linkedMapOf<String, PhonebookContact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val lookupKeyIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex).orEmpty().trim()
                val phone = it.getString(numberIndex).orEmpty().trim()
                val normalizedPhone = PhoneNumberNormalizer.normalize(phone)
                val lookupKey = it.getString(lookupKeyIndex)
                if (name.isBlank() || normalizedPhone.isBlank()) continue
                if (lookupKey != null && lookupKey in existingLookupKeys) continue
                if (normalizedPhone in existingPhones) continue

                val key = lookupKey ?: normalizedPhone
                contacts.putIfAbsent(
                    key,
                    PhonebookContact(
                        name = name,
                        phoneNumber = phone,
                        normalizedPhoneNumber = normalizedPhone,
                        lookupKey = lookupKey
                    )
                )
            }
        }

        return contacts.values.sortedBy { it.name.lowercase() }
    }
}
