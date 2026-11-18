package com.example.monethra.utils

import androidx.compose.runtime.staticCompositionLocalOf
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

data class CurrencyConfig(
    val currencyCode: String = "IDR",
    val symbol: String = "Rp",
    val rateFromIdr: Double = 1.0
)

val LocalCurrencyConfig = staticCompositionLocalOf { CurrencyConfig() }

object CurrencyFormatter {

    @Volatile
    var currentConfig: CurrencyConfig = CurrencyConfig()

    fun format(
        amountInIdr: Long,
        config: CurrencyConfig = currentConfig
    ): String = format(
        amountInIdr = amountInIdr,
        currencyCode = config.currencyCode,
        currencySymbol = config.symbol,
        rateFromIdr = config.rateFromIdr
    )

    fun format(
        amountInIdr: Long,
        currencyCode: String = currentConfig.currencyCode,
        currencySymbol: String = currentConfig.symbol,
        rateFromIdr: Double = currentConfig.rateFromIdr
    ): String {
        val convertedAmount = amountInIdr * rateFromIdr
        return formatConverted(convertedAmount, currencyCode, currencySymbol)
    }

    fun formatConverted(
        convertedAmount: Double,
        currencyCode: String,
        currencySymbol: String
    ): String {
        val isZeroDecimal = currencyCode.equals("IDR", ignoreCase = true) ||
                currencyCode.equals("JPY", ignoreCase = true) ||
                currencySymbol == "Rp" ||
                currencySymbol == "¥"

        val isNegative = convertedAmount < 0
        val absAmount = abs(convertedAmount)

        val formattedNumber = if (isZeroDecimal) {
            val rounded = round(absAmount).toLong()
            val locale = if (currencyCode.equals("IDR", ignoreCase = true) || currencySymbol == "Rp") {
                Locale.forLanguageTag("id-ID")
            } else {
                Locale.US
            }
            NumberFormat.getNumberInstance(locale).format(rounded)
        } else {
            val symbols = DecimalFormatSymbols(Locale.US)
            val df = DecimalFormat("#,##0.00", symbols)
            df.format(absAmount)
        }

        return if (isNegative) {
            "-$currencySymbol$formattedNumber"
        } else {
            "$currencySymbol$formattedNumber"
        }
    }

    fun formatIdr(
        amount: Long,
        currencySymbol: String = currentConfig.symbol,
        rateFromIdr: Double = currentConfig.rateFromIdr,
        currencyCode: String = currentConfig.currencyCode
    ): String {
        return format(amount, currencyCode, currencySymbol, rateFromIdr)
    }

    fun parseAmount(input: String): Long? {
        val cleaned = input.replace("[^0-9]".toRegex(), "")
        return cleaned.toLongOrNull()
    }

    fun convertToIdr(inputAmount: Double, rateFromIdr: Double): Long {
        if (rateFromIdr <= 0.0) return inputAmount.toLong()
        return round(inputAmount / rateFromIdr).toLong()
    }

    fun convertFromIdr(amountInIdr: Long, rateFromIdr: Double): Double {
        return amountInIdr * rateFromIdr
    }
}
