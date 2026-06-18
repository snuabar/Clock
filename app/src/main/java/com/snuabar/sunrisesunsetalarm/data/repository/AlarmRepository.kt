package com.snuabar.sunrisesunsetalarm.data.repository

import com.snuabar.sunrisesunsetalarm.data.database.AlarmDao
import com.snuabar.sunrisesunsetalarm.data.model.Alarm
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {
    fun getAllAlarms(): Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun getEnabledAlarms(): List<Alarm> = alarmDao.getEnabledAlarms()

    suspend fun getAlarmById(id: String): Alarm? = alarmDao.getAlarmById(id)

    suspend fun insertAlarm(alarm: Alarm) = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: Alarm) = alarmDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = alarmDao.deleteAlarm(alarm)

    suspend fun deleteAlarmById(id: String) = alarmDao.deleteAlarmById(id)
}
