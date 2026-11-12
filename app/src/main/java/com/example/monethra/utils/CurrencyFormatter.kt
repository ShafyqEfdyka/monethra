package com.example.monethra.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun formatIdr(amount: Long, currencySymbol: String = "Rp"): String {
        val localeID = Locale.forLanguageTag("id-ID")
        val formatter = NumberFormat.getNumberInstance(localeID)
        val formattedNumber = formatter.format(amount)
        return if (amount < 0) {
            "-$currencySymbol${formattedNumber.replace("-", "")}"
        } else {
            "$currencySymbol$formattedNumber"
        }
    }

    fun parseAmount(input: String): Long? {
        val cleaned = input.replace("[^0-9]".toRegex(), "")
        return cleaned.toLongOrNull()
    }
}
