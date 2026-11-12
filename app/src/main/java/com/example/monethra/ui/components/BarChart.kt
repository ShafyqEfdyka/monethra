package com.example.monethra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monethra.ui.theme.ExpenseRed
import com.example.monethra.ui.theme.IncomeGreen
import com.example.monethra.ui.theme.PrimaryText
import com.example.monethra.ui.theme.SecondaryText
import kotlin.math.max

data class BarChartGroup(
    val label: String,
    val income: Long,
    val expense: Long
)

@Composable
fun BarChart(
    groups: List<BarChartGroup>,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    currencySymbol: String = "Rp",
    incomeColor: Color = IncomeGreen,
    expenseColor: Color = ExpenseRed
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(groups) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(incomeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Income",
                    fontSize = 12.sp,
                    color = PrimaryText,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(expenseColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Expense",
                    fontSize = 12.sp,
                    color = PrimaryText,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (groups.isEmpty() || groups.all { it.income == 0L && it.expense == 0L }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No income or expense data for this month",
                    fontSize = 13.sp,
                    color = SecondaryText
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val labelAreaHeight = 32.dp.toPx()
                val yAxisWidth = 50.dp.toPx()

                val chartWidth = canvasWidth - yAxisWidth
                val chartHeight = canvasHeight - labelAreaHeight

                val maxVal = groups.maxOf { max(it.income, it.expense) }.coerceAtLeast(100L)
                val roundedMax = getNiceMaxValue(maxVal)

                // Draw Y-axis grid lines & labels (4 lines)
                val gridLineCount = 4
                for (i in 0..gridLineCount) {
                    val fraction = i.toFloat() / gridLineCount
                    val y = chartHeight - (fraction * chartHeight)

                    val gridValue = (fraction * roundedMax).toLong()
                    val formattedVal = formatShortAmount(gridValue, currencySymbol)

                    // Line
                    drawLine(
                        color = gridLineColor,
                        start = Offset(yAxisWidth, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    // Y Label
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = labelTextColor.hashCode()
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }
                        drawText(
                            formattedVal,
                            yAxisWidth - 8.dp.toPx(),
                            y + 4.dp.toPx(),
                            paint
                        )
                    }
                }

                // X Axis baseline
                drawLine(
                    color = axisColor,
                    start = Offset(yAxisWidth, chartHeight),
                    end = Offset(canvasWidth, chartHeight),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Draw bars for each group
                val groupCount = groups.size
                val groupWidth = chartWidth / groupCount
                val barWidth = (groupWidth * 0.28f).coerceAtMost(24.dp.toPx())
                val barGap = 4.dp.toPx()

                groups.forEachIndexed { index, group ->
                    val groupCenterX = yAxisWidth + (index + 0.5f) * groupWidth

                    val incomeHeight = (group.income.toFloat() / roundedMax.toFloat() * chartHeight * animProgress.value)
                    val expenseHeight = (group.expense.toFloat() / roundedMax.toFloat() * chartHeight * animProgress.value)

                    val incomeLeft = groupCenterX - barWidth - (barGap / 2)
                    val expenseLeft = groupCenterX + (barGap / 2)

                    // Income bar
                    if (incomeHeight > 0f) {
                        drawRoundRect(
                            color = incomeColor,
                            topLeft = Offset(incomeLeft, chartHeight - incomeHeight),
                            size = Size(barWidth, incomeHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Expense bar
                    if (expenseHeight > 0f) {
                        drawRoundRect(
                            color = expenseColor,
                            topLeft = Offset(expenseLeft, chartHeight - expenseHeight),
                            size = Size(barWidth, expenseHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Draw X Label
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = labelTextColor.hashCode()
                            textSize = 11.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawText(
                            group.label,
                            groupCenterX,
                            canvasHeight - 6.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }
    }
}

private fun getNiceMaxValue(maxVal: Long): Long {
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(maxVal.toDouble()))).toLong().coerceAtLeast(1L)
    val normalized = maxVal.toDouble() / magnitude
    val niceNormalized = when {
        normalized <= 1.2 -> 1.2
        normalized <= 1.5 -> 1.5
        normalized <= 2.0 -> 2.0
        normalized <= 2.5 -> 2.5
        normalized <= 5.0 -> 5.0
        normalized <= 7.5 -> 7.5
        else -> 10.0
    }
    return (niceNormalized * magnitude).toLong().coerceAtLeast(100L)
}

private fun formatShortAmount(amount: Long, symbol: String): String {
    return when {
        amount >= 1_000_000 -> "$symbol${String.format(java.util.Locale.ROOT, "%.1fM", amount / 1_000_000f)}"
        amount >= 1_000 -> "$symbol${amount / 1_000}k"
        else -> "$symbol$amount"
    }
}
