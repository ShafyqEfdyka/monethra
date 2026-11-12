package com.example.monethra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.monethra.data.repository.FinanceRepository
import com.example.monethra.ui.components.BarChartGroup
import com.example.monethra.ui.components.DailySpendingData
import com.example.monethra.ui.components.DonutSliceData
import com.example.monethra.utils.CurrencyFormatter
import com.example.monethra.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Locale

import android.content.Context
import com.example.monethra.utils.EmailRecapHelper
import com.example.monethra.utils.PdfReportGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReportsUiState(
    val userName: String = "User",
    val userEmail: String? = null,
    val selectedMonth: Int = DateUtils.getCurrentMonth(),
    val selectedYear: Int = DateUtils.getCurrentYear(),
    val monthYearText: String = "",
    val paydayStartDay: Int = 1,
    val paydayCycleLabel: String = "",
    val totalIncome: Long = 0L,
    val totalExpenses: Long = 0L,
    val netSavings: Long = 0L,
    val remainingBudget: Long = 0L,
    val budgetUtilizationPercentage: Float = 0f,
    val categoryBreakdown: List<DonutSliceData> = emptyList(),
    val dailySpendingTrends: List<DailySpendingData> = emptyList(),
    val weeklyIncomeVsExpense: List<BarChartGroup> = emptyList(),
    val prevMonthComparisonPercentage: Float = 0f,
    val highestSpendingCategory: String? = null,
    val lowestSpendingCategory: String? = null,
    val transactionCount: Int = 0,
    val textualFinancialInsight: String = "",
    val currencySymbol: String = "Rp",
    val isLoading: Boolean = true
)

private data class ReportsMonthYearSelection(
    val month: Int,
    val year: Int,
    val paydayStartDay: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(DateUtils.getCurrentMonth())
    private val _selectedYear = MutableStateFlow(DateUtils.getCurrentYear())

    private val monthYearSelection = combine(_selectedMonth, _selectedYear, repository.paydayStartDay) { month, year, paydayDay ->
        ReportsMonthYearSelection(month, year, paydayDay)
    }

    val uiState: StateFlow<ReportsUiState> = monthYearSelection.flatMapLatest { selection ->
        val month = selection.month
        val year = selection.year
        val paydayDay = selection.paydayStartDay

        val (startOfCycle, endOfCycle) = DateUtils.getPaydayCycleStartAndEnd(month, year, paydayDay)
        val cycleLabel = DateUtils.getPaydayCycleLabel(month, year, paydayDay)

        val prevMonth = if (month == 1) 12 else month - 1
        val prevYear = if (month == 1) year - 1 else year
        val (startOfPrev, endOfPrev) = DateUtils.getPaydayCycleStartAndEnd(prevMonth, prevYear, paydayDay)

        combine(
            repository.userProfile,
            repository.allTransactions,
            repository.getBudgetForMonth(month, year),
            repository.currencySymbol
        ) { user, allTx, budget, symbol ->
            val monthTx = allTx.filter { it.date in startOfCycle..endOfCycle }
            val prevMonthTx = allTx.filter { it.date in startOfPrev..endOfPrev }

            val totalIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExpenses = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val netSavings = totalIncome - totalExpenses

            val totalBudget = budget?.totalBudget ?: 0L
            val remainingBudget = totalBudget - totalExpenses
            val budgetUtilization = if (totalBudget > 0) {
                (totalExpenses.toFloat() / totalBudget.toFloat()) * 100f
            } else {
                0f
            }

            // Category breakdown
            val expenseTx = monthTx.filter { it.type == "EXPENSE" }
            val categoryGrouped = expenseTx.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val categorySlices = categoryGrouped.entries
                .sortedByDescending { it.value }
                .mapIndexed { index, entry ->
                    val catAmount = entry.value
                    val percentage = if (totalExpenses > 0) (catAmount.toFloat() / totalExpenses) * 100f else 0f
                    DonutSliceData(
                        categoryName = entry.key,
                        amount = catAmount,
                        percentage = percentage,
                        color = DashboardViewModel.getCategoryColor(entry.key, index)
                    )
                }

            val highestCategory = categoryGrouped.maxByOrNull { it.value }?.key
            val lowestCategory = categoryGrouped.filter { it.value > 0 }.minByOrNull { it.value }?.key

            // Previous month comparison
            val prevExpenses = prevMonthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val prevComparisonPct = when {
                prevExpenses == 0L && totalExpenses == 0L -> 0f
                prevExpenses == 0L -> 100f
                else -> ((totalExpenses - prevExpenses).toFloat() / prevExpenses.toFloat()) * 100f
            }

            // Daily Spending Trends
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val dailyTrends = (1..maxDays).map { day ->
                val dayExpenses = expenseTx.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    txCal.get(Calendar.DAY_OF_MONTH) == day
                }.sumOf { it.amount }

                DailySpendingData(
                    day = day,
                    dateLabel = day.toString(),
                    amount = dayExpenses
                )
            }

            // Weekly Income vs Expense breakdown for Bar Chart
            val weeklyBreakdown = mutableListOf<BarChartGroup>()
            val weekRanges = listOf(
                "W1" to (1..7),
                "W2" to (8..14),
                "W3" to (15..21),
                "W4" to (22..28),
                "W5" to (29..maxDays)
            )

            weekRanges.forEach { (label, dayRange) ->
                val weekTx = monthTx.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    txCal.get(Calendar.DAY_OF_MONTH) in dayRange
                }
                val weekIncome = weekTx.filter { it.type == "INCOME" }.sumOf { it.amount }
                val weekExpense = weekTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                if (dayRange.first <= maxDays) {
                    weeklyBreakdown.add(
                        BarChartGroup(
                            label = label,
                            income = weekIncome,
                            expense = weekExpense
                        )
                    )
                }
            }

            // Insight generation
            val insightText = generateFinancialInsight(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netSavings = netSavings,
                prevComparisonPct = prevComparisonPct,
                highestCategory = highestCategory,
                symbol = symbol
            )

            ReportsUiState(
                userName = user?.name ?: "User",
                userEmail = user?.email,
                selectedMonth = month,
                selectedYear = year,
                monthYearText = DateUtils.formatMonthYear(month, year),
                paydayStartDay = paydayDay,
                paydayCycleLabel = cycleLabel,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netSavings = netSavings,
                remainingBudget = remainingBudget,
                budgetUtilizationPercentage = budgetUtilization,
                categoryBreakdown = categorySlices,
                dailySpendingTrends = dailyTrends,
                weeklyIncomeVsExpense = weeklyBreakdown,
                prevMonthComparisonPercentage = prevComparisonPct,
                highestSpendingCategory = highestCategory,
                lowestSpendingCategory = lowestCategory,
                transactionCount = monthTx.size,
                textualFinancialInsight = insightText,
                currencySymbol = symbol,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun selectMonthYear(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun previousMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value - 1)
            add(Calendar.MONTH, -1)
        }
        _selectedMonth.value = cal.get(Calendar.MONTH) + 1
        _selectedYear.value = cal.get(Calendar.YEAR)
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value - 1)
            add(Calendar.MONTH, 1)
        }
        _selectedMonth.value = cal.get(Calendar.MONTH) + 1
        _selectedYear.value = cal.get(Calendar.YEAR)
    }

    fun sendEmailRecap(context: Context) {
        val state = uiState.value
        val subject = EmailRecapHelper.generateEmailSubject(state.paydayCycleLabel)
        val body = EmailRecapHelper.generateEmailBody(
            userName = state.userName,
            cycleLabel = state.paydayCycleLabel,
            totalIncome = state.totalIncome,
            totalExpense = state.totalExpenses,
            netSavings = state.netSavings,
            budgetUtilization = state.budgetUtilizationPercentage.toInt(),
            topCategory = state.highestSpendingCategory ?: "",
            insight = state.textualFinancialInsight
        )
        EmailRecapHelper.sendEmailRecap(
            context = context,
            userEmail = state.userEmail ?: "",
            subject = subject,
            body = body
        )
    }

    fun exportPdfRecap(context: Context, month: Int = _selectedMonth.value, year: Int = _selectedYear.value) {
        viewModelScope.launch {
            val state = uiState.value
            val paydayDay = state.paydayStartDay
            val (start, end) = DateUtils.getPaydayCycleStartAndEnd(month, year, paydayDay)
            val cycleLabel = DateUtils.getPaydayCycleLabel(month, year, paydayDay)

            val monthTx = repository.getTransactionsByDateRange(start, end).first()
            val totalIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExpenses = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val netSavings = totalIncome - totalExpenses

            val pdfFile = PdfReportGenerator.generateMonthlyRecapPdf(
                context = context,
                userName = state.userName,
                userEmail = state.userEmail,
                cycleLabel = cycleLabel,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netSavings = netSavings,
                transactions = monthTx,
                currencySymbol = state.currencySymbol
            )

            val subject = EmailRecapHelper.generateEmailSubject(cycleLabel)
            val body = EmailRecapHelper.generateEmailBody(
                userName = state.userName,
                cycleLabel = cycleLabel,
                totalIncome = totalIncome,
                totalExpense = totalExpenses,
                netSavings = netSavings,
                insight = state.textualFinancialInsight
            )

            EmailRecapHelper.sendEmailRecapWithPdf(
                context = context,
                userEmail = state.userEmail ?: "",
                subject = subject,
                body = body,
                pdfFile = pdfFile
            )
        }
    }

    private fun generateFinancialInsight(
        totalIncome: Long,
        totalExpenses: Long,
        netSavings: Long,
        prevComparisonPct: Float,
        highestCategory: String?,
        symbol: String
    ): String {
        if (totalIncome == 0L && totalExpenses == 0L) {
            return "No transaction activity recorded for this month."
        }

        val comparisonPart = when {
            prevComparisonPct < 0f -> "Your expenses decreased by ${String.format(Locale.ROOT, "%.1f", -prevComparisonPct)}% compared to last month."
            prevComparisonPct > 0f -> "Your expenses increased by ${String.format(Locale.ROOT, "%.1f", prevComparisonPct)}% compared to last month."
            else -> "Your spending remained steady compared to last month."
        }

        val highestPart = if (highestCategory != null) {
            "$highestCategory was your highest spending category."
        } else ""

        val savingsPart = if (netSavings >= 0) {
            "You saved ${CurrencyFormatter.formatIdr(netSavings, symbol)}."
        } else {
            "You overspent by ${CurrencyFormatter.formatIdr(-netSavings, symbol)}."
        }

        return listOf(comparisonPart, highestPart, savingsPart)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }
}

class ReportsViewModelFactory(
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            return ReportsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
