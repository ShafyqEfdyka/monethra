package com.example.monethra.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.monethra.data.entity.TransactionEntity
import com.example.monethra.data.repository.FinanceRepository
import com.example.monethra.ui.components.DonutSliceData
import com.example.monethra.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String = "User",
    val userEmail: String? = null,
    val totalBalance: Long = 0L,
    val monthlyIncome: Long = 0L,
    val monthlyExpenses: Long = 0L,
    val monthlyBudget: Long = 0L,
    val remainingBudget: Long = 0L,
    val budgetUsagePercentage: Float = 0f,
    val categoryExpenses: List<DonutSliceData> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val currencySymbol: String = "Rp",
    val paydayStartDay: Int = 1,
    val paydayCycleLabel: String = "",
    val showPaydayBanner: Boolean = false,
    val paydayBannerMessage: String? = null,
    val isLoading: Boolean = true
)

private data class DashboardData(
    val user: com.example.monethra.data.entity.UserEntity?,
    val allTx: List<TransactionEntity>,
    val budget: com.example.monethra.data.entity.BudgetEntity?,
    val symbol: String,
    val paydayStartDay: Int
)

class DashboardViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val currentMonth = DateUtils.getCurrentMonth()
    private val currentYear = DateUtils.getCurrentYear()

    private val _showPaydayBanner = MutableStateFlow(false)
    private val _paydayBannerMessage = MutableStateFlow<String?>(null)

    private val bannerState = combine(_showPaydayBanner, _paydayBannerMessage) { show, msg ->
        Pair(show, msg)
    }

    init {
        checkPaydayCycleArrival()
    }

    private fun checkPaydayCycleArrival() {
        viewModelScope.launch {
            combine(
                repository.paydayStartDay,
                repository.lastProcessedPaydayCycle
            ) { paydayDay, lastKey ->
                Pair(paydayDay, lastKey)
            }.collect { (paydayDay, lastKey) ->
                val currentCycleKey = "$currentYear-$currentMonth-$paydayDay"
                if (lastKey.isNotBlank() && lastKey != currentCycleKey) {
                    repository.carryOverBudgetIfNecessary(currentMonth, currentYear)
                    _paydayBannerMessage.value = "New Payday Cycle Started!"
                    _showPaydayBanner.value = true
                }
                repository.setLastProcessedPaydayCycle(currentCycleKey)
            }
        }
    }

    fun dismissPaydayBanner() {
        _showPaydayBanner.value = false
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.userProfile,
        repository.allTransactions,
        repository.getBudgetForMonth(currentMonth, currentYear),
        repository.currencySymbol,
        repository.paydayStartDay
    ) { user, allTx, budget, symbol, paydayDay ->
        DashboardData(user, allTx, budget, symbol, paydayDay)
    }.combine(bannerState) { data, banner ->
        val (user, allTx, budget, symbol, paydayDay) = data
        val (showBanner, bannerMsg) = banner

        val (startOfCycle, endOfCycle) = DateUtils.getPaydayCycleStartAndEnd(currentMonth, currentYear, paydayDay)
        val cycleLabel = DateUtils.getPaydayCycleLabel(currentMonth, currentYear, paydayDay)

        val monthTx = allTx.filter { it.date in startOfCycle..endOfCycle }

        // Total Balance = all income - all expense
        val totalIncome = allTx.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = allTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalBalance = totalIncome - totalExpense

        // Monthly Income and Expenses
        val monthlyIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
        val monthlyExpenses = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        // Monthly Budget & Remaining
        val monthlyBudgetAmount = budget?.totalBudget ?: 0L
        val remainingBudget = monthlyBudgetAmount - monthlyExpenses
        val budgetUsagePercentage = if (monthlyBudgetAmount > 0) {
            (monthlyExpenses.toFloat() / monthlyBudgetAmount.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Category Expenses breakdown for Donut Chart
        val expenseTransactions = monthTx.filter { it.type == "EXPENSE" }
        val categoryGrouped = expenseTransactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val categorySlices = categoryGrouped.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, entry ->
                val catAmount = entry.value
                val percentage = if (monthlyExpenses > 0) (catAmount.toFloat() / monthlyExpenses) * 100f else 0f
                DonutSliceData(
                    categoryName = entry.key,
                    amount = catAmount,
                    percentage = percentage,
                    color = getCategoryColor(entry.key, index)
                )
            }

        // Recent 5 transactions sorted by date descending
        val recentFive = allTx.sortedByDescending { it.date }.take(5)

        DashboardUiState(
            userName = user?.name ?: "User",
            userEmail = user?.email,
            totalBalance = totalBalance,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            monthlyBudget = monthlyBudgetAmount,
            remainingBudget = remainingBudget,
            budgetUsagePercentage = budgetUsagePercentage,
            categoryExpenses = categorySlices,
            recentTransactions = recentFive,
            currencySymbol = symbol,
            paydayStartDay = paydayDay,
            paydayCycleLabel = cycleLabel,
            showPaydayBanner = showBanner,
            paydayBannerMessage = bannerMsg,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    companion object {
        fun getCategoryColor(category: String, index: Int): Color {
            return when (category.lowercase()) {
                "food & beverages", "food", "f&b" -> Color(0xFF81C784)
                "transportation", "transport" -> Color(0xFF64B5F6)
                "shopping" -> Color(0xFFE57373)
                "bills & utilities", "bills", "utilities" -> Color(0xFFFFB74D)
                "entertainment" -> Color(0xFFBA68C8)
                "health", "healthcare" -> Color(0xFF4DD0E1)
                "education" -> Color(0xFFAED581)
                else -> {
                    val fallbackColors = listOf(
                        Color(0xFF81C784),
                        Color(0xFF64B5F6),
                        Color(0xFFE57373),
                        Color(0xFFFFB74D),
                        Color(0xFFBA68C8),
                        Color(0xFF4DD0E1),
                        Color(0xFFAED581),
                        Color(0xFFFF8A65)
                    )
                    fallbackColors[index % fallbackColors.size]
                }
            }
        }
    }
}

class DashboardViewModelFactory(
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
