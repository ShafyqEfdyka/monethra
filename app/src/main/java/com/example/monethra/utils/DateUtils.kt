package com.example.monethra.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val indonesianLocale = Locale.forLanguageTag("id-ID")

    fun formatDate(epochMillis: Long, pattern: String = "dd MMM yyyy"): String {
        val sdf = SimpleDateFormat(pattern, indonesianLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatMonthYear(epochMillis: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatMonthYear(month: Int, year: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", indonesianLocale)
        return sdf.format(calendar.time)
    }

    fun getStartOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar month is 0-indexed
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun getCurrentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH) + 1
    }

    fun getCurrentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    fun getStartOfCurrentMonth(): Long {
        return getStartOfMonth(getCurrentYear(), getCurrentMonth())
    }

    fun getEndOfCurrentMonth(): Long {
        return getEndOfMonth(getCurrentYear(), getCurrentMonth())
    }

    fun getMonthName(month: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
        }
        val sdf = SimpleDateFormat("MMMM", indonesianLocale)
        return sdf.format(calendar.time)
    }

    fun getPaydayCycleStartAndEnd(month: Int, year: Int, paydayStartDay: Int): Pair<Long, Long> {
        if (paydayStartDay <= 1) {
            return Pair(getStartOfMonth(year, month), getEndOfMonth(year, month))
        }

        val prevMonth = if (month == 1) 12 else month - 1
        val prevYear = if (month == 1) year - 1 else year

        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, prevYear)
            set(Calendar.MONTH, prevMonth - 1)
            set(Calendar.DAY_OF_MONTH, paydayStartDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, paydayStartDay - 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    fun getPaydayCycleLabel(month: Int, year: Int, paydayStartDay: Int): String {
        if (paydayStartDay <= 1) {
            val start = getStartOfMonth(year, month)
            val end = getEndOfMonth(year, month)
            val startStr = formatDate(start, "dd MMM")
            val endStr = formatDate(end, "dd MMM yyyy")
            return "$startStr - $endStr"
        }

        val (startMillis, endMillis) = getPaydayCycleStartAndEnd(month, year, paydayStartDay)

        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        val startYear = startCal.get(Calendar.YEAR)
        val endYear = endCal.get(Calendar.YEAR)

        val startPattern = if (startYear != endYear) "dd MMM yyyy" else "dd MMM"
        val startStr = formatDate(startMillis, startPattern)
        val endStr = formatDate(endMillis, "dd MMM yyyy")

        return "$startStr - $endStr"
    }
}
