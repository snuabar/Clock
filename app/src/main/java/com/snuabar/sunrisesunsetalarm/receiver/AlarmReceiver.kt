package com.snuabar.sunrisesunsetalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snuabar.sunrisesunsetalarm.SunriseSunsetApplication
import com.snuabar.sunrisesunsetalarm.service.AlarmService
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: return

        val app = SunriseSunsetApplication.instance
        val alarmRepository = app.alarmRepository

        val alarm = runBlocking { alarmRepository.getAlarmById(alarmId) } ?: return

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
    }
}
