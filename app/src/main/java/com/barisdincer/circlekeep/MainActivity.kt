package com.barisdincer.circlekeep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.barisdincer.circlekeep.preferences.UserPreferencesStore
import com.barisdincer.circlekeep.ui.AppNavigation
import com.barisdincer.circlekeep.ui.theme.MyApplicationTheme
import com.barisdincer.circlekeep.device.CallLogSyncManager
import com.barisdincer.circlekeep.notifications.NotificationUtils
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        syncCallLogIfPermitted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationUtils.scheduleDailyReminder(this)

        requestRuntimePermissions()

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

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_CALL_LOG)
            }

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            runtimePermissionsLauncher.launch(permissions.toTypedArray())
        } else {
            syncCallLogIfPermitted()
        }
    }

    private fun syncCallLogIfPermitted() {
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val app = application as NetworkApplication
            CallLogSyncManager.sync(applicationContext, app.repository)
        }
    }
}
