package com.snuabar.sunrisesunsetalarm.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snuabar.sunrisesunsetalarm.SunriseSunsetApplication
import com.snuabar.sunrisesunsetalarm.data.repository.AlarmRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import com.snuabar.sunrisesunsetalarm.data.model.BaseType
import com.snuabar.sunrisesunsetalarm.data.model.DarkMode
import com.snuabar.sunrisesunsetalarm.ui.components.AddAlarmBottomSheet
import com.snuabar.sunrisesunsetalarm.ui.components.AlarmCard
import com.snuabar.sunrisesunsetalarm.ui.components.LocationPicker
import com.snuabar.sunrisesunsetalarm.util.SettingsManager
import com.snuabar.sunrisesunsetalarm.service.AlarmManagerHelper
import com.snuabar.sunrisesunsetalarm.util.SunCalcUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    // Initialize default alarms on first launch
    LaunchedEffect(Unit) {
        if (alarms.isEmpty()) {
            alarmRepository.insertAlarm(
                Alarm("1", "晨间唤醒", BaseType.SUNRISE, -30, "true,true,true,true,true,false,false")
            )
            alarmRepository.insertAlarm(
                Alarm("2", "日落提醒", BaseType.SUNSET, 0, "true,true,true,true,true,true,true")
            )
        }
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var lastCalibratedAt by remember { mutableStateOf(settingsManager.lastCalibratedAt) }
    val calibrationText = remember(lastCalibratedAt) {
        if (lastCalibratedAt == 0L) "未校准"
        else "上次校准：${SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()).format(Date(lastCalibratedAt))}"
    }

    // Track recently deleted alarm for undo
    var recentlyDeletedAlarm by remember { mutableStateOf<Alarm?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it }) {
            coroutineScope.launch { snackbarHostState.showSnackbar("需要位置权限") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("日出日落闹钟") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加闹钟")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SunTimesHeader(
                locationName = currentLocation,
                sunriseTime = sunriseTime,
                sunsetTime = sunsetTime,
                onLocationClick = { showLocationPicker = true }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        latitude = currentLat,
                        longitude = currentLng,
                        onToggle = { isEnabled ->
                            coroutineScope.launch {
                                alarmRepository.updateAlarm(alarm.copy(isEnabled = isEnabled))
                            }
                        },
                        onClick = { editingAlarm = alarm },
                        onDelete = {
                            recentlyDeletedAlarm = alarm
                            coroutineScope.launch {
                                alarmRepository.deleteAlarm(alarm)
                                val result = snackbarHostState.showSnackbar(
                                    message = "已删除",
                                    actionLabel = "撤销",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    recentlyDeletedAlarm?.let { deletedAlarm ->
                                        alarmRepository.insertAlarm(deletedAlarm)
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
                        alarmRepository.updateAlarm(alarm)
                    } else {
                        alarmRepository.insertAlarm(alarm)
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
                val alarmManagerHelper = AlarmManagerHelper(context)
                alarms.filter { it.isEnabled }.forEach { alarm ->
                    alarmManagerHelper.cancelAlarm(alarm)
                    alarmManagerHelper.scheduleAlarm(alarm, currentLat, currentLng)
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("位置已切换，闹钟时间已更新")
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
                coroutineScope.launch { snackbarHostState.showSnackbar("校准完成") }
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
                SunTimeItem("日出", sunriseTime)
                SunTimeItem("日落", sunsetTime)
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
                Text("设置", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onDismiss) { Text("关闭") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("深色模式", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            DarkMode.entries.forEach { mode ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onDarkModeChange(mode) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = currentDarkMode == mode, onClick = { onDarkModeChange(mode) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(when (mode) {
                        DarkMode.SYSTEM -> "跟随系统"
                        DarkMode.LIGHT -> "浅色模式"
                        DarkMode.DARK -> "深色模式"
                    })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("位置设置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth().clickable { onLocationClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("当前城市", style = MaterialTheme.typography.bodyMedium)
                        Text(currentLocation, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("时间校准", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(calibrationText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onCalibrate) { Text("立即校准") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("关于", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("版本 1.0.0", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
