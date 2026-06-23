package com.snuabar.sunrisesunsetalarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.BaseType
import com.snuabar.sunrisesunsetalarm.data.model.RepeatMode
import com.snuabar.sunrisesunsetalarm.util.SunCalcUtil
import java.util.Calendar

@Composable
fun AlarmCard(
    alarm: Alarm,
    latitude: Double,
    longitude: Double,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val alarmTime = calculateAlarmTime(alarm, latitude, longitude)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                val iconData = when (alarm.baseType) {
                    BaseType.SUNRISE -> Triple(Icons.Default.WbSunny, "日出", MaterialTheme.colorScheme.primary)
                    BaseType.SUNSET -> Triple(Icons.Default.WbTwilight, "日落", MaterialTheme.colorScheme.tertiary)
                    BaseType.CUSTOM -> Triple(Icons.Default.AccessTime, "自定义", MaterialTheme.colorScheme.secondary)
                }
                Icon(
                    imageVector = iconData.first,
                    contentDescription = iconData.second,
                    modifier = Modifier.size(32.dp),
                    tint = iconData.third
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = alarm.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildDescription(alarm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = alarmTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun buildDescription(alarm: Alarm): String {
    val baseText = when (alarm.baseType) {
        BaseType.SUNRISE -> "日出"
        BaseType.SUNSET -> "日落"
        BaseType.CUSTOM -> "自定义"
    }
    val offsetText = when {
        alarm.baseType == BaseType.CUSTOM -> "" // Custom alarms don't show offset
        alarm.offsetMinutes > 0 -> " +${alarm.offsetMinutes}分钟"
        alarm.offsetMinutes < 0 -> " ${alarm.offsetMinutes}分钟"
        else -> ""
    }
    val repeatText = formatRepeatMode(alarm)
    return "$baseText$offsetText · $repeatText"
}

private fun formatRepeatMode(alarm: Alarm): String {
    return when (alarm.repeatMode) {
        RepeatMode.ONCE -> "仅一次"
        RepeatMode.DAILY -> "每天"
        RepeatMode.WEEKDAYS -> "工作日"
        RepeatMode.WEEKENDS -> "周末"
        RepeatMode.CUSTOM -> {
            val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
            val days = alarm.getRepeatDaysList()
            val selectedDays = days.mapIndexedNotNull { index, isSelected ->
                if (isSelected) dayLabels[index] else null
            }
            when {
                selectedDays.isEmpty() -> "仅一次"
                selectedDays.size == 7 -> "每天"
                else -> selectedDays.joinToString(", ")
            }
        }
    }
}

private fun calculateAlarmTime(alarm: Alarm, latitude: Double, longitude: Double): String {
    return try {
        when (alarm.baseType) {
            BaseType.CUSTOM -> {
                String.format("%02d:%02d", alarm.customHour.coerceIn(0, 23), alarm.customMinute.coerceIn(0, 59))
            }
            else -> {
                val sunTimes = SunCalcUtil.calculateSunTimes(Calendar.getInstance(), latitude, longitude)
                val baseTimeMillis = if (alarm.baseType == BaseType.SUNRISE) sunTimes.sunrise else sunTimes.sunset
                val calendar = Calendar.getInstance().apply { timeInMillis = baseTimeMillis }
                calendar.add(Calendar.MINUTE, alarm.offsetMinutes)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                String.format("%02d:%02d", hour, minute)
            }
        }
    } catch (_: Exception) {
        "06:15"
    }
}
