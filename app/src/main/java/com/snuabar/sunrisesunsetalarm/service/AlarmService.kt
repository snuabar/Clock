package com.snuabar.sunrisesunsetalarm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.snuabar.sunrisesunsetalarm.MainActivity
import com.snuabar.sunrisesunsetalarm.R
import com.snuabar.sunrisesunsetalarm.ui.screens.FullScreenAlarmActivity

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var crescendoRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getStringExtra("alarm_id") ?: return START_NOT_STICKY
        val alarmName = intent.getStringExtra("alarm_name") ?: "Alarm"
        val ringMode = intent.getStringExtra("ring_mode") ?: "FULL_SCREEN"
        val ringtoneUri = intent.getStringExtra("ringtone_uri")
        val vibrateEnabled = intent.getBooleanExtra("vibrate_enabled", true)
        val ringDurationMinutes = intent.getIntExtra("ring_duration_minutes", 5)
        val crescendoSeconds = intent.getIntExtra("crescendo_seconds", 0)

        // Stop any previous alarm state before starting new one (prevents overlapping)
        stopAlarm()

        val notification = buildForegroundNotification(alarmName, alarmId)
        startForeground(NOTIFICATION_ID, notification)

        // Handle alarm based on ring mode
        when (ringMode) {
            "FULL_SCREEN" -> {
                startRingtone(ringtoneUri, crescendoSeconds)
                if (vibrateEnabled) startVibration()
                showFullScreenAlarm(alarmId, alarmName)
            }
            "NOTIFICATION" -> {
                startRingtone(ringtoneUri, crescendoSeconds)
                if (vibrateEnabled) startVibration()
                // Only notification, no full-screen activity
            }
            "LIVE_ACTIVITY" -> {
                // Live Activity / heads-up notification: ring + vibrate, no full-screen
                startRingtone(ringtoneUri, crescendoSeconds)
                if (vibrateEnabled) startVibration()
                // Notification is already shown via startForeground with heads-up priority
            }
        }

        // Schedule auto-stop after ring duration
        scheduleAutoStop(ringDurationMinutes)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for alarm triggers"
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(alarmName: String, alarmId: String): android.app.Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(this, AlarmDismissReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("闹钟响了")
            .setContentText(alarmName)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.BigTextStyle().bigText("闹钟 $alarmName 正在响铃"))
            .addAction(R.drawable.ic_launcher_foreground, "关闭", dismissIntent)
            .build()
    }

    private fun showFullScreenAlarm(alarmId: String, alarmName: String) {
        val fullScreenIntent = Intent(this, FullScreenAlarmActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_name", alarmName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(fullScreenIntent)
    }

    private fun startRingtone(ringtoneUri: String?, crescendoSeconds: Int) {
        try {
            val alarmUri: Uri = if (ringtoneUri != null) {
                Uri.parse(ringtoneUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alarmUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                isLooping = true
                prepare()

                if (crescendoSeconds > 0) {
                    // Gradually increase volume from 0 to 1
                    setVolume(0f, 0f)
                    start()
                    startCrescendo(crescendoSeconds)
                } else {
                    setVolume(1f, 1f)
                    start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startCrescendo(crescendoSeconds: Int) {
        val totalSteps = crescendoSeconds * 10 // Update every 100ms
        var currentStep = 0

        val runnable = object : Runnable {
            override fun run() {
                currentStep++
                val volume = currentStep.toFloat() / totalSteps.toFloat()
                val clampedVolume = volume.coerceIn(0f, 1f)
                mediaPlayer?.setVolume(clampedVolume, clampedVolume)

                if (currentStep < totalSteps) {
                    handler.postDelayed(this, 100)
                }
            }
        }

        crescendoRunnable = runnable
        handler.postDelayed(runnable, 100)
    }

    private fun startVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 1000, 500), 0)
        }
    }

    private fun stopAlarm() {
        crescendoRunnable?.let { handler.removeCallbacks(it) }
        crescendoRunnable = null

        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun scheduleAutoStop(ringDurationMinutes: Int) {
        // Cancel any existing timeout
        timeoutRunnable?.let { handler.removeCallbacks(it) }

        val durationMillis = ringDurationMinutes * 60 * 1000L
        val runnable = Runnable {
            stopAlarm()
            stopSelf()
        }
        timeoutRunnable = runnable
        handler.postDelayed(runnable, durationMillis)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
