package com.snuabar.sunrisesunsetalarm.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.snuabar.sunrisesunsetalarm.R
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.BaseType
import com.snuabar.sunrisesunsetalarm.data.model.RepeatMode
import com.snuabar.sunrisesunsetalarm.util.SunCalcUtil
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun AlarmCard(
    alarm: Alarm,
    latitude: Double,
    longitude: Double,
    isExpanded: Boolean,
    onExpandChanged: (Boolean) -> Unit,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val alarmTime = calculateAlarmTime(alarm, latitude, longitude)
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val maxSwipePx = with(androidx.compose.ui.platform.LocalDensity.current) { 80.dp.toPx() }

    // Sync with external expand state
    LaunchedEffect(isExpanded) {
        val target = if (isExpanded) -maxSwipePx else 0f
        if (kotlin.math.abs(offsetX.value - target) > 1f) {
            offsetX.animateTo(target)
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // 背景层（红色删除区域）
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    MaterialTheme.colorScheme.error,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(
                onClick = {
                    onExpandChanged(false)
                    onDelete()
                },
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }

        // 前景层（闹钟卡片）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -maxSwipePx / 2) {
                                    offsetX.animateTo(-maxSwipePx)
                                    onExpandChanged(true)
                                } else {
                                    offsetX.animateTo(0f)
                                    onExpandChanged(false)
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-maxSwipePx, 0f)
                            scope.launch { offsetX.snapTo(newOffset) }
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isExpanded) {
                            onExpandChanged(false)
                        } else {
                            onClick()
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val iconData = when (alarm.baseType) {
                        BaseType.SUNRISE -> Triple(Icons.Default.WbSunny, stringResource(R.string.base_type_sunrise), MaterialTheme.colorScheme.primary)
                        BaseType.SUNSET -> Triple(Icons.Default.WbTwilight, stringResource(R.string.base_type_sunset), MaterialTheme.colorScheme.tertiary)
                        BaseType.CUSTOM -> Triple(Icons.Default.AccessTime, stringResource(R.string.base_type_custom), MaterialTheme.colorScheme.secondary)
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

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { enabled ->
                        if (isExpanded) {
                            onExpandChanged(false)
                        }
                        onToggle(enabled)
                    }
                )
            }
        }
    }
}

@Composable
private fun buildDescription(alarm: Alarm): String {
    val baseText = when (alarm.baseType) {
        BaseType.SUNRISE -> stringResource(R.string.base_type_sunrise)
        BaseType.SUNSET -> stringResource(R.string.base_type_sunset)
        BaseType.CUSTOM -> stringResource(R.string.base_type_custom)
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

@Composable
private fun formatRepeatMode(alarm: Alarm): String {
    return when (alarm.repeatMode) {
        RepeatMode.ONCE -> stringResource(R.string.repeat_once)
        RepeatMode.DAILY -> stringResource(R.string.repeat_daily)
        RepeatMode.WEEKDAYS -> stringResource(R.string.repeat_weekdays)
        RepeatMode.WEEKENDS -> stringResource(R.string.repeat_weekends)
        RepeatMode.CUSTOM -> {
            val dayLabels = listOf(
                stringResource(R.string.day_mon),
                stringResource(R.string.day_tue),
                stringResource(R.string.day_wed),
                stringResource(R.string.day_thu),
                stringResource(R.string.day_fri),
                stringResource(R.string.day_sat),
                stringResource(R.string.day_sun)
            )
            val days = alarm.getRepeatDaysList()
            val selectedDays = days.mapIndexedNotNull { index, isSelected ->
                if (isSelected) dayLabels[index] else null
            }
            when {
                selectedDays.isEmpty() -> stringResource(R.string.repeat_once)
                selectedDays.size == 7 -> stringResource(R.string.repeat_daily)
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
