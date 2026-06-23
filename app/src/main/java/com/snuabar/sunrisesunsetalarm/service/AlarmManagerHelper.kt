package com.snuabar.sunrisesunsetalarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.BaseType
import com.snuabar.sunrisesunsetalarm.data.model.RepeatMode
import com.snuabar.sunrisesunsetalarm.receiver.AlarmReceiver
import com.snuabar.sunrisesunsetalarm.util.HolidayUtil
import com.snuabar.sunrisesunsetalarm.util.SunCalcUtil
import java.util.*

class AlarmManagerHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: Alarm, latitude: Double, longitude: Double) {
        if (!alarm.isEnabled) {
            Log.d("AlarmManagerHelper", "Alarm ${alarm.id} is disabled, skipping schedule")
            return
        }

        val nextTriggerTime = calculateNextTriggerTime(alarm, latitude, longitude)
        val pendingIntent = createPendingIntent(alarm)

        Log.d("AlarmManagerHelper", "Scheduling alarm ${alarm.id} at ${Date(nextTriggerTime)}, repeatMode=${alarm.repeatMode}, baseType=${alarm.baseType}")

        // Check exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmManagerHelper", "Cannot schedule exact alarm - SCHEDULE_EXACT_ALARM permission not granted. " +
                    "User must enable this in system Settings > Apps > Special app access > Alarms & reminders")
                // Fallback to inexact alarm as a best-effort approach
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
                Log.d("AlarmManagerHelper", "Scheduled inexact fallback alarm for ${alarm.id}")
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerTime,
            pendingIntent
        )
        Log.d("AlarmManagerHelper", "Successfully scheduled exact alarm ${alarm.id}")
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

            val triggerTime = when (alarm.baseType) {
                BaseType.CUSTOM -> {
                    // For custom alarms, use the specified hour and minute
                    checkDate.set(Calendar.HOUR_OF_DAY, alarm.customHour.coerceIn(0, 23))
                    checkDate.set(Calendar.MINUTE, alarm.customMinute.coerceIn(0, 59))
                    checkDate.set(Calendar.SECOND, 0)
                    checkDate.set(Calendar.MILLISECOND, 0)
                    checkDate.timeInMillis
                }
                else -> {
                    val sunTimes = SunCalcUtil.calculateSunTimes(checkDate, latitude, longitude)
                    val baseTime = when (alarm.baseType) {
                        BaseType.SUNRISE -> sunTimes.sunrise
                        BaseType.SUNSET -> sunTimes.sunset
                        else -> throw IllegalStateException("Unexpected base type: ${alarm.baseType}")
                    }
                    baseTime + (alarm.offsetMinutes * 60 * 1000)
                }
            }

            // Check if the trigger time has already passed (for all days, not just today)
            if (triggerTime <= now.timeInMillis) {
                if (!hasRepeatDays && daysAhead == 0) {
                    // No repeat set, time has passed for today, try tomorrow
                    continue
                }
                // Time passed, try next day
                continue
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
        return when (alarm.baseType) {
            BaseType.CUSTOM -> {
                tomorrow.set(Calendar.HOUR_OF_DAY, alarm.customHour.coerceIn(0, 23))
                tomorrow.set(Calendar.MINUTE, alarm.customMinute.coerceIn(0, 59))
                tomorrow.set(Calendar.SECOND, 0)
                tomorrow.set(Calendar.MILLISECOND, 0)
                tomorrow.timeInMillis
            }
            else -> {
                val sunTimes = SunCalcUtil.calculateSunTimes(tomorrow, latitude, longitude)
                val baseTime = when (alarm.baseType) {
                    BaseType.SUNRISE -> sunTimes.sunrise
                    BaseType.SUNSET -> sunTimes.sunset
                    else -> throw IllegalStateException("Unexpected base type: ${alarm.baseType}")
                }
                baseTime + (alarm.offsetMinutes * 60 * 1000)
            }
        }
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
