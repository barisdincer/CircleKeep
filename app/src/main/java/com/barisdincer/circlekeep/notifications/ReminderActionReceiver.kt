package com.barisdincer.circlekeep.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.barisdincer.circlekeep.NetworkApplication
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val personId = intent.getIntExtra(ReminderActions.EXTRA_PERSON_ID, -1)
        if (personId <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as NetworkApplication
                when (intent.action) {
                    ReminderActions.ACTION_LOG_CALL -> {
                        app.repository.logInteraction(personId, "CALL")
                    }
                    ReminderActions.ACTION_SNOOZE_TOMORROW -> {
                        app.repository.snoozePerson(personId, tomorrowAtNine())
                    }
                }

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(ReminderActions.NOTIFICATION_ID)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun tomorrowAtNine(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
