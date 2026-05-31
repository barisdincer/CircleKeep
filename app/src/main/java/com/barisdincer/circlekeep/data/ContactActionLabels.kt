package com.barisdincer.circlekeep.data

fun contactActionLabel(typeKey: String, fallbackLabel: String): String {
    return when (typeKey) {
        DefaultContactTypes.CALL -> "Aradım"
        DefaultContactTypes.MESSAGE -> "Mesajlaştık"
        DefaultContactTypes.MEETING -> "Buluştuk"
        else -> "$fallbackLabel kaydet"
    }
}
