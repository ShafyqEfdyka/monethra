package com.example.monethra.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.monethra.data.repository.FinanceRepository
import com.example.monethra.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AlertLevel {
    NONE,
    APPROACHING_75,
    NEAR_90,
    EXCEEDED_100
}

data class BudgetAlert(
    val categoryName: String,
    val allocatedAmount: Long,
    val spentAmount: Long,
    val percentage: Float,
    val alertLevel: AlertLevel,
    val message: String
)

data class CategoryAllocationUiModel(
    val categoryName: String,
    val allocatedAmount: Long,
    val spentAmount: Long,
    val remainingAmount: Long,
    val percentage: Float,
    val alertLevel: AlertLevel
)

data class BudgetUiState(
    val selectedMonth: Int = DateUtils.getCurrentMonth(),
    val selectedYear: Int = DateUtils.getCurrentYear(),
    val monthYearText: String = "",
    val totalMonthlyBudget: Long = 0L,
    val totalAllocated: Long = 0L,
    val unallocatedBudget: Long = 0L,
    val totalActualExpenses: Long = 0L,
    val totalRemainingBudget: Long = 0L,
    val categoryAllocations: List<CategoryAllocationUiModel> = emptyList(),
    val budgetAlerts: List<BudgetAlert> = emptyList(),
    val currencySymbol: String = "Rp",
    val paydayStartDay: Int = 1,
    val paydayCycleLabel: String = "",
    val isSetTotalBudgetDialogOpen: Boolean = false,
    val isAddEditCategoryDialogOpen: Boolean = false,
    val selectedCategoryToEdit: CategoryAllocationUiModel? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

private data class MonthYearSelection(
    val month: Int,
    val year: Int,
    val paydayStartDay: Int
)

private data class DialogState(
    val isSetTotalBudgetOpen: Boolean,
    val isAddEditCategoryOpen: Boolean,
    val selectedCategory: CategoryAllocationUiModel?,
    val errorMessage: String?
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(DateUtils.getCurrentMonth())
    private val _selectedYear = MutableStateFlow(DateUtils.getCurrentYear())

    private val _isSetTotalBudgetDialogOpen = MutableStateFlow(false)
    private val _isAddEditCategoryDialogOpen = MutableStateFlow(false)
    private val _selectedCategoryToEdit = MutableStateFlow<CategoryAllocationUiModel?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val monthYearSelection = combine(_selectedMonth, _selectedYear, repository.paydayStartDay) { month, year, paydayDay ->
        MonthYearSelection(month, year, paydayDay)
    }

    private val dialogState = combine(
        _isSetTotalBudgetDialogOpen,
        _isAddEditCategoryDialogOpen,
        _selectedCategoryToEdit,
        _errorMessage
    ) { setBudgetOpen, addCategoryOpen, selectedCat, error ->
        DialogState(setBudgetOpen, addCategoryOpen, selectedCat, error)
    }

    val uiState: StateFlow<BudgetUiState> = monthYearSelection.flatMapLatest { selection ->
        val month = selection.month
        val year = selection.year
        val paydayDay = selection.paydayStartDay

        val (startOfCycle, endOfCycle) = DateUtils.getPaydayCycleStartAndEnd(month, year, paydayDay)
        val cycleLabel = DateUtils.getPaydayCycleLabel(month, year, paydayDay)

        combine(
            repository.getBudgetForMonth(month, year),
            repository.getTransactionsByDateRange(startOfCycle, endOfCycle),
            repository.currencySymbol,
            dialogState
        ) { budgetEntity, monthTx, symbol, dialog ->
            Tuple4(budgetEntity, monthTx, symbol, dialog)
        }.flatMapLatest { tuple ->
            val budgetEntity = tuple.v1
            val monthTx = tuple.v2
            val symbol = tuple.v3
            val dialog = tuple.v4
            val totalBudget = budgetEntity?.totalBudget ?: 0L

            val categoryBudgetsFlow = if (budgetEntity != null) {
                repository.getCategoryBudgetsForBudget(budgetEntity.id)
            } else {
                flowOf(emptyList())
            }

            categoryBudgetsFlow.map { catBudgets ->
                // Calculate actual expenses per category from EXPENSE transactions
                val expenseTransactions = monthTx.filter { it.type == "EXPENSE" }
                val expensesByCategory = expenseTransactions.groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                val totalActualExpenses = expenseTransactions.sumOf { it.amount }

                val allocations = catBudgets.map { cat ->
                    val allocated = cat.allocatedAmount
                    val spent = expensesByCategory[cat.categoryName] ?: 0L
                    val remaining = allocated - spent
                    val percentage = if (allocated > 0) (spent.toFloat() / allocated.toFloat()) * 100f else 0f

                    val alertLevel = when {
                        allocated > 0 && spent >= allocated -> AlertLevel.EXCEEDED_100
                        percentage >= 90f -> AlertLevel.NEAR_90
                        percentage >= 75f -> AlertLevel.APPROACHING_75
                        else -> AlertLevel.NONE
                    }

                    CategoryAllocationUiModel(
                        categoryName = cat.categoryName,
                        allocatedAmount = allocated,
                        spentAmount = spent,
                        remainingAmount = remaining,
                        percentage = percentage,
                        alertLevel = alertLevel
                    )
                }.sortedByDescending { it.allocatedAmount }

                val totalAllocated = allocations.sumOf { it.allocatedAmount }
                val unallocatedBudget = totalBudget - totalAllocated
                val totalRemainingBudget = totalBudget - totalActualExpenses

                // Build budget alert list for >= 75%
                val alerts = allocations.filter { it.alertLevel != AlertLevel.NONE }.map { alloc ->
                    val formattedPct = String.format(java.util.Locale.ROOT, "%.0f", alloc.percentage)
                    val msg = when (alloc.alertLevel) {
                        AlertLevel.EXCEEDED_100 -> "Exceeded budget limit: ${alloc.categoryName} is at $formattedPct%"
                        AlertLevel.NEAR_90 -> "Near limit: ${alloc.categoryName} is at $formattedPct%"
                        AlertLevel.APPROACHING_75 -> "Approaching limit: ${alloc.categoryName} is at $formattedPct%"
                        AlertLevel.NONE -> ""
                    }
                    BudgetAlert(
                        categoryName = alloc.categoryName,
                        allocatedAmount = alloc.allocatedAmount,
                        spentAmount = alloc.spentAmount,
                        percentage = alloc.percentage,
                        alertLevel = alloc.alertLevel,
                        message = msg
                    )
                }

                BudgetUiState(
                    selectedMonth = month,
                    selectedYear = year,
                    monthYearText = DateUtils.formatMonthYear(month, year),
                    totalMonthlyBudget = totalBudget,
                    totalAllocated = totalAllocated,
                    unallocatedBudget = unallocatedBudget,
                    totalActualExpenses = totalActualExpenses,
                    totalRemainingBudget = totalRemainingBudget,
                    categoryAllocations = allocations,
                    budgetAlerts = alerts,
                    currencySymbol = symbol,
                    paydayStartDay = paydayDay,
                    paydayCycleLabel = cycleLabel,
                    isSetTotalBudgetDialogOpen = dialog.isSetTotalBudgetOpen,
                    isAddEditCategoryDialogOpen = dialog.isAddEditCategoryOpen,
                    selectedCategoryToEdit = dialog.selectedCategory,
                    isLoading = false,
                    errorMessage = dialog.errorMessage
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState(isLoading = true)
    )

    fun setMonthYear(month: Int, year: Int) {
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

    fun openSetTotalBudgetDialog() {
        _errorMessage.value = null
        _isSetTotalBudgetDialogOpen.value = true
    }

    fun closeSetTotalBudgetDialog() {
        _isSetTotalBudgetDialogOpen.value = false
        _errorMessage.value = null
    }

    fun setTotalMonthlyBudget(amount: Long) {
        if (amount < 0) {
            _errorMessage.value = "Budget amount cannot be negative"
            return
        }
        viewModelScope.launch {
            repository.setTotalBudget(_selectedMonth.value, _selectedYear.value, amount)
            closeSetTotalBudgetDialog()
        }
    }

    fun openAddCategoryAllocationDialog(category: CategoryAllocationUiModel? = null) {
        _selectedCategoryToEdit.value = category
        _errorMessage.value = null
        _isAddEditCategoryDialogOpen.value = true
    }

    fun closeAddCategoryAllocationDialog() {
        _isAddEditCategoryDialogOpen.value = false
        _selectedCategoryToEdit.value = null
        _errorMessage.value = null
    }

    fun saveCategoryAllocation(categoryName: String, amount: Long) {
        if (categoryName.isBlank()) {
            _errorMessage.value = "Category name is required"
            return
        }
        if (amount <= 0) {
            _errorMessage.value = "Allocated amount must be greater than 0"
            return
        }

        viewModelScope.launch {
            repository.saveCategoryAllocation(
                month = _selectedMonth.value,
                year = _selectedYear.value,
                categoryName = categoryName.trim(),
                allocatedAmount = amount
            )
            closeAddCategoryAllocationDialog()
        }
    }

    fun deleteCategoryAllocation(categoryName: String) {
        viewModelScope.launch {
            repository.deleteCategoryAllocation(
                month = _selectedMonth.value,
                year = _selectedYear.value,
                categoryName = categoryName
            )
            closeAddCategoryAllocationDialog()
        }
    }
}

private data class Tuple4<A, B, C, D>(val v1: A, val v2: B, val v3: C, val v4: D)

class BudgetViewModelFactory(
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            return BudgetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
