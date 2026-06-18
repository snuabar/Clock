package com.snuabar.sunrisesunsetalarm.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * 节假日数据管理器。
 *
 * 使用策略：在 Application.onCreate 中调用 [preload] 预加载当年节假日数据，
 * 后续通过 [isHoliday] 同步查询内存缓存。若缓存未命中则 fallback 到硬编码数据。
 *
 * 数据来源：https://github.com/NateScarlet/holiday-cn （国务院节假日数据）
 */
object HolidayUtil {

    private const val TAG = "HolidayUtil"
    private const val PREF_NAME = "holiday_cache"
    private const val KEY_HOLIDAYS = "cached_holidays"
    private const val KEY_YEAR = "cached_year"
    // jsDelivr CDN 加速，国内访问更稳定
    private const val API_BASE = "https://cdn.jsdelivr.net/gh/NateScarlet/holiday-cn@master/"

    @Volatile
    private var memoryCache: Set<String>? = null
    private var cachedYear: Int = 0

    private val gson = Gson()

    /**
     * 预加载节假日数据。应在 Application.onCreate 中通过协程调用。
     * 优先读取本地缓存，缓存未命中再发网络请求，最后 fallback 到硬编码。
     */
    suspend fun preload(context: Context) {
        val year = Calendar.getInstance().get(Calendar.YEAR)

        // 1. 内存缓存已存在且年份匹配
        if (memoryCache != null && cachedYear == year) {
            return
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // 2. 尝试从 SharedPreferences 恢复
        val local = loadFromPrefs(prefs, year)
        if (local != null) {
            memoryCache = local
            cachedYear = year
            Log.d(TAG, "Loaded holidays from local cache for year $year")
            return
        }

        // 3. 从网络获取
        val networkHolidays = fetchFromNetwork(year)
        if (networkHolidays != null) {
            memoryCache = networkHolidays
            cachedYear = year
            saveToPrefs(prefs, year, networkHolidays)
            Log.d(TAG, "Loaded holidays from network for year $year, count=${networkHolidays.size}")
            return
        }

        // 4. Fallback 到硬编码
        val embedded = getEmbeddedHolidays(year)
        memoryCache = embedded
        cachedYear = year
        Log.w(TAG, "Using embedded holiday data for year $year")
    }

    /**
     * 同步判断某天是否为节假日。调用前需确保 [preload] 已执行。
     */
    fun isHoliday(calendar: Calendar): Boolean {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dateString = String.format("%04d-%02d-%02d", year, month, day)

        // 优先使用内存缓存
        val cache = memoryCache
        if (cache != null && cachedYear == year) {
            return cache.contains(dateString)
        }

        // Fallback 到硬编码
        return getEmbeddedHolidays(year).contains(dateString)
    }

    // region 网络请求

    private suspend fun fetchFromNetwork(year: Int): Set<String>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("${API_BASE}${year}.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseHolidayResponse(response)
            } else {
                Log.w(TAG, "Holiday API returned HTTP $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch holidays from network: ${e.message}")
            null
        }
    }

    /**
     * holiday-cn 返回格式：
     * {
     *   "year": 2026,
     *   "days": [
     *     {"name": "元旦", "date": "2026-01-01", "isOffDay": true},
     *     ...
     *   ]
     * }
     */
    private fun parseHolidayResponse(json: String): Set<String>? {
        return try {
            val type = object : TypeToken<HolidayApiResponse>() {}.type
            val response = gson.fromJson<HolidayApiResponse>(json, type)

            response.days
                ?.filter { it.isOffDay == true }
                ?.mapNotNull { it.date }
                ?.toSet()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse holiday response: ${e.message}")
            null
        }
    }

    // endregion

    // region 本地缓存

    private fun loadFromPrefs(prefs: SharedPreferences, year: Int): Set<String>? {
        val cachedYear = prefs.getInt(KEY_YEAR, 0)
        if (cachedYear != year) return null

        val json = prefs.getString(KEY_HOLIDAYS, null) ?: return null
        return try {
            gson.fromJson<Set<String>>(json, object : TypeToken<Set<String>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveToPrefs(prefs: SharedPreferences, year: Int, holidays: Set<String>) {
        prefs.edit()
            .putInt(KEY_YEAR, year)
            .putString(KEY_HOLIDAYS, gson.toJson(holidays))
            .apply()
    }

    // endregion

    // region 硬编码 fallback

    /**
     * 获取指定年份的硬编码节假日数据。作为网络不可用时的兜底方案。
     */
    fun getEmbeddedHolidays(year: Int): Set<String> {
        return when (year) {
            2024 -> setOf(
                "2024-01-01", "2024-02-10", "2024-02-11", "2024-02-12", "2024-02-13", "2024-02-14", "2024-02-15", "2024-02-16",
                "2024-04-04", "2024-04-05", "2024-04-06",
                "2024-05-01", "2024-05-02", "2024-05-03", "2024-05-04", "2024-05-05",
                "2024-06-10",
                "2024-09-15", "2024-09-16", "2024-09-17",
                "2024-10-01", "2024-10-02", "2024-10-03", "2024-10-04", "2024-10-05", "2024-10-06", "2024-10-07"
            )
            2025 -> setOf(
                "2025-01-01",
                "2025-01-28", "2025-01-29", "2025-01-30", "2025-01-31", "2025-02-01", "2025-02-02", "2025-02-03", "2025-02-04",
                "2025-04-04", "2025-04-05", "2025-04-06",
                "2025-05-01", "2025-05-02", "2025-05-03", "2025-05-04", "2025-05-05",
                "2025-05-31",
                "2025-10-01", "2025-10-02", "2025-10-03", "2025-10-04", "2025-10-05", "2025-10-06", "2025-10-07", "2025-10-08"
            )
            2026 -> setOf(
                "2026-01-01",
                "2026-02-17", "2026-02-18", "2026-02-19", "2026-02-20", "2026-02-21", "2026-02-22", "2026-02-23",
                "2026-04-04", "2026-04-05", "2026-04-06",
                "2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05",
                "2026-06-19",
                "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04", "2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08"
            )
            else -> emptySet()
        }
    }

    // endregion

    // region 数据类

    data class HolidayApiResponse(
        val year: Int?,
        val days: List<HolidayDay>?
    )

    data class HolidayDay(
        val name: String?,
        val date: String?,      // "2026-01-01"
        val isOffDay: Boolean?  // true 表示放假
    )

    // endregion
}
