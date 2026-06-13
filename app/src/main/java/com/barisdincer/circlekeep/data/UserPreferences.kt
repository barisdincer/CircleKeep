package com.barisdincer.circlekeep.data

data class UserPreferences(
    val displayName: String = "",
    val initials: String = "CK",
    val avatarColorKey: String = "green",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val searchButtonEnabled: Boolean = true
) {
    val displayInitials: String
        get() = initials.trim().ifBlank { displayName.initialsFromName() }.ifBlank { "CK" }
            .take(3)
            .uppercase()
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private fun String.initialsFromName(): String {
    return trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1) }
}
