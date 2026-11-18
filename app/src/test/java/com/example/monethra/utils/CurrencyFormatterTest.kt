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

    @Test
    fun format_formatsUsdWithTwoDecimals() {
        // 1,800,000 IDR with rate 0.000062 = 111.60 USD
        val result = CurrencyFormatter.format(
            amountInIdr = 1800000L,
            currencyCode = "USD",
            currencySymbol = "$",
            rateFromIdr = 0.000062
        )
        assertEquals("$111.60", result)
    }

    @Test
    fun format_formatsMyrWithTwoDecimals() {
        // 1,800,000 IDR with rate 0.00028 = 504.00 MYR
        val result = CurrencyFormatter.format(
            amountInIdr = 1800000L,
            currencyCode = "MYR",
            currencySymbol = "RM",
            rateFromIdr = 0.00028
        )
        assertEquals("RM504.00", result)
    }

    @Test
    fun format_formatsJpyWithZeroDecimals() {
        // 100,000 IDR with rate 0.0096 = 960 JPY
        val result = CurrencyFormatter.format(
            amountInIdr = 100000L,
            currencyCode = "JPY",
            currencySymbol = "¥",
            rateFromIdr = 0.0096
        )
        assertEquals("¥960", result)
    }

    @Test
    fun convertToIdr_and_convertFromIdr_areConsistent() {
        val rate = 0.000062 // 1 IDR = 0.000062 USD
        val amountInIdr = 1612903L
        val converted = CurrencyFormatter.convertFromIdr(amountInIdr, rate) // ~100.0
        val backToIdr = CurrencyFormatter.convertToIdr(converted, rate)
        assertEquals(amountInIdr, backToIdr)
    }
}
