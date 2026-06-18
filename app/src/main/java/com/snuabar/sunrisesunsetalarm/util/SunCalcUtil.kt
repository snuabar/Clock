package com.snuabar.sunrisesunsetalarm.util

import java.util.*
import kotlin.math.*

object SunCalcUtil {

    data class SunTimes(
        val sunrise: Long,
        val sunset: Long,
        val solarNoon: Long
    )

    /**
     * Calculate sunrise and sunset times for a given date and location.
     * Uses the NOAA Solar Calculations simplified algorithm.
     * Results are returned in local time.
     */
    fun calculateSunTimes(
        date: Calendar,
        latitude: Double,
        longitude: Double
    ): SunTimes {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH)

        // Calculate sunrise and sunset in UTC hours (0-24)
        val sunriseUTC = calculateEventTime(year, month, day, latitude, longitude, true)
        val sunsetUTC = calculateEventTime(year, month, day, latitude, longitude, false)

        // Convert UTC hours to local Calendar instances
        val sunriseCal = utcHoursToLocalCalendar(year, month, day, sunriseUTC)
        val sunsetCal = utcHoursToLocalCalendar(year, month, day, sunsetUTC)

        // Solar noon is halfway between sunrise and sunset
        val noonMillis = (sunriseCal.timeInMillis + sunsetCal.timeInMillis) / 2
        val noonCal = Calendar.getInstance().apply { timeInMillis = noonMillis }

        return SunTimes(
            sunrise = sunriseCal.timeInMillis,
            sunset = sunsetCal.timeInMillis,
            solarNoon = noonCal.timeInMillis
        )
    }

    private fun calculateEventTime(
        year: Int, month: Int, day: Int,
        latitude: Double, longitude: Double,
        isSunrise: Boolean
    ): Double {
        val zenith = 90.8333

        // 1. Calculate day of year
        val n1 = floor(275.0 * month / 9.0)
        val n2 = floor((month + 9.0) / 12.0)
        val n3 = (1 + floor((year - 4 * floor(year / 4.0) + 3) / 4.0))
        val n = (n1 - n2 * n3 + day - 30).toInt()

        // 2. Convert longitude to hour value
        val lngHour = longitude / 15.0

        // 3. Calculate approximate time (6am for sunrise, 18:00 for sunset)
        val t = if (isSunrise) {
            n + ((6.0 - lngHour) / 24.0)
        } else {
            n + ((18.0 - lngHour) / 24.0)
        }

        // 4. Calculate mean anomaly
        val m = (0.9856 * t) - 3.289

        // 5. Calculate true longitude
        val l = normalizeDegree(m + 1.916 * sin(degToRad(m)) + 0.020 * sin(degToRad(2 * m)) + 282.634)

        // 6. Calculate right ascension
        var ra = normalizeDegree(radToDeg(atan(0.91764 * tan(degToRad(l)))))

        // 7. Adjust for quadrant
        val lQuadrant = floor(l / 90.0) * 90
        val raQuadrant = floor(ra / 90.0) * 90
        ra = (ra + (lQuadrant - raQuadrant)) / 15.0

        // 8. Calculate declination
        val sinDec = 0.39782 * sin(degToRad(l))
        val cosDec = cos(asin(sinDec))

        // 9. Calculate hour angle
        val cosH = (cos(degToRad(zenith)) - (sinDec * sin(degToRad(latitude)))) /
                (cosDec * cos(degToRad(latitude)))

        // Check for polar day/night
        if (cosH < -1.0 || cosH > 1.0) {
            return if (isSunrise) 0.0 else 23.99
        }

        // 10. Calculate local hour angle
        val h = if (isSunrise) {
            (360.0 - radToDeg(acos(cosH))) / 15.0
        } else {
            radToDeg(acos(cosH)) / 15.0
        }

        // 11. Calculate local mean time
        val tLocal = h + ra - (0.06571 * t) - 6.622

        // 12. Adjust back to UTC
        val utcHours = (tLocal - lngHour) % 24.0
        return if (utcHours < 0) utcHours + 24.0 else utcHours
    }

    /**
     * Convert UTC hours to a local Calendar instance.
     */
    private fun utcHoursToLocalCalendar(year: Int, month: Int, day: Int, utcHours: Double): Calendar {
        val safeHours = utcHours % 24.0
        val hour = safeHours.toInt()
        val minute = ((safeHours - hour) * 60).toInt()

        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCal.set(year, month - 1, day, hour.coerceIn(0, 23), minute.coerceIn(0, 59), 0)

        // Convert to local time
        val localCal = Calendar.getInstance()
        localCal.timeInMillis = utcCal.timeInMillis
        return localCal
    }

    private fun degToRad(deg: Double): Double = deg * (PI / 180.0)
    private fun radToDeg(rad: Double): Double = rad * (180.0 / PI)

    private fun normalizeDegree(degree: Double): Double {
        var result = degree % 360.0
        if (result < 0) result += 360.0
        return result
    }
}
