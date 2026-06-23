package com.snuabar.sunrisesunsetalarm.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.Location

@Database(
    entities = [Alarm::class, Location::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun locationDao(): LocationDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alarms ADD COLUMN customHour INTEGER NOT NULL DEFAULT -1")
                database.execSQL("ALTER TABLE alarms ADD COLUMN customMinute INTEGER NOT NULL DEFAULT -1")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alarms ADD COLUMN repeatMode TEXT NOT NULL DEFAULT 'CUSTOM'")
            }
        }
    }
}
