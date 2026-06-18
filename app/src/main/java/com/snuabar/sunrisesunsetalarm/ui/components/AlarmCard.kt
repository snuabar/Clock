package com.snuabar.sunrisesunsetalarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
                Icon(
                    imageVector = when (alarm.baseType) {
                        BaseType.SUNRISE -> Icons.Default.WbSunny
                        BaseType.SUNSET -> Icons.Default.WbTwilight
                    },
                    contentDescription = if (alarm.baseType == BaseType.SUNRISE) "日出" else "日落",
                    modifier = Modifier.size(32.dp),
                    tint = when (alarm.baseType) {
                        BaseType.SUNRISE -> MaterialTheme.colorScheme.primary
                        BaseType.SUNSET -> MaterialTheme.colorScheme.tertiary
                    }
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
    }
    val offsetText = when {
        alarm.offsetMinutes > 0 -> " +${alarm.offsetMinutes}分钟"
        alarm.offsetMinutes < 0 -> " ${alarm.offsetMinutes}分钟"
        else -> ""
    }
    return "$baseText$offsetText"
}

private fun calculateAlarmTime(alarm: Alarm, latitude: Double, longitude: Double): String {
    return try {
        val sunTimes = SunCalcUtil.calculateSunTimes(Calendar.getInstance(), latitude, longitude)
        val baseTimeMillis = if (alarm.baseType == BaseType.SUNRISE) sunTimes.sunrise else sunTimes.sunset
        val calendar = Calendar.getInstance().apply { timeInMillis = baseTimeMillis }
        calendar.add(Calendar.MINUTE, alarm.offsetMinutes)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        String.format("%02d:%02d", hour, minute)
    } catch (_: Exception) {
        "06:15"
    }
}
