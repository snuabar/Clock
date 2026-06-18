package com.snuabar.sunrisesunsetalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snuabar.sunrisesunsetalarm.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: return
        val alarmName = intent.getStringExtra("alarm_name") ?: "Alarm"

        // Start the alarm service to handle the alarm trigger
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_name", alarmName)
        }
        context.startForegroundService(serviceIntent)
    }
}
