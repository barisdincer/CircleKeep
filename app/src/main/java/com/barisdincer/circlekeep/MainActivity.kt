package com.barisdincer.circlekeep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.barisdincer.circlekeep.preferences.UserPreferencesStore
import com.barisdincer.circlekeep.ui.AppNavigation
import com.barisdincer.circlekeep.ui.theme.MyApplicationTheme
import com.barisdincer.circlekeep.notifications.NotificationUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationUtils.scheduleDailyReminder(this)

        enableEdgeToEdge()
        setContent {
            val preferencesStore = remember { UserPreferencesStore(applicationContext) }
            val userPreferences by preferencesStore.preferences.collectAsState()
            MyApplicationTheme(themeMode = userPreferences.themeMode) {
                AppNavigation(
                    userPreferences = userPreferences,
                    preferencesStore = preferencesStore
                )
            }
        }
    }
}
