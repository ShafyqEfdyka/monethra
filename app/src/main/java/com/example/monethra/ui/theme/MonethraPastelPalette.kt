package com.example.monethra.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Curated Monethra Pastel Color Library
 *
 * Soft, cohesive pastel colors established across Monethra's cards, badges, and surfaces.
 */
object MonethraPastelPalette {
    val PastelPeriwinkle = Color(0xFF9FA0FF) // Primary Brand Accent
    val SoftLavenderTint = Color(0xFFEAE6F8) // Smart Insight / Active Pill
    val PastelMintGreen = Color(0xFFDDF3E4)  // Income Card
    val PastelBlushPink = Color(0xFFFCE4E6)  // Expense Card
    val PastelButterCream = Color(0xFFFEF3D6) // Remaining Budget Card
    val PastelSkyBlue = Color(0xFFDCEAFB)    // Total Balance Card / Notification Track
    val SoftLilacMist = Color(0xFFD6D2F2)    // Secondary Accent
    val WarmAlabasterPastel = Color(0xFFF4F1EA) // App Surface

    /**
     * All curated pastel palette colors.
     */
    val allColors: List<Color> = listOf(
        PastelPeriwinkle,
        SoftLavenderTint,
        PastelMintGreen,
        PastelBlushPink,
        PastelButterCream,
        PastelSkyBlue,
        SoftLilacMist,
        WarmAlabasterPastel,
    )

    /**
     * Returns a smooth, randomized 3-color pastel linear gradient brush
     * selecting 3 distinct colors from [allColors].
     */
    fun getRandomThreeColorGradient(): Brush {
        val selectedThreeColors = allColors.shuffled().take(3)
        return Brush.linearGradient(
            colors = selectedThreeColors,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    }
}
