package com.snuabar.sunrisesunsetalarm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey
    val id: String,
    val name: String,
    val baseType: BaseType,
    val offsetMinutes: Int,
    val repeatDays: String,
    val repeatMode: RepeatMode = RepeatMode.CUSTOM,
    val ringtoneUri: String? = null,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val ringMode: RingMode = RingMode.FULL_SCREEN,
    val vibrateEnabled: Boolean = true,
    val ringDurationMinutes: Int = 5,
    val crescendoSeconds: Int = 0,
    val skipHolidays: Boolean = false,
    val customHour: Int = -1,
    val customMinute: Int = -1,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getRepeatDaysList(): List<Boolean> {
        return repeatDays.split(",").map { it.toBoolean() }
    }

    companion object {
        fun fromRepeatDaysList(days: List<Boolean>): String {
            return days.joinToString(",")
        }
    }
}

enum class BaseType {
    SUNRISE, SUNSET, CUSTOM
}

enum class RepeatMode {
    ONCE, DAILY, WEEKDAYS, WEEKENDS, CUSTOM
}

enum class RingMode {
    FULL_SCREEN, NOTIFICATION, LIVE_ACTIVITY
}
