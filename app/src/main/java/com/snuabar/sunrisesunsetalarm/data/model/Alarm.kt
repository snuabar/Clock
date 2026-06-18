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
    val ringtoneUri: String? = null,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val ringMode: RingMode = RingMode.FULL_SCREEN,
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
    SUNRISE, SUNSET
}

enum class RingMode {
    FULL_SCREEN, NOTIFICATION, LIVE_ACTIVITY
}
