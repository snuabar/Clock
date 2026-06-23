package com.snuabar.sunrisesunsetalarm

import android.app.Application
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.snuabar.sunrisesunsetalarm.data.database.AppDatabase
import com.snuabar.sunrisesunsetalarm.data.repository.AlarmRepository
import com.snuabar.sunrisesunsetalarm.data.repository.LocationRepository
import com.snuabar.sunrisesunsetalarm.receiver.AlarmRescheduleWork
import com.snuabar.sunrisesunsetalarm.util.HolidayUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SunriseSunsetApplication : Application() {

    companion object {
        lateinit var instance: SunriseSunsetApplication
            private set
    }

    lateinit var database: AppDatabase
        private set

    val alarmRepository: AlarmRepository by lazy {
        AlarmRepository(database.alarmDao())
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepository(database.locationDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "sunrise_sunset_alarm_db"
        )
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .build()

        scheduleAlarmRescheduleWork()

        // Preload holiday data in background
        CoroutineScope(Dispatchers.IO).launch {
            HolidayUtil.preload(this@SunriseSunsetApplication)
        }
    }

    private fun scheduleAlarmRescheduleWork() {
        val workRequest = PeriodicWorkRequestBuilder<AlarmRescheduleWork>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "alarm_reschedule",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
