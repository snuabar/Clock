package com.snuabar.sunrisesunsetalarm.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.snuabar.sunrisesunsetalarm.R
import com.snuabar.sunrisesunsetalarm.SunriseSunsetApplication
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.BaseType
import com.snuabar.sunrisesunsetalarm.data.model.DarkMode
import com.snuabar.sunrisesunsetalarm.data.model.RepeatMode
import com.snuabar.sunrisesunsetalarm.service.AlarmManagerHelper
import com.snuabar.sunrisesunsetalarm.ui.components.AddAlarmBottomSheet
import com.snuabar.sunrisesunsetalarm.ui.components.AlarmCard
import com.snuabar.sunrisesunsetalarm.ui.components.LocationPicker
import com.snuabar.sunrisesunsetalarm.util.SettingsManager
import com.snuabar.sunrisesunsetalarm.util.SunCalcUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(settingsManager: SettingsManager, onThemeChange: (DarkMode) -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var darkMode by remember { mutableStateOf(settingsManager.darkMode) }
    var currentLocation by remember { mutableStateOf(settingsManager.currentLocationName) }
    var currentLat by remember { mutableStateOf(settingsManager.currentLatitude) }
    var currentLng by remember { mutableStateOf(settingsManager.currentLongitude) }

    val sunTimes = remember(currentLat, currentLng) {
        try {
            SunCalcUtil.calculateSunTimes(Calendar.getInstance(), currentLat, currentLng)
        } catch (_: Exception) { null }
    }
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val sunriseTime = sunTimes?.let { fmt.format(Date(it.sunrise)) } ?: "05:23"
    val sunsetTime = sunTimes?.let { fmt.format(Date(it.sunset)) } ?: "19:45"

    val alarmRepository = remember { (context.applicationContext as SunriseSunsetApplication).alarmRepository }
    val alarms by alarmRepository.getAllAlarms().collectAsStateWithLifecycle(initialValue = emptyList())

    // Initialize default alarms on first launch only
    LaunchedEffect(Unit) {
        if (!settingsManager.hasInitializedDemoAlarms && alarms.isEmpty()) {
            alarmRepository.insertAlarm(
                Alarm("1", "晨间唤醒", BaseType.SUNRISE, 0, "true,true,true,true,true,true,true", repeatMode = RepeatMode.DAILY)
            )
            alarmRepository.insertAlarm(
                Alarm("2", "日落提醒", BaseType.SUNSET, 0, "true,true,true,true,true,true,true", repeatMode = RepeatMode.DAILY)
            )
            settingsManager.hasInitializedDemoAlarms = true
        }
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Track which alarm card is currently expanded for swipe-delete
    var expandedAlarmId by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    val alarmManagerHelper = remember { AlarmManagerHelper(context) }

    // Auto-collapse when scrolling
    if (listState.isScrollInProgress && expandedAlarmId != null) {
        expandedAlarmId = null
    }

    // Check if exact alarm permission is granted (Android 12+)
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    // Prompt user to enable exact alarm permission
    suspend fun requestExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.snackbar_exact_alarm_permission),
                actionLabel = context.getString(R.string.action_go_to_settings),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
            return false
        }
        return true
    }

    var lastCalibratedAt by remember { mutableStateOf(settingsManager.lastCalibratedAt) }
    val calibrationText = if (lastCalibratedAt == 0L) stringResource(R.string.calibration_none)
    else stringResource(R.string.calibration_last, SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()).format(Date(lastCalibratedAt)))

    // Track recently deleted alarm for undo
    var recentlyDeletedAlarm by remember { mutableStateOf<Alarm?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snackbar_location_permission)) }
        }
    }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.snackbar_notification_permission))
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.top_bar_title)) },
                actions = {
                    IconButton(onClick = { expandedAlarmId = null; showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { expandedAlarmId = null; showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_alarm))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SunTimesHeader(
                locationName = currentLocation,
                sunriseTime = sunriseTime,
                sunsetTime = sunsetTime,
                onLocationClick = { expandedAlarmId = null; showLocationPicker = true }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        latitude = currentLat,
                        longitude = currentLng,
                        isExpanded = expandedAlarmId == alarm.id,
                        onExpandChanged = { expanded ->
                            expandedAlarmId = if (expanded) alarm.id else null
                        },
                        onToggle = { isEnabled ->
                            coroutineScope.launch {
                                alarmRepository.updateAlarm(alarm.copy(isEnabled = isEnabled))
                                if (isEnabled) {
                                    if (requestExactAlarmPermission()) {
                                        alarmManagerHelper.scheduleAlarm(alarm.copy(isEnabled = true), currentLat, currentLng)
                                    }
                                } else {
                                    alarmManagerHelper.cancelAlarm(alarm)
                                }
                            }
                        },
                        onClick = { editingAlarm = alarm },
                        onDelete = {
                            recentlyDeletedAlarm = alarm
                            coroutineScope.launch {
                                alarmManagerHelper.cancelAlarm(alarm)
                                alarmRepository.deleteAlarm(alarm)
                                val result = snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.snackbar_alarm_deleted),
                                    actionLabel = context.getString(R.string.action_undo),
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    recentlyDeletedAlarm?.let { deletedAlarm ->
                                        alarmRepository.insertAlarm(deletedAlarm)
                                        if (deletedAlarm.isEnabled) {
                                            alarmManagerHelper.scheduleAlarm(deletedAlarm, currentLat, currentLng)
                                        }
                                    }
                                }
                                recentlyDeletedAlarm = null
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddSheet || editingAlarm != null) {
        AddAlarmBottomSheet(
            alarm = editingAlarm,
            onDismiss = { showAddSheet = false; editingAlarm = null },
            onSave = { alarm ->
                coroutineScope.launch {
                    if (editingAlarm != null) {
                        alarmManagerHelper.cancelAlarm(editingAlarm!!)
                        alarmRepository.updateAlarm(alarm)
                    } else {
                        alarmRepository.insertAlarm(alarm)
                    }
                    if (alarm.isEnabled) {
                        if (requestExactAlarmPermission()) {
                            alarmManagerHelper.scheduleAlarm(alarm, currentLat, currentLng)
                        }
                    }
                }
                showAddSheet = false
                editingAlarm = null
            }
        )
    }

    if (showLocationPicker) {
        LocationPicker(
            onCitySelected = { city ->
                currentLocation = city.name
                currentLat = city.latitude
                currentLng = city.longitude
                settingsManager.currentLocationName = city.name
                settingsManager.currentLatitude = city.latitude
                settingsManager.currentLongitude = city.longitude
                showLocationPicker = false

                // Reschedule all enabled alarms with new location
                alarms.filter { it.isEnabled }.forEach { alarm ->
                    alarmManagerHelper.cancelAlarm(alarm)
                    alarmManagerHelper.scheduleAlarm(alarm, currentLat, currentLng)
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_location_changed))
                }
            },
            onDismiss = { showLocationPicker = false },
            onRequestLocationPermission = {
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        )
    }

    if (showSettings) {
        SettingsSheet(
            currentDarkMode = darkMode,
            currentLocation = currentLocation,
            calibrationText = calibrationText,
            onDarkModeChange = {
                darkMode = it
                settingsManager.darkMode = it
                onThemeChange(it)
            },
            onLocationClick = { showSettings = false; showLocationPicker = true },
            onCalibrate = {
                lastCalibratedAt = System.currentTimeMillis()
                settingsManager.lastCalibratedAt = lastCalibratedAt
                coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snackbar_calibration_done)) }
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun SunTimesHeader(locationName: String, sunriseTime: String, sunsetTime: String, onLocationClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { onLocationClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${dateFormat.format(Date())} $locationName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SunTimeItem(stringResource(R.string.label_sunrise), sunriseTime)
                SunTimeItem(stringResource(R.string.label_sunset), sunsetTime)
            }
        }
    }
}

@Composable
private fun SunTimeItem(label: String, time: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(time, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    currentDarkMode: DarkMode,
    currentLocation: String,
    calibrationText: String,
    onDarkModeChange: (DarkMode) -> Unit,
    onLocationClick: () -> Unit,
    onCalibrate: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(stringResource(R.string.dark_mode_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            DarkMode.entries.forEach { mode ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onDarkModeChange(mode) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = currentDarkMode == mode, onClick = { onDarkModeChange(mode) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(when (mode) {
                        DarkMode.SYSTEM -> stringResource(R.string.dark_mode_system)
                        DarkMode.LIGHT -> stringResource(R.string.dark_mode_light)
                        DarkMode.DARK -> stringResource(R.string.dark_mode_dark)
                    })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(stringResource(R.string.location_settings), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth().clickable { onLocationClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.current_city), style = MaterialTheme.typography.bodyMedium)
                        Text(currentLocation, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(stringResource(R.string.time_calibration), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(calibrationText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onCalibrate) { Text(stringResource(R.string.action_calibrate_now)) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(stringResource(R.string.about), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.version_format, "1.0.0"), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
