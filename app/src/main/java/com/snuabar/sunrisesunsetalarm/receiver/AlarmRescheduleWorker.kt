package com.snuabar.sunrisesunsetalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.snuabar.sunrisesunsetalarm.SunriseSunsetApplication
import com.snuabar.sunrisesunsetalarm.service.AlarmManagerHelper
import com.snuabar.sunrisesunsetalarm.util.SettingsManager
import java.util.concurrent.TimeUnit

class AlarmRescheduleWorker : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val workRequest = PeriodicWorkRequestBuilder<AlarmRescheduleWork>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "alarm_reschedule",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

class AlarmRescheduleWork(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = SunriseSunsetApplication.instance
            val settingsManager = SettingsManager(applicationContext)
            val alarmRepository = app.alarmRepository
            val alarmManagerHelper = AlarmManagerHelper(applicationContext)
            val latitude = settingsManager.currentLatitude
            val longitude = settingsManager.currentLongitude

            val enabledAlarms = alarmRepository.getEnabledAlarms()
            for (alarm in enabledAlarms) {
                alarmManagerHelper.scheduleAlarm(alarm, latitude, longitude)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
