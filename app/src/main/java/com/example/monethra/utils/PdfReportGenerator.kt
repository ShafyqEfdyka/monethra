package com.example.monethra.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.monethra.data.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private data class PdfCategorySlice(
        val categoryName: String,
        val amount: Long,
        val percentage: Float,
        val color: Int
    )

    private fun getCategoryColor(category: String, index: Int): Int {
        return when (category.lowercase(Locale.ROOT)) {
            "food & beverages", "food & drinks", "food", "f&b" -> Color.parseColor("#4CAF50")
            "transportation", "transport" -> Color.parseColor("#2196F3")
            "shopping" -> Color.parseColor("#E91E63")
            "bills & utilities", "bills", "utilities" -> Color.parseColor("#FF9800")
            "entertainment" -> Color.parseColor("#9C27B0")
            "health", "healthcare" -> Color.parseColor("#00BCD4")
            "education" -> Color.parseColor("#8BC34A")
            "investment" -> Color.parseColor("#3F51B5")
            else -> {
                val fallbacks = intArrayOf(
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#2196F3"),
                    Color.parseColor("#FF9800"),
                    Color.parseColor("#E91E63"),
                    Color.parseColor("#9C27B0"),
                    Color.parseColor("#00BCD4"),
                    Color.parseColor("#8BC34A"),
                    Color.parseColor("#FF5722"),
                    Color.parseColor("#607D8B")
                )
                fallbacks[index % fallbacks.size]
            }
        }
    }

    fun generateMonthlyRecapPdf(
        context: Context,
        userName: String,
        userEmail: String?,
        cycleLabel: String,
        totalIncome: Long,
        totalExpenses: Long,
        netSavings: Long,
        transactions: List<TransactionEntity>,
        currencySymbol: String = "Rp"
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        var yPos = 40f

        // Brand Banner Header
        paint.color = Color.parseColor("#2E5B88") // Primary Brand Color
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 100f, paint)

        // MONETHRA Title
        paint.color = Color.WHITE
        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MONETHRA", 36f, 50f, paint)

        // Subtitle
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Smart Personal Financial Recap", 36f, 72f, paint)

        // Date generated on top right
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val generatedDateStr = "Generated: ${sdf.format(Date())}"
        paint.textSize = 10f
        paint.color = Color.parseColor("#E0E6ED")
        canvas.drawText(generatedDateStr, pageWidth - 36f - paint.measureText(generatedDateStr), 50f, paint)

        yPos = 130f

        // User Info Box
        paint.color = Color.parseColor("#F4F6F9")
        canvas.drawRoundRect(36f, yPos, pageWidth - 36f, yPos + 70f, 12f, 12f, paint)

        paint.color = Color.parseColor("#1C252E")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Account & Report Details", 52f, yPos + 24f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val emailText = if (!userEmail.isNullOrBlank()) userEmail else "N/A"
        canvas.drawText("User: $userName  |  Email: $emailText", 52f, yPos + 44f, paint)
        canvas.drawText("Report Period / Payday Cycle: $cycleLabel", 52f, yPos + 60f, paint)

        yPos += 90f

        // Financial Summary Box Header
        paint.color = Color.parseColor("#1C252E")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Financial Summary", 36f, yPos, paint)

        yPos += 14f

        val boxWidth = (pageWidth - 72f - 24f) / 3f
        val boxHeight = 64f

        // Total Income Card
        paint.color = Color.parseColor("#E8F5E9")
        canvas.drawRoundRect(36f, yPos, 36f + boxWidth, yPos + boxHeight, 8f, 8f, paint)
        paint.color = Color.parseColor("#2E7D32")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL INCOME", 48f, yPos + 22f, paint)
        paint.textSize = 13f
        canvas.drawText(CurrencyFormatter.formatIdr(totalIncome, currencySymbol), 48f, yPos + 46f, paint)

        // Total Expenses Card
        val expLeft = 36f + boxWidth + 12f
        paint.color = Color.parseColor("#FFEBEE")
        canvas.drawRoundRect(expLeft, yPos, expLeft + boxWidth, yPos + boxHeight, 8f, 8f, paint)
        paint.color = Color.parseColor("#C62828")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL EXPENSES", expLeft + 12f, yPos + 22f, paint)
        paint.textSize = 13f
        canvas.drawText(CurrencyFormatter.formatIdr(totalExpenses, currencySymbol), expLeft + 12f, yPos + 46f, paint)

        // Net Savings Card
        val savLeft = expLeft + boxWidth + 12f
        paint.color = Color.parseColor("#E3F2FD")
        canvas.drawRoundRect(savLeft, yPos, savLeft + boxWidth, yPos + boxHeight, 8f, 8f, paint)
        paint.color = Color.parseColor("#1565C0")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NET SAVINGS", savLeft + 12f, yPos + 22f, paint)
        paint.textSize = 13f
        canvas.drawText(CurrencyFormatter.formatIdr(netSavings, currencySymbol), savLeft + 12f, yPos + 46f, paint)

        yPos += boxHeight + 24f

        // ---------------------------------------------------------
        // EXPENSE BREAKDOWN VISUAL CHART SECTION (2-Column Block)
        // ---------------------------------------------------------
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1C252E")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Expense Breakdown by Category", 36f, yPos, paint)

        yPos += 14f

        val chartSectionHeight = 150f

        // Container Card Background
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F8F9FA")
        canvas.drawRoundRect(36f, yPos, pageWidth - 36f, yPos + chartSectionHeight, 12f, 12f, paint)

        // Container Card Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#E9ECEF")
        canvas.drawRoundRect(36f, yPos, pageWidth - 36f, yPos + chartSectionHeight, 12f, 12f, paint)

        // Dynamic Expense Data Grouping
        val expenseTransactions = transactions.filter { it.type == "EXPENSE" }
        val categoryTotals = expenseTransactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val effectiveTotalExpense = if (totalExpenses > 0) totalExpenses else categoryTotals.values.sum()

        val sortedCategories = categoryTotals.entries.sortedByDescending { it.value }
        val displaySlices = mutableListOf<PdfCategorySlice>()

        if (sortedCategories.isNotEmpty() && effectiveTotalExpense > 0) {
            if (sortedCategories.size <= 5) {
                sortedCategories.forEachIndexed { index, entry ->
                    val pct = (entry.value.toFloat() / effectiveTotalExpense.toFloat()) * 100f
                    displaySlices.add(
                        PdfCategorySlice(
                            categoryName = entry.key,
                            amount = entry.value,
                            percentage = pct,
                            color = getCategoryColor(entry.key, index)
                        )
                    )
                }
            } else {
                val top4 = sortedCategories.take(4)
                val remaining = sortedCategories.drop(4)
                top4.forEachIndexed { index, entry ->
                    val pct = (entry.value.toFloat() / effectiveTotalExpense.toFloat()) * 100f
                    displaySlices.add(
                        PdfCategorySlice(
                            categoryName = entry.key,
                            amount = entry.value,
                            percentage = pct,
                            color = getCategoryColor(entry.key, index)
                        )
                    )
                }
                val otherAmount = remaining.sumOf { it.value }
                val otherPct = (otherAmount.toFloat() / effectiveTotalExpense.toFloat()) * 100f
                displaySlices.add(
                    PdfCategorySlice(
                        categoryName = "Others",
                        amount = otherAmount,
                        percentage = otherPct,
                        color = Color.parseColor("#78909C")
                    )
                )
            }
        }

        // Left Column: Donut / Ring Chart Dimensions
        val chartDiameter = 114f
        val ringStrokeWidth = 20f
        val centerX = 36f + 20f + (chartDiameter / 2f)
        val centerY = yPos + (chartSectionHeight / 2f)
        val arcRadius = (chartDiameter - ringStrokeWidth) / 2f
        val donutRect = android.graphics.RectF(
            centerX - arcRadius,
            centerY - arcRadius,
            centerX + arcRadius,
            centerY + arcRadius
        )

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ringStrokeWidth
        }

        if (displaySlices.isEmpty()) {
            // Draw Empty / Neutral Ring
            strokePaint.color = Color.parseColor("#E0E6ED")
            canvas.drawArc(donutRect, 0f, 360f, false, strokePaint)

            // Center text inside empty donut ring
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#6C757D")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val centerLabel = "Total Spent"
            canvas.drawText(centerLabel, centerX - paint.measureText(centerLabel) / 2f, centerY - 4f, paint)

            paint.color = Color.parseColor("#1C252E")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val zeroAmt = CurrencyFormatter.formatIdr(0L, currencySymbol)
            canvas.drawText(zeroAmt, centerX - paint.measureText(zeroAmt) / 2f, centerY + 12f, paint)

            // Right Column: Neutral Empty State
            paint.color = Color.parseColor("#6C757D")
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("No expenses recorded for this period", 210f, centerY + 4f, paint)
        } else {
            // Draw Donut Arcs
            var startAngle = -90f
            displaySlices.forEach { slice ->
                val sweepAngle = (slice.amount.toFloat() / effectiveTotalExpense.toFloat()) * 360f
                strokePaint.color = slice.color
                canvas.drawArc(donutRect, startAngle, sweepAngle, false, strokePaint)
                startAngle += sweepAngle
            }

            // Center text inside donut ring
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#6C757D")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val centerLabel = "Total Spent"
            canvas.drawText(centerLabel, centerX - paint.measureText(centerLabel) / 2f, centerY - 4f, paint)

            paint.color = Color.parseColor("#1C252E")
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val totalSpentStr = CurrencyFormatter.formatIdr(effectiveTotalExpense, currencySymbol)
            canvas.drawText(totalSpentStr, centerX - paint.measureText(totalSpentStr) / 2f, centerY + 12f, paint)

            // Right Column: Color-coded Legend
            val legendLeft = 210f
            val legendRight = pageWidth - 52f
            val itemHeight = 22f
            val totalLegendHeight = displaySlices.size * itemHeight
            var legendY = centerY - (totalLegendHeight / 2f) + (itemHeight / 2f) + 4f

            displaySlices.forEach { slice ->
                // Color marker dot
                paint.style = Paint.Style.FILL
                paint.color = slice.color
                canvas.drawCircle(legendLeft + 5f, legendY - 3.5f, 4.5f, paint)

                // Category name and percentage
                paint.color = Color.parseColor("#212529")
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val pctStr = String.format(Locale.US, "%.1f%%", slice.percentage)
                val catLabel = if (slice.categoryName.length > 18) slice.categoryName.take(16) + "…" else slice.categoryName
                canvas.drawText("$catLabel ($pctStr)", legendLeft + 16f, legendY, paint)

                // Formatted amount (Right aligned)
                paint.color = Color.parseColor("#495057")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val amtStr = CurrencyFormatter.formatIdr(slice.amount, currencySymbol)
                canvas.drawText(amtStr, legendRight - paint.measureText(amtStr), legendY, paint)

                legendY += itemHeight
            }
        }

        // Push down Transaction Details table dynamically based on chart section height
        yPos += chartSectionHeight + 28f

        // Reset paint style for table rendering
        paint.style = Paint.Style.FILL

        // Transaction Table Header Title
        paint.color = Color.parseColor("#1C252E")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Transaction Details (${transactions.size})", 36f, yPos, paint)

        yPos += 14f

        // Table Header Row Background
        paint.color = Color.parseColor("#2E5B88")
        canvas.drawRect(36f, yPos, pageWidth - 36f, yPos + 26f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Date", 46f, yPos + 17f, paint)
        canvas.drawText("Category", 130f, yPos + 17f, paint)
        canvas.drawText("Description", 240f, yPos + 17f, paint)
        val amtHeader = "Amount"
        canvas.drawText(amtHeader, pageWidth - 46f - paint.measureText(amtHeader), yPos + 17f, paint)

        yPos += 26f

        // Rows
        val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val rowHeight = 24f

        transactions.forEachIndexed { index, tx ->
            if (yPos + rowHeight > pageHeight - 50f) {
                // Finish page and start new page
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                yPos = 40f
                // Redraw table header
                paint.color = Color.parseColor("#2E5B88")
                canvas.drawRect(36f, yPos, pageWidth - 36f, yPos + 26f, paint)

                paint.color = Color.WHITE
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Date", 46f, yPos + 17f, paint)
                canvas.drawText("Category", 130f, yPos + 17f, paint)
                canvas.drawText("Description", 240f, yPos + 17f, paint)
                canvas.drawText(amtHeader, pageWidth - 46f - paint.measureText(amtHeader), yPos + 17f, paint)
                yPos += 26f
            }

            // Alternating Row Background
            if (index % 2 == 0) {
                paint.color = Color.parseColor("#F8F9FA")
                canvas.drawRect(36f, yPos, pageWidth - 36f, yPos + rowHeight, paint)
            }

            paint.color = Color.parseColor("#212529")
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            val dateStr = dateSdf.format(Date(tx.date))
            canvas.drawText(dateStr, 46f, yPos + 16f, paint)

            val categoryStr = if (tx.category.length > 18) tx.category.take(16) + "…" else tx.category
            canvas.drawText(categoryStr, 130f, yPos + 16f, paint)

            val descStr = if (tx.title.length > 28) tx.title.take(26) + "…" else tx.title
            canvas.drawText(descStr, 240f, yPos + 16f, paint)

            val formattedAmt = (if (tx.type == "INCOME") "+ " else "- ") + CurrencyFormatter.formatIdr(tx.amount, currencySymbol)
            paint.color = if (tx.type == "INCOME") Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(formattedAmt, pageWidth - 46f - paint.measureText(formattedAmt), yPos + 16f, paint)

            yPos += rowHeight
        }

        // Footer on final page
        paint.color = Color.parseColor("#6C757D")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Generated by MONETHRA App • End of Financial Recap", 36f, pageHeight - 24f, paint)

        pdfDocument.finishPage(page)

        // Save File in Cache
        val outputFile = File(context.cacheDir, "monethra_recap_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }
}
