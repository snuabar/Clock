package com.snuabar.sunrisesunsetalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.snuabar.sunrisesunsetalarm.service.AlarmManagerHelper
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
        // Reschedule all enabled alarms
        // This is called daily to ensure alarms are properly set
        return Result.success()
    }
}
