package com.example.monethra.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun getStartAndEndOfMonth_returnsValidTimestamps() {
        val start = DateUtils.getStartOfMonth(2026, 5)
        val end = DateUtils.getEndOfMonth(2026, 5)
        assertTrue(end > start)
    }

    @Test
    fun getCurrentMonthAndYear_returnsValidValues() {
        val month = DateUtils.getCurrentMonth()
        val year = DateUtils.getCurrentYear()
        assertTrue(month in 1..12)
        assertTrue(year >= 2025)
    }

    @Test
    fun getPaydayCycleStartAndEnd_payday1_returnsCalendarMonthStartAndEnd() {
        val (start, end) = DateUtils.getPaydayCycleStartAndEnd(month = 11, year = 2024, paydayStartDay = 1)
        val expectedStart = DateUtils.getStartOfMonth(2024, 11)
        val expectedEnd = DateUtils.getEndOfMonth(2024, 11)
        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
    }

    @Test
    fun getPaydayCycleStartAndEnd_payday25_returnsPreviousMonth25ToCurrentMonth24() {
        val (start, end) = DateUtils.getPaydayCycleStartAndEnd(month = 11, year = 2024, paydayStartDay = 25)

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(2024, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH)) // Prev month (Oct)
        assertEquals(25, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        assertEquals(2024, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, endCal.get(Calendar.MONTH)) // Current month (Nov)
        assertEquals(24, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
    }

    @Test
    fun getPaydayCycleStartAndEnd_januaryPayday25_handlesPreviousYearDecember() {
        val (start, end) = DateUtils.getPaydayCycleStartAndEnd(month = 1, year = 2025, paydayStartDay = 25)

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(2024, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, startCal.get(Calendar.MONTH))
        assertEquals(25, startCal.get(Calendar.DAY_OF_MONTH))

        assertEquals(2025, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, endCal.get(Calendar.MONTH))
        assertEquals(24, endCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun getPaydayCycleLabel_payday25_returnsFormattedRange() {
        val label = DateUtils.getPaydayCycleLabel(month = 11, year = 2024, paydayStartDay = 25)
        assertTrue(label.contains("25"))
        assertTrue(label.contains("24"))
        assertTrue(label.contains("2024"))
    }
}
