package com.fortunepocket.core.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject

class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleDailyReminder(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTriggerAtMillis(hour, minute),
            AlarmManager.INTERVAL_DAY,
            reminderPendingIntent()
        )
    }

    fun cancelDailyReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent())
    }

    private fun reminderPendingIntent(): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerAtMillis(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
    }

    companion object {
        private const val REQUEST_CODE = 4001
    }
}
