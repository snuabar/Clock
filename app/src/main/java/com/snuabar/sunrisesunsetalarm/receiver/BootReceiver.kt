package com.snuabar.sunrisesunsetalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.snuabar.sunrisesunsetalarm.SunriseSunsetApplication
import com.snuabar.sunrisesunsetalarm.service.AlarmManagerHelper
import com.snuabar.sunrisesunsetalarm.util.SettingsManager
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllAlarms(context)
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        val app = SunriseSunsetApplication.instance
        val settingsManager = SettingsManager(context)
        val alarmRepository = app.alarmRepository
        val alarmManagerHelper = AlarmManagerHelper(context)
        val latitude = settingsManager.currentLatitude
        val longitude = settingsManager.currentLongitude

        runBlocking {
            val enabledAlarms = alarmRepository.getEnabledAlarms()
            for (alarm in enabledAlarms) {
                alarmManagerHelper.scheduleAlarm(alarm, latitude, longitude)
            }
        }
    }
}
