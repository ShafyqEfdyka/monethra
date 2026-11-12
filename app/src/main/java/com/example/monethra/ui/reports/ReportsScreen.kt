package com.example.monethra.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monethra.MonethraApplication
import com.example.monethra.ui.components.BarChart
import com.example.monethra.ui.components.DonutChart
import com.example.monethra.ui.components.LineChart
import com.example.monethra.ui.theme.DeepIndigo
import com.example.monethra.ui.theme.DustyPink
import com.example.monethra.ui.theme.ExpenseRed
import com.example.monethra.ui.theme.IncomeGreen
import com.example.monethra.ui.theme.PastelPeriwinkle
import com.example.monethra.ui.theme.PrimaryText
import com.example.monethra.ui.theme.SageGreen
import com.example.monethra.ui.theme.SecondaryText
import com.example.monethra.ui.theme.SoftLavender
import com.example.monethra.ui.theme.WarmIvory
import com.example.monethra.utils.CurrencyFormatter
import com.example.monethra.viewmodel.ReportsUiState
import com.example.monethra.viewmodel.ReportsViewModel
import com.example.monethra.viewmodel.ReportsViewModelFactory
import java.util.Locale

import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.OutlinedButton

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(
            (LocalContext.current.applicationContext as MonethraApplication).financeRepository,
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var isMonthPickerOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                ) {
                // Screen Title
                Text(
                    text = "Financial Reports",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    text = "Analytics, trends, and monthly insights",
                    fontSize = 13.sp,
                    color = SecondaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Header Month Picker
                ReportsMonthPicker(
                    monthYearText = uiState.monthYearText,
                    paydayCycleLabel = uiState.paydayCycleLabel,
                    onPreviousClick = { viewModel.previousMonth() },
                    onNextClick = { viewModel.nextMonth() },
                    onMonthClick = { isMonthPickerOpen = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Card
                ReportsSummaryCard(uiState = uiState)

                Spacer(modifier = Modifier.height(16.dp))

                // Income vs Expense Bar Chart
                BarChartCard(uiState = uiState)

                Spacer(modifier = Modifier.height(16.dp))

                // Spending Trends Line Chart
                LineChartCard(uiState = uiState)

                Spacer(modifier = Modifier.height(16.dp))

                // Expense Breakdown Donut Chart
                DonutChartCard(uiState = uiState)

                Spacer(modifier = Modifier.height(16.dp))

                // Automatic Monthly Recap Card
                MonthlyRecapCard(
                    uiState = uiState,
                    onExportPdfClick = {
                        viewModel.exportPdfRecap(context, uiState.selectedMonth, uiState.selectedYear)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (isMonthPickerOpen) {
            MonthYearPickerDialog(
                currentMonth = uiState.selectedMonth,
                currentYear = uiState.selectedYear,
                onDismiss = { isMonthPickerOpen = false },
                onConfirm = { month, year ->
                    viewModel.selectMonthYear(month, year)
                    isMonthPickerOpen = false
                }
            )
        }
    }
}

@Composable
fun ReportsMonthPicker(
    monthYearText: String,
    paydayCycleLabel: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onMonthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "Previous Month",
                    modifier = Modifier.size(18.dp),
                    tint = PrimaryText
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onMonthClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthYearText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    if (paydayCycleLabel.isNotBlank()) {
                        Text(
                            text = "Cycle: $paydayCycleLabel",
                            fontSize = 11.sp,
                            color = SecondaryText
                        )
                    }
                }
            }

            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = "Next Month",
                    modifier = Modifier.size(18.dp),
                    tint = PrimaryText
                )
            }
        }
    }
}

@Composable
fun ReportsSummaryCard(
    uiState: ReportsUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Monthly Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Income & Expense Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Income Badge (Sage Green)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SageGreen)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Income",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = CurrencyFormatter.formatIdr(uiState.totalIncome, uiState.currencySymbol),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }
                }

                // Total Expense Badge (Dusty Pink)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DustyPink)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Expenses",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = CurrencyFormatter.formatIdr(uiState.totalExpenses, uiState.currencySymbol),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Net Savings & Remaining Budget Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Net Savings",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                    Text(
                        text = CurrencyFormatter.formatIdr(uiState.netSavings, uiState.currencySymbol),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.netSavings >= 0) IncomeGreen else ExpenseRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Remaining Budget",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                    Text(
                        text = CurrencyFormatter.formatIdr(uiState.remainingBudget, uiState.currencySymbol),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Budget Utilization Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Utilization",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SecondaryText
                )
                Text(
                    text = "${String.format(Locale.ROOT, "%.1f", uiState.budgetUtilizationPercentage)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (uiState.budgetUtilizationPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    uiState.budgetUtilizationPercentage >= 100f -> ExpenseRed
                    uiState.budgetUtilizationPercentage >= 75f -> Color(0xFFFF9800)
                    else -> IncomeGreen
                },
                trackColor = WarmIvory
            )
        }
    }
}

@Composable
fun BarChartCard(
    uiState: ReportsUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Analytics,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Income vs Expense",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }
            Text(
                text = "Weekly comparison within the month",
                fontSize = 12.sp,
                color = SecondaryText,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            BarChart(
                groups = uiState.weeklyIncomeVsExpense,
                currencySymbol = uiState.currencySymbol,
                height = 200.dp
            )
        }
    }
}

@Composable
fun LineChartCard(
    uiState: ReportsUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Daily Spending Trends",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }
            Text(
                text = "Daily expense progression through the month",
                fontSize = 12.sp,
                color = SecondaryText,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LineChart(
                dataPoints = uiState.dailySpendingTrends,
                currencySymbol = uiState.currencySymbol,
                height = 180.dp
            )
        }
    }
}

@Composable
fun DonutChartCard(
    uiState: ReportsUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PieChart,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Expense Distribution",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }
            Text(
                text = "Percentage distribution by category",
                fontSize = 12.sp,
                color = SecondaryText,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DonutChart(
                slices = uiState.categoryBreakdown,
                totalAmount = uiState.totalExpenses,
                currencySymbol = uiState.currencySymbol
            )
        }
    }
}

@Composable
fun MonthlyRecapCard(
    uiState: ReportsUiState,
    onExportPdfClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SoftLavender),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Automated Monthly Recap",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Text(
                        text = "Key insights generated for ${uiState.monthYearText}",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key Stats Grid (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 1: Transactions
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarmIvory)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = SecondaryText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Transactions",
                                fontSize = 11.sp,
                                color = SecondaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.transactionCount.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }
                }

                // Stat 2: Comparison vs Last Month
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarmIvory)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isLower = uiState.prevMonthComparisonPercentage < 0
                            Icon(
                                imageVector = if (isLower) Icons.AutoMirrored.Rounded.TrendingDown else Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isLower) IncomeGreen else ExpenseRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "vs Prev Month",
                                fontSize = 11.sp,
                                color = SecondaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (uiState.prevMonthComparisonPercentage > 0) "+" else ""}${String.format(Locale.ROOT, "%.1f", uiState.prevMonthComparisonPercentage)}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.prevMonthComparisonPercentage < 0) IncomeGreen else if (uiState.prevMonthComparisonPercentage > 0) ExpenseRed else PrimaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 3: Highest Category
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarmIvory)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = SecondaryText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Highest Category",
                                fontSize = 11.sp,
                                color = SecondaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.highestSpendingCategory ?: "-",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 1
                        )
                    }
                }

                // Stat 4: Lowest Category
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarmIvory)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = SecondaryText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lowest Category",
                                fontSize = 11.sp,
                                color = SecondaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.lowestSpendingCategory ?: "-",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Highlighted Financial Insight Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SoftLavender)
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Smart Financial Insight",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.textualFinancialInsight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full-Width Action Button: Export to PDF
            Button(
                onClick = onExportPdfClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PastelPeriwinkle,
                    contentColor = DeepIndigo
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "PDF Icon",
                        tint = DeepIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export Monthly Recap to PDF",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepIndigo
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthYearPickerDialog(
    currentMonth: Int,
    currentYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    var selectedYear by remember { mutableIntStateOf(currentYear) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    var monthExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Month & Year",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Month Selector
                ExposedDropdownMenuBox(
                    expanded = monthExpanded,
                    onExpandedChange = { monthExpanded = !monthExpanded }
                ) {
                    OutlinedTextField(
                        value = monthNames[selectedMonth - 1],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Month") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = monthExpanded,
                        onDismissRequest = { monthExpanded = false }
                    ) {
                        monthNames.forEachIndexed { index, monthName ->
                            DropdownMenuItem(
                                text = { Text(monthName) },
                                onClick = {
                                    selectedMonth = index + 1
                                    monthExpanded = false
                                }
                            )
                        }
                    }
                }

                // Year Selector Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Year",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedYear -= 1 }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                                contentDescription = "Decrease Year",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "$selectedYear",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { selectedYear += 1 }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = "Increase Year",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMonth, selectedYear) }) {
                Text("Select", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
