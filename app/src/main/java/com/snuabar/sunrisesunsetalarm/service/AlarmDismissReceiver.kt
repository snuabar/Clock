package com.snuabar.sunrisesunsetalarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: return

        // Cancel the notification
        val notificationHelper = AlarmNotificationHelper(context)
        notificationHelper.cancelNotification()

        // TODO: Mark alarm as dismissed, handle snooze logic if needed
    }
}
