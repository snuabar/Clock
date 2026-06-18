package com.snuabar.sunrisesunsetalarm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.BaseType
import com.snuabar.sunrisesunsetalarm.data.model.RingMode
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmBottomSheet(
    alarm: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    // Pre-fill values if editing
    var selectedType by remember { mutableStateOf(alarm?.baseType ?: BaseType.SUNRISE) }
    var alarmName by remember { mutableStateOf(alarm?.name ?: "") }
    var offsetMinutes by remember { mutableIntStateOf(alarm?.offsetMinutes ?: 0) }
    var showAdvanced by remember { mutableStateOf(false) }

    // Advanced settings
    var selectedRingMode by remember { mutableStateOf(alarm?.ringMode ?: RingMode.FULL_SCREEN) }
    var snoozeEnabled by remember { mutableStateOf(alarm?.snoozeEnabled ?: true) }
    var snoozeMinutes by remember { mutableIntStateOf(alarm?.snoozeMinutes ?: 5) }
    var repeatDays by remember {
        mutableStateOf(alarm?.getRepeatDaysList() ?: List(7) { true })
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title + Save button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (alarm != null) "编辑闹钟" else "添加闹钟",
                    style = MaterialTheme.typography.headlineSmall
                )
                TextButton(
                    onClick = {
                        val savedAlarm = Alarm(
                            id = alarm?.id ?: UUID.randomUUID().toString(),
                            name = alarmName.ifEmpty { "新闹钟" },
                            baseType = selectedType,
                            offsetMinutes = offsetMinutes,
                            repeatDays = repeatDays.joinToString(",") { it.toString() },
                            ringMode = selectedRingMode,
                            snoozeEnabled = snoozeEnabled,
                            snoozeMinutes = snoozeMinutes
                        )
                        onSave(savedAlarm)
                    }
                ) {
                    Text("保存")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alarm name input
            OutlinedTextField(
                value = alarmName,
                onValueChange = { alarmName = it },
                label = { Text("闹钟名称") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Base type selector
            Text(
                text = "基准时间",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.selectableGroup()) {
                BaseType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Text(
                            text = when (type) {
                                BaseType.SUNRISE -> "日出"
                                BaseType.SUNSET -> "日落"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Offset slider
            Text(
                text = "偏移量: ${formatOffset(offsetMinutes)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = offsetMinutes.toFloat(),
                onValueChange = { offsetMinutes = it.toInt() },
                valueRange = -120f..120f,
                steps = 239,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Repeat days selector
            Text(
                text = "重复",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
                repeatDays.forEachIndexed { index, isSelected ->
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            repeatDays = repeatDays.toMutableList().apply { this[index] = !this[index] }
                        },
                        label = { Text(dayLabels[index]) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced settings button
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "收起高级设置" else "高级设置")
            }

            if (showAdvanced) {
                Spacer(modifier = Modifier.height(16.dp))

                // Ring mode selector
                Text(
                    text = "响铃模式",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    RingMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRingMode = mode }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRingMode == mode,
                                onClick = { selectedRingMode = mode }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    RingMode.FULL_SCREEN -> "全屏闹钟"
                                    RingMode.NOTIFICATION -> "通知栏提醒"
                                    RingMode.LIVE_ACTIVITY -> "实时活动"
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Snooze settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("启用贪睡", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = snoozeEnabled,
                        onCheckedChange = { snoozeEnabled = it }
                    )
                }

                if (snoozeEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "贪睡时长: $snoozeMinutes 分钟",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = snoozeMinutes.toFloat(),
                        onValueChange = { snoozeMinutes = it.toInt() },
                        valueRange = 1f..30f,
                        steps = 29,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun formatOffset(minutes: Int): String {
    return when {
        minutes > 0 -> "延后 $minutes 分钟"
        minutes < 0 -> "提前 ${-minutes} 分钟"
        else -> "无偏移"
    }
}
