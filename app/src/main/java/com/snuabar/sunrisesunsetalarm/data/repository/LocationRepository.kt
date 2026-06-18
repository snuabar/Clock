package com.snuabar.sunrisesunsetalarm.data.repository

import com.snuabar.sunrisesunsetalarm.data.database.LocationDao
import com.snuabar.sunrisesunsetalarm.data.model.Location
import kotlinx.coroutines.flow.Flow

class LocationRepository(private val locationDao: LocationDao) {
    fun getAllLocations(): Flow<List<Location>> = locationDao.getAllLocations()

    suspend fun getLocationById(id: String): Location? = locationDao.getLocationById(id)

    suspend fun insertLocation(location: Location) = locationDao.insertLocation(location)

    suspend fun updateLocation(location: Location) = locationDao.updateLocation(location)

    suspend fun deleteLocation(location: Location) = locationDao.deleteLocation(location)
}
