package com.example.monethra.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun formatIdr_formatsPositiveAmountCorrectly() {
        val result = CurrencyFormatter.formatIdr(1500000L)
        // Checks that formatted output starts with Rp and contains 1.500.000 or 1,500,000 depending on locale
        assertEquals("Rp1.500.000", result)
    }

    @Test
    fun formatIdr_formatsZeroCorrectly() {
        val result = CurrencyFormatter.formatIdr(0L)
        assertEquals("Rp0", result)
    }

    @Test
    fun parseAmount_cleansNumberInput() {
        val result = CurrencyFormatter.parseAmount("Rp1.500.000")
        assertEquals(1500000L, result)
    }
}
