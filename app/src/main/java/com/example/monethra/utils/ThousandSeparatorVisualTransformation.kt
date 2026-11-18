package com.example.monethra.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ThousandSeparatorVisualTransformation : VisualTransformation {
    private val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID"))
    private val formatter = DecimalFormat("#,###", symbols)

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val digitsOnly = originalText.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = try {
            if (digitsOnly.all { it == '0' }) {
                "0"
            } else {
                formatter.format(BigInteger(digitsOnly)).ifEmpty { "0" }
            }
        } catch (_: Exception) {
            digitsOnly
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, digitsOnly.length)
                var digitCount = 0
                for (i in formatted.indices) {
                    if ((digitCount == clamped) && !formatted[i].isDigit()) {
                        continue
                    }
                    if (digitCount == clamped) {
                        return i
                    }
                    if (formatted[i].isDigit()) {
                        digitCount++
                    }
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                var digitCount = 0
                for (i in 0 until clamped) {
                    if (formatted[i].isDigit()) {
                        digitCount++
                    }
                }
                return digitCount.coerceIn(0, digitsOnly.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
