package com.fortunepocket.core.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fortunepocket.core.data.datastore.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-schedules daily reminder after device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                val preferences = UserPreferencesDataStore(context.applicationContext)
                if (preferences.getNotificationEnabledValue()) {
                    val scheduler = NotificationScheduler(context.applicationContext)
                    scheduler.scheduleDailyReminder(
                        preferences.getNotificationHourValue(),
                        preferences.getNotificationMinuteValue()
                    )
                }
                pendingResult.finish()
            }
        }
    }
}
