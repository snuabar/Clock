package com.snuabar.sunrisesunsetalarm

import android.app.Application
import androidx.room.Room
import com.snuabar.sunrisesunsetalarm.data.database.AppDatabase
import com.snuabar.sunrisesunsetalarm.data.repository.AlarmRepository
import com.snuabar.sunrisesunsetalarm.data.repository.LocationRepository

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
        ).build()
    }
}
