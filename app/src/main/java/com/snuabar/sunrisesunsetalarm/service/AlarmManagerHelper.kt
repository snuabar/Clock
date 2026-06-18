package com.snuabar.sunrisesunsetalarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.BaseType
import com.snuabar.sunrisesunsetalarm.receiver.AlarmReceiver
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
        val today = now.clone() as Calendar

        val sunTimes = SunCalcUtil.calculateSunTimes(today, latitude, longitude)
        val baseTime = when (alarm.baseType) {
            BaseType.SUNRISE -> sunTimes.sunrise
            BaseType.SUNSET -> sunTimes.sunset
        }

        val triggerTime = baseTime + (alarm.offsetMinutes * 60 * 1000)
        val triggerCalendar = Calendar.getInstance().apply { timeInMillis = triggerTime }

        if (triggerCalendar.before(now)) {
            today.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowSunTimes = SunCalcUtil.calculateSunTimes(today, latitude, longitude)
            val tomorrowBaseTime = when (alarm.baseType) {
                BaseType.SUNRISE -> tomorrowSunTimes.sunrise
                BaseType.SUNSET -> tomorrowSunTimes.sunset
            }
            return tomorrowBaseTime + (alarm.offsetMinutes * 60 * 1000)
        }

        return triggerTime
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
