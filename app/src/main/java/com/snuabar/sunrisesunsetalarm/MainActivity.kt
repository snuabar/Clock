package com.snuabar.sunrisesunsetalarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.snuabar.sunrisesunsetalarm.data.model.DarkMode
import com.snuabar.sunrisesunsetalarm.ui.screens.HomeScreen
import com.snuabar.sunrisesunsetalarm.ui.theme.SunriseSunsetAlarmTheme
import com.snuabar.sunrisesunsetalarm.util.SettingsManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsManager = SettingsManager(this)
        setContent {
            var darkMode by remember { mutableStateOf(settingsManager.darkMode) }
            SunriseSunsetAlarmTheme(darkMode = darkMode) {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        settingsManager = settingsManager,
                        onThemeChange = { newMode ->
                            darkMode = newMode
                            settingsManager.darkMode = newMode
                        }
                    )
                }
            }
        }
    }
}
