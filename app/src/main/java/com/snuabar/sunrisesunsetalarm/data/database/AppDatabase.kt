package com.snuabar.sunrisesunsetalarm.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.Location

@Database(
    entities = [Alarm::class, Location::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun locationDao(): LocationDao
}
