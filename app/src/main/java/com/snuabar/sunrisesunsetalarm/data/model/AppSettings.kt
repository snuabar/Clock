package com.snuabar.sunrisesunsetalarm.data.model

data class AppSettings(
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val currentLocationId: String = "",
    val lastNetworkSyncAt: Long? = null
)

enum class DarkMode {
    SYSTEM, LIGHT, DARK
}
