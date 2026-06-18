package com.snuabar.sunrisesunsetalarm.util

import android.content.Context
import android.content.SharedPreferences
import com.snuabar.sunrisesunsetalarm.data.model.DarkMode
import androidx.core.content.edit

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var darkMode: DarkMode
        get() = try {
            DarkMode.valueOf(prefs.getString(KEY_DARK_MODE, DarkMode.SYSTEM.name) ?: DarkMode.SYSTEM.name)
        } catch (_: Exception) {
            DarkMode.SYSTEM
        }
        set(value) = prefs.edit { putString(KEY_DARK_MODE, value.name) }

    var lastCalibratedAt: Long
        get() = prefs.getLong(KEY_LAST_CALIBRATED, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_CALIBRATED, value) }

    var currentLocationName: String
        get() = prefs.getString(KEY_LOCATION_NAME, "北京市") ?: "北京市"
        set(value) = prefs.edit { putString(KEY_LOCATION_NAME, value) }

    var currentLatitude: Double
        get() = prefs.getString(KEY_LATITUDE, "39.9042")?.toDoubleOrNull() ?: 39.9042
        set(value) = prefs.edit { putString(KEY_LATITUDE, value.toString()) }

    var currentLongitude: Double
        get() = prefs.getString(KEY_LONGITUDE, "116.4074")?.toDoubleOrNull() ?: 116.4074
        set(value) = prefs.edit { putString(KEY_LONGITUDE, value.toString()) }

    companion object {
        private const val PREFS_NAME = "sunrise_sunset_alarm_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LAST_CALIBRATED = "last_calibrated"
        private const val KEY_LOCATION_NAME = "location_name"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
    }
}
