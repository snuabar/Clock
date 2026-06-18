package com.snuabar.sunrisesunsetalarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.BaseType
import com.snuabar.sunrisesunsetalarm.receiver.AlarmReceiver
import com.snuabar.sunrisesunsetalarm.util.HolidayUtil
import com.snuabar.sunrisesunsetalarm.util.SunCalcUtil
import java.util.*

class AlarmManagerHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm, latitude: Double, longitude: Double) {
        if (!alarm.isEnabled) return

        val nextTriggerTime = calculateNextTriggerTime(alarm, latitude, longitude)
        val pendingIntent = createPendingIntent(alarm)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerTime,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        val pendingIntent = createPendingIntent(alarm)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun calculateNextTriggerTime(alarm: Alarm, latitude: Double, longitude: Double): Long {
        val now = Calendar.getInstance()
        val repeatDays = alarm.getRepeatDaysList()
        val hasRepeatDays = repeatDays.any { it }

        // Try up to 8 days to find the next valid trigger day
        for (daysAhead in 0..7) {
            val checkDate = now.clone() as Calendar
            checkDate.add(Calendar.DAY_OF_YEAR, daysAhead)

            val sunTimes = SunCalcUtil.calculateSunTimes(checkDate, latitude, longitude)
            val baseTime = when (alarm.baseType) {
                BaseType.SUNRISE -> sunTimes.sunrise
                BaseType.SUNSET -> sunTimes.sunset
            }

            val triggerTime = baseTime + (alarm.offsetMinutes * 60 * 1000)

            if (daysAhead == 0) {
                // For today, check if the time has already passed
                if (triggerTime <= now.timeInMillis) {
                    if (!hasRepeatDays) {
                        // No repeat set, time has passed for today, try tomorrow
                        continue
                    }
                    // Time passed, try next day
                    continue
                }
            }

            if (hasRepeatDays) {
                // Check if this day is in the repeat schedule
                // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
                // repeatDays index: 0=Monday, 1=Tuesday, ..., 6=Sunday
                val dayOfWeek = checkDate.get(Calendar.DAY_OF_WEEK)
                val repeatIndex = when (dayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                if (!repeatDays[repeatIndex]) {
                    continue // This day is not in the repeat schedule
                }
            }

            // Skip holidays if configured
            if (alarm.skipHolidays && HolidayUtil.isHoliday(checkDate)) {
                continue
            }

            return triggerTime
        }

        // Fallback: return tomorrow's time
        val tomorrow = now.clone() as Calendar
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)
        val sunTimes = SunCalcUtil.calculateSunTimes(tomorrow, latitude, longitude)
        val baseTime = when (alarm.baseType) {
            BaseType.SUNRISE -> sunTimes.sunrise
            BaseType.SUNSET -> sunTimes.sunset
        }
        return baseTime + (alarm.offsetMinutes * 60 * 1000)
    }

    private fun createPendingIntent(alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_name", alarm.name)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
