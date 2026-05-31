package com.barisdincer.circlekeep.data

fun contactActionLabel(typeKey: String, fallbackLabel: String): String {
    return when (typeKey) {
        DefaultContactTypes.CALL -> "Arama kaydet"
        DefaultContactTypes.MESSAGE -> "Mesaj kaydet"
        DefaultContactTypes.MEETING -> "Buluşma kaydet"
        else -> "$fallbackLabel kaydet"
    }
}
