package com.barisdincer.circlekeep.data

enum class ContactDatePreset(
    val label: String,
    val daysAgo: Int?
) {
    TODAY("Bugün", 0),
    YESTERDAY("Dün", 1),
    THREE_DAYS_AGO("3 gün önce", 3),
    WEEK_AGO("1 hafta önce", 7),
    NONE("Henüz temas yok", null)
}

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

fun ContactDatePreset.resolveTimestamp(nowMillis: Long = System.currentTimeMillis()): Long? {
    return daysAgo?.let { nowMillis - it * MILLIS_PER_DAY }
}
