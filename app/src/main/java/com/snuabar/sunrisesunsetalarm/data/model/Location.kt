package com.snuabar.sunrisesunsetalarm.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class Location(
    @PrimaryKey
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isAutoLocated: Boolean = false,
    val lastCalibratedAt: Long? = null
)
