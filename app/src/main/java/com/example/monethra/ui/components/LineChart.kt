package com.example.monethra.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monethra.ui.theme.ExpenseRed
import com.example.monethra.ui.theme.SecondaryText

data class DailySpendingData(
    val day: Int,
    val dateLabel: String,
    val amount: Long
)

@Composable
fun LineChart(
    dataPoints: List<DailySpendingData>,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    currencySymbol: String = "Rp",
    lineColor: Color = ExpenseRed
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(dataPoints) {
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
        if (dataPoints.isEmpty() || dataPoints.all { it.amount == 0L }) {
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
                    text = "No daily spending trends available for this month",
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

                val labelAreaHeight = 30.dp.toPx()
                val yAxisWidth = 48.dp.toPx()

                val chartWidth = canvasWidth - yAxisWidth
                val chartHeight = canvasHeight - labelAreaHeight

                val maxAmount = dataPoints.maxOf { it.amount }.coerceAtLeast(100L)
                val roundedMax = getNiceMaxValue(maxAmount)

                // Grid lines & Y-axis labels
                val gridLineCount = 4
                for (i in 0..gridLineCount) {
                    val fraction = i.toFloat() / gridLineCount
                    val y = chartHeight - (fraction * chartHeight)

                    val gridValue = (fraction * roundedMax).toLong()
                    val formattedVal = formatShortAmount(gridValue, currencySymbol)

                    drawLine(
                        color = gridLineColor,
                        start = Offset(yAxisWidth, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = labelTextColor.hashCode()
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }
                        drawText(
                            formattedVal,
                            yAxisWidth - 6.dp.toPx(),
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

                // Compute point coordinates
                val pointCount = dataPoints.size
                val stepX = if (pointCount > 1) chartWidth / (pointCount - 1) else chartWidth

                val points = dataPoints.mapIndexed { index, dp ->
                    val x = yAxisWidth + index * stepX
                    val y = chartHeight - (dp.amount.toFloat() / roundedMax.toFloat() * chartHeight * animProgress.value)
                    Offset(x, y)
                }

                // Draw gradient fill under line
                if (points.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(points.first().x, chartHeight)
                        lineTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val p1 = points[i - 1]
                            val p2 = points[i]
                            val controlX1 = p1.x + (p2.x - p1.x) / 2
                            val controlX2 = p1.x + (p2.x - p1.x) / 2
                            cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                        }
                        lineTo(points.last().x, chartHeight)
                        close()
                    }

                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.4f),
                                lineColor.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    // Draw line stroke
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val p1 = points[i - 1]
                            val p2 = points[i]
                            val controlX1 = p1.x + (p2.x - p1.x) / 2
                            val controlX2 = p1.x + (p2.x - p1.x) / 2
                            cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw data point dots & X Labels for subset of days
                    val labelInterval = (pointCount / 5).coerceAtLeast(1)
                    points.forEachIndexed { index, point ->
                        if (dataPoints[index].amount > 0) {
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = point
                            )
                        }

                        // X-axis label
                        if (index == 0 || index == pointCount - 1 || index % labelInterval == 0) {
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = labelTextColor.hashCode()
                                    textSize = 10.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                drawText(
                                    dataPoints[index].dateLabel,
                                    point.x,
                                    canvasHeight - 6.dp.toPx(),
                                    paint
                                )
                            }
                        }
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
