package com.snuabar.sunrisesunsetalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.snuabar.sunrisesunsetalarm.SunriseSunsetApplication
import com.snuabar.sunrisesunsetalarm.data.model.RepeatMode
import com.snuabar.sunrisesunsetalarm.service.AlarmManagerHelper
import com.snuabar.sunrisesunsetalarm.service.AlarmService
import com.snuabar.sunrisesunsetalarm.util.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: run {
            Log.w("AlarmReceiver", "Received alarm broadcast with no alarm_id")
            return
        }

        Log.d("AlarmReceiver", "Alarm triggered: $alarmId")

        val app = SunriseSunsetApplication.instance
        val alarmRepository = app.alarmRepository

        val alarm = runBlocking { alarmRepository.getAlarmById(alarmId) }
        if (alarm == null) {
            Log.w("AlarmReceiver", "Alarm $alarmId not found in database")
            return
        }

        // Start the alarm service to handle the alarm trigger
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_name", alarm.name)
            putExtra("ring_mode", alarm.ringMode.name)
            putExtra("ringtone_uri", alarm.ringtoneUri)
            putExtra("vibrate_enabled", alarm.vibrateEnabled)
            putExtra("ring_duration_minutes", alarm.ringDurationMinutes)
            putExtra("crescendo_seconds", alarm.crescendoSeconds)
        }
        context.startForegroundService(serviceIntent)

        // Handle post-alarm logic: reschedule for repeating alarms, disable for one-time
        val settingsManager = SettingsManager(context)
        val latitude = settingsManager.currentLatitude
        val longitude = settingsManager.currentLongitude

        when (alarm.repeatMode) {
            RepeatMode.ONCE -> {
                // Disable one-time alarm after it triggers
                Log.d("AlarmReceiver", "Disabling one-time alarm ${alarm.id}")
                CoroutineScope(Dispatchers.IO).launch {
                    alarmRepository.updateAlarm(alarm.copy(isEnabled = false))
                }
            }
            RepeatMode.DAILY, RepeatMode.WEEKDAYS, RepeatMode.WEEKENDS, RepeatMode.CUSTOM -> {
                // Reschedule repeating alarm for next occurrence
                Log.d("AlarmReceiver", "Rescheduling repeating alarm ${alarm.id}")
                val alarmManagerHelper = AlarmManagerHelper(context)
                alarmManagerHelper.scheduleAlarm(alarm, latitude, longitude)
            }
        }
    }
}
