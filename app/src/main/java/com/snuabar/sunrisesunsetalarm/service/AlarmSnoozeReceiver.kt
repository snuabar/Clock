package com.snuabar.sunrisesunsetalarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.snuabar.sunrisesunsetalarm.SunriseSunsetApplication
import com.snuabar.sunrisesunsetalarm.receiver.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: return

        // Cancel the notification
        val notificationHelper = AlarmNotificationHelper(context)
        notificationHelper.cancelNotification()

        // Stop the alarm service
        context.stopService(Intent(context, AlarmService::class.java))

        // Get snooze minutes from database and reschedule
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = SunriseSunsetApplication.instance.database.alarmDao().getAlarmById(alarmId)
                val snoozeMinutes = alarm?.snoozeMinutes ?: 5

                // Schedule a one-time alarm after snooze minutes
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val triggerTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L

                val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("alarm_id", alarmId)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarmId.hashCode(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
