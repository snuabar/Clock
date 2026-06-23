package com.snuabar.sunrisesunsetalarm.ui.components

import android.app.TimePickerDialog
import android.media.RingtoneManager
import android.net.Uri
import android.widget.TimePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.snuabar.sunrisesunsetalarm.R
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.BaseType
import com.snuabar.sunrisesunsetalarm.data.model.RepeatMode
import com.snuabar.sunrisesunsetalarm.data.model.RingMode
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmBottomSheet(
    alarm: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val context = LocalContext.current

    // Pre-fill values if editing
    var selectedType by remember { mutableStateOf(alarm?.baseType ?: BaseType.SUNRISE) }
    var alarmName by remember { mutableStateOf(alarm?.name ?: "") }
    var offsetMinutes by remember { mutableIntStateOf(alarm?.offsetMinutes ?: 0) }
    var showAdvanced by remember { mutableStateOf(false) }

    // Custom time fields - default to current time for new alarms
    val currentCalendar = Calendar.getInstance()
    var customHour by remember { mutableIntStateOf(alarm?.customHour?.takeIf { it >= 0 } ?: currentCalendar.get(Calendar.HOUR_OF_DAY)) }
    var customMinute by remember { mutableIntStateOf(alarm?.customMinute?.takeIf { it >= 0 } ?: currentCalendar.get(Calendar.MINUTE)) }

    // Time picker dialog
    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _: TimePicker, hour: Int, minute: Int ->
                customHour = hour
                customMinute = minute
            },
            customHour,
            customMinute,
            true
        )
    }

    // Repeat mode
    var selectedRepeatMode by remember { mutableStateOf(alarm?.repeatMode ?: RepeatMode.CUSTOM) }
    var repeatDays by remember {
        mutableStateOf(alarm?.getRepeatDaysList() ?: List(7) { true })
    }

    // Repeat mode dropdown
    val repeatModeOptions = listOf(
        RepeatMode.ONCE to stringResource(R.string.repeat_once),
        RepeatMode.DAILY to stringResource(R.string.repeat_daily),
        RepeatMode.WEEKDAYS to stringResource(R.string.repeat_weekdays),
        RepeatMode.WEEKENDS to stringResource(R.string.repeat_weekends),
        RepeatMode.CUSTOM to stringResource(R.string.repeat_custom)
    )
    var repeatModeExpanded by remember { mutableStateOf(false) }

    // Advanced settings
    var selectedRingMode by remember { mutableStateOf(alarm?.ringMode ?: RingMode.FULL_SCREEN) }
    var vibrateEnabled by remember { mutableStateOf(alarm?.vibrateEnabled ?: true) }
    var ringDurationMinutes by remember { mutableIntStateOf(alarm?.ringDurationMinutes ?: 5) }
    var crescendoSeconds by remember { mutableIntStateOf(alarm?.crescendoSeconds ?: 0) }
    var skipHolidays by remember { mutableStateOf(alarm?.skipHolidays ?: false) }
    var ringtoneUri by remember { mutableStateOf(alarm?.ringtoneUri) }
    var snoozeEnabled by remember { mutableStateOf(alarm?.snoozeEnabled ?: true) }
    var snoozeMinutes by remember { mutableIntStateOf(alarm?.snoozeMinutes ?: 5) }

    // Ringtone picker launcher
    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            ringtoneUri = uri?.toString()
        }
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
                    text = if (alarm != null) stringResource(R.string.edit_alarm) else stringResource(R.string.add_alarm),
                    style = MaterialTheme.typography.headlineSmall
                )
                TextButton(
                    onClick = {
                        val savedAlarm = Alarm(
                            id = alarm?.id ?: UUID.randomUUID().toString(),
                            name = alarmName.ifEmpty { context.getString(R.string.default_alarm_name) },
                            baseType = selectedType,
                            offsetMinutes = offsetMinutes,
                            repeatDays = repeatDays.joinToString(",") { it.toString() },
                            repeatMode = selectedRepeatMode,
                            ringtoneUri = ringtoneUri,
                            ringMode = selectedRingMode,
                            vibrateEnabled = vibrateEnabled,
                            ringDurationMinutes = ringDurationMinutes,
                            crescendoSeconds = crescendoSeconds,
                            skipHolidays = skipHolidays,
                            customHour = customHour,
                            customMinute = customMinute,
                            snoozeEnabled = snoozeEnabled,
                            snoozeMinutes = snoozeMinutes,
                            createdAt = alarm?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(savedAlarm)
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alarm name input
            OutlinedTextField(
                value = alarmName,
                onValueChange = { alarmName = it },
                label = { Text(stringResource(R.string.alarm_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Base type selector
            Text(
                text = stringResource(R.string.base_time),
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
                                BaseType.SUNRISE -> stringResource(R.string.base_type_sunrise)
                                BaseType.SUNSET -> stringResource(R.string.base_type_sunset)
                                BaseType.CUSTOM -> stringResource(R.string.base_type_custom)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show offset slider for SUNRISE/SUNSET, show time picker for CUSTOM
            if (selectedType == BaseType.CUSTOM) {
                // Custom time picker - click to open TimePickerDialog
                Text(
                    text = stringResource(R.string.ring_time),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { timePickerDialog.show() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d:%02d", customHour, customMinute),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                // Offset slider for sunrise/sunset
                Text(
                    text = stringResource(R.string.offset_format, formatOffset(offsetMinutes)),
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = offsetMinutes.toFloat(),
                    onValueChange = { offsetMinutes = it.toInt() },
                    valueRange = -120f..120f,
                    steps = 239,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Repeat mode selector
            Text(
                text = stringResource(R.string.repeat),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = repeatModeExpanded,
                onExpandedChange = { repeatModeExpanded = !repeatModeExpanded }
            ) {
                OutlinedTextField(
                    value = repeatModeOptions.find { it.first == selectedRepeatMode }?.second ?: stringResource(R.string.repeat_custom),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatModeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = repeatModeExpanded,
                    onDismissRequest = { repeatModeExpanded = false }
                ) {
                    repeatModeOptions.forEach { (mode, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedRepeatMode = mode
                                repeatModeExpanded = false
                                // Update repeatDays based on selected mode
                                repeatDays = when (mode) {
                                    RepeatMode.ONCE -> List(7) { false }
                                    RepeatMode.DAILY -> List(7) { true }
                                    RepeatMode.WEEKDAYS -> listOf(true, true, true, true, true, false, false)
                                    RepeatMode.WEEKENDS -> listOf(false, false, false, false, false, true, true)
                                    RepeatMode.CUSTOM -> repeatDays // Keep current selection
                                }
                            }
                        )
                    }
                }
            }

            // Show day selector only when CUSTOM is selected
            if (selectedRepeatMode == RepeatMode.CUSTOM) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val dayLabels = listOf(
                        stringResource(R.string.day_mon_short),
                        stringResource(R.string.day_tue_short),
                        stringResource(R.string.day_wed_short),
                        stringResource(R.string.day_thu_short),
                        stringResource(R.string.day_fri_short),
                        stringResource(R.string.day_sat_short),
                        stringResource(R.string.day_sun_short)
                    )
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ringtone selector
            Text(
                text = stringResource(R.string.ringtone),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = RingtoneManager.ACTION_RINGTONE_PICKER
                        val pickerIntent = android.content.Intent(intent).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.select_ringtone))
                            ringtoneUri?.let {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                            }
                        }
                        ringtoneLauncher.launch(pickerIntent)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.alarm_ringtone), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (ringtoneUri != null) stringResource(R.string.ringtone_selected) else stringResource(R.string.ringtone_default),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced settings button
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) stringResource(R.string.hide_advanced) else stringResource(R.string.show_advanced))
            }

            if (showAdvanced) {
                Spacer(modifier = Modifier.height(16.dp))

                // Ring mode selector
                Text(
                    text = stringResource(R.string.ring_mode),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    RingMode.entries.filter { it != RingMode.LIVE_ACTIVITY }.forEach { mode ->
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
                                    RingMode.FULL_SCREEN -> stringResource(R.string.ring_mode_fullscreen)
                                    RingMode.NOTIFICATION -> stringResource(R.string.ring_mode_notification)
                                    else -> mode.name
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vibrate settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.vibrate), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = vibrateEnabled,
                        onCheckedChange = { vibrateEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ring duration settings
                Text(
                    text = stringResource(R.string.ring_duration_format, ringDurationMinutes),
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = ringDurationMinutes.toFloat(),
                    onValueChange = { ringDurationMinutes = it.toInt() },
                    valueRange = 1f..30f,
                    steps = 29,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Crescendo settings
                Text(
                    text = stringResource(R.string.crescendo_format, crescendoSeconds),
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = crescendoSeconds.toFloat(),
                    onValueChange = { crescendoSeconds = it.toInt() },
                    valueRange = 0f..60f,
                    steps = 59,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Skip holidays settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.skip_holidays), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = skipHolidays,
                        onCheckedChange = { skipHolidays = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Snooze settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.snooze_enabled), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = snoozeEnabled,
                        onCheckedChange = { snoozeEnabled = it }
                    )
                }

                if (snoozeEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.snooze_duration_format, snoozeMinutes),
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

@Composable
private fun formatOffset(minutes: Int): String {
    return when {
        minutes > 0 -> stringResource(R.string.offset_delayed, minutes)
        minutes < 0 -> stringResource(R.string.offset_ahead, -minutes)
        else -> stringResource(R.string.offset_none)
    }
}
