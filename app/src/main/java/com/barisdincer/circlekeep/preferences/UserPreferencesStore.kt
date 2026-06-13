package com.barisdincer.circlekeep.preferences

import android.content.Context
import com.barisdincer.circlekeep.data.ThemeMode
import com.barisdincer.circlekeep.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesStore(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        "circlekeep_user_preferences",
        Context.MODE_PRIVATE
    )
    private val _preferences = MutableStateFlow(readPreferences())

    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    fun updateProfile(displayName: String, initials: String, avatarColorKey: String) {
        sharedPreferences.edit()
            .putString(KEY_DISPLAY_NAME, displayName.trim())
            .putString(KEY_INITIALS, initials.trim().uppercase().take(3))
            .putString(KEY_AVATAR_COLOR, avatarColorKey)
            .apply()
        refresh()
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        sharedPreferences.edit()
            .putString(KEY_THEME_MODE, themeMode.name)
            .apply()
        refresh()
    }

    fun updateSearchButtonEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SEARCH_BUTTON, enabled)
            .apply()
        refresh()
    }

    private fun refresh() {
        _preferences.value = readPreferences()
    }

    private fun readPreferences(): UserPreferences {
        return UserPreferences(
            displayName = sharedPreferences.getString(KEY_DISPLAY_NAME, "").orEmpty(),
            initials = sharedPreferences.getString(KEY_INITIALS, "CK").orEmpty().ifBlank { "CK" },
            avatarColorKey = sharedPreferences.getString(KEY_AVATAR_COLOR, "green").orEmpty().ifBlank { "green" },
            themeMode = sharedPreferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
                ?.let { value -> runCatching { ThemeMode.valueOf(value) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            searchButtonEnabled = sharedPreferences.getBoolean(KEY_SEARCH_BUTTON, true)
        )
    }

    private companion object {
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_INITIALS = "initials"
        const val KEY_AVATAR_COLOR = "avatar_color"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SEARCH_BUTTON = "search_button_enabled"
    }
}
