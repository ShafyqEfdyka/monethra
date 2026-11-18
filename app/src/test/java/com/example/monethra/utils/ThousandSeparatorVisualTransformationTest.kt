package com.example.monethra.utils

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class ThousandSeparatorVisualTransformationTest {

    private val transformation = ThousandSeparatorVisualTransformation()

    @Test
    fun filter_formatsThousandSeparatorsCorrectly() {
        val result = transformation.filter(AnnotatedString("1500000"))
        assertEquals("1.500.000", result.text.text)
    }

    @Test
    fun filter_formatsSmallNumbersWithoutSeparators() {
        val result = transformation.filter(AnnotatedString("500"))
        assertEquals("500", result.text.text)
    }

    @Test
    fun filter_formatsFourDigits() {
        val result = transformation.filter(AnnotatedString("1000"))
        assertEquals("1.000", result.text.text)
    }

    @Test
    fun filter_formatsZero() {
        val result = transformation.filter(AnnotatedString("0"))
        assertEquals("0", result.text.text)
    }

    @Test
    fun filter_formatsEmpty() {
        val result = transformation.filter(AnnotatedString(""))
        assertEquals("", result.text.text)
    }

    @Test
    fun offsetMapping_biDirectionalMappingIsAccurate() {
        val raw = "1500000"
        val transformed = transformation.filter(AnnotatedString(raw))
        assertEquals("1.500.000", transformed.text.text)

        val mapping = transformed.offsetMapping
        for (i in 0..raw.length) {
            val transformedOffset = mapping.originalToTransformed(i)
            val backToOriginal = mapping.transformedToOriginal(transformedOffset)
            assertEquals("Mapping mismatch for original offset $i", i, backToOriginal)
        }
    }
}
