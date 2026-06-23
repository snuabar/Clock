package com.snuabar.sunrisesunsetalarm.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snuabar.sunrisesunsetalarm.service.AlarmService
import androidx.compose.ui.res.stringResource
import com.snuabar.sunrisesunsetalarm.R
import com.snuabar.sunrisesunsetalarm.service.SnoozeHelper
import com.snuabar.sunrisesunsetalarm.ui.theme.SunriseSunsetAlarmTheme

class FullScreenAlarmActivity : ComponentActivity() {

    private var ringtone: android.media.Ringtone? = null
    private var vibrator: Vibrator? = null
    private var alarmId: String = ""
    private var alarmName: String = "闹钟"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        alarmId = intent.getStringExtra("alarm_id") ?: ""
        alarmName = intent.getStringExtra("alarm_name") ?: getString(R.string.default_alarm_name_short)

        startRingtone()
        startVibration()

        setContent {
            SunriseSunsetAlarmTheme {
                FullScreenAlarmContent(
                    alarmName = alarmName,
                    onDismiss = { dismissAlarm() },
                    onSnooze = { snoozeAlarm() }
                )
            }
        }
    }

    private fun startRingtone() {
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(this, alarmUri)
        ringtone?.play()
    }

    private fun startVibration() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 1000, 500), 0)
        }
    }

    private fun dismissAlarm() {
        stopAlarm()
        // Also stop the background AlarmService
        stopService(Intent(this, AlarmService::class.java))
        finish()
    }

    private fun snoozeAlarm() {
        stopAlarm()
        // Also stop the background AlarmService before snoozing
        stopService(Intent(this, com.snuabar.sunrisesunsetalarm.service.AlarmService::class.java))
        SnoozeHelper(this).snoozeAlarm(alarmId, alarmName, 5)
        finish()
    }

    private fun stopAlarm() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}

@Composable
private fun FullScreenAlarmContent(
    alarmName: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = alarmName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date()),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Snooze button
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Snooze, contentDescription = stringResource(R.string.cd_snooze))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.snooze_format))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dismiss button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.dismiss_alarm))
            }
        }
    }
}
