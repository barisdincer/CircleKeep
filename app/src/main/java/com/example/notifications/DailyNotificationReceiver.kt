package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NetworkApplication
import com.example.data.ContactReminderCalculator
import com.example.device.CallLogSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as NetworkApplication
                CallLogSyncManager.sync(context, app.repository)

                val dueContacts = ContactReminderCalculator.dueContacts(
                    people = app.repository.getPeopleSnapshot(),
                    waves = app.repository.getWaveSnapshot()
                )

                if (dueContacts.isNotEmpty()) {
                    val names = dueContacts.take(3).joinToString(", ") { it.person.name }
                    val primaryPersonId = dueContacts.first().person.id
                    val message = if (dueContacts.size == 1) {
                        "${dueContacts.first().person.name} ile hal hatir sorma zamani."
                    } else {
                        "Bugun ${dueContacts.size} kisi icin zaman geldi: $names"
                    }
                    showNotification(context, "Hal hatir zamani", message, primaryPersonId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String, primaryPersonId: Int) {
        val channelId = "daily_network_reminders"
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to reach out to your network"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calledIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActions.ACTION_LOG_CALL
            putExtra(ReminderActions.EXTRA_PERSON_ID, primaryPersonId)
        }
        val calledPendingIntent = PendingIntent.getBroadcast(
            context,
            primaryPersonId * 10 + 1,
            calledIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActions.ACTION_SNOOZE_TOMORROW
            putExtra(ReminderActions.EXTRA_PERSON_ID, primaryPersonId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            primaryPersonId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Called", calledPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Tomorrow", snoozePendingIntent)

        notificationManager.notify(ReminderActions.NOTIFICATION_ID, builder.build())
    }
}
