package com.example.monethra.viewmodel

import com.example.monethra.data.dao.BudgetDao
import com.example.monethra.data.dao.CategoryBudgetDao
import com.example.monethra.data.dao.TransactionDao
import com.example.monethra.data.dao.UserDao
import com.example.monethra.data.datastore.UserPreferencesRepository
import com.example.monethra.data.entity.BudgetEntity
import com.example.monethra.data.entity.CategoryBudgetEntity
import com.example.monethra.data.entity.TransactionEntity
import com.example.monethra.data.entity.UserEntity
import com.example.monethra.data.repository.FinanceRepository
import com.example.monethra.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeBudgetDao : BudgetDao {
        val budgets = MutableStateFlow<List<BudgetEntity>>(emptyList())

        override fun getBudgetForMonth(month: Int, year: Int, userId: Long): Flow<BudgetEntity?> {
            return budgets.map { list -> list.find { it.month == month && it.year == year } }
        }
        override suspend fun getBudgetForMonthSync(month: Int, year: Int, userId: Long): BudgetEntity? {
            return budgets.value.find { it.month == month && it.year == year }
        }
        override fun getAllBudgets(userId: Long): Flow<List<BudgetEntity>> = budgets
        override suspend fun getBudgetCount(userId: Long): Int = budgets.value.size
        override suspend fun insertBudget(budget: BudgetEntity): Long {
            val current = budgets.value.toMutableList()
            val newId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            val entity = budget.copy(id = newId)
            current.add(entity)
            budgets.value = current
            return newId
        }
        override suspend fun updateBudget(budget: BudgetEntity) {
            val current = budgets.value.toMutableList()
            val idx = current.indexOfFirst { it.id == budget.id }
            if (idx != -1) {
                current[idx] = budget
                budgets.value = current
            }
        }
        override suspend fun deleteBudget(budget: BudgetEntity) {
            val current = budgets.value.toMutableList()
            current.removeAll { it.id == budget.id }
            budgets.value = current
        }
        override suspend fun deleteAllBudgets(userId: Long) {
            budgets.value = emptyList()
        }
    }

    private class FakeCategoryBudgetDao : CategoryBudgetDao {
        val categoryBudgets = MutableStateFlow<List<CategoryBudgetEntity>>(emptyList())

        override fun getCategoryBudgetsForBudget(budgetId: Long): Flow<List<CategoryBudgetEntity>> {
            return categoryBudgets.map { list -> list.filter { it.budgetId == budgetId } }
        }
        override suspend fun getCategoryBudgetsForBudgetSync(budgetId: Long): List<CategoryBudgetEntity> {
            return categoryBudgets.value.filter { it.budgetId == budgetId }
        }
        override suspend fun insertCategoryBudget(categoryBudget: CategoryBudgetEntity): Long {
            val current = categoryBudgets.value.toMutableList()
            val newId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            val entity = categoryBudget.copy(id = newId)
            current.add(entity)
            categoryBudgets.value = current
            return newId
        }
        override suspend fun insertCategoryBudgets(categoryBudgets: List<CategoryBudgetEntity>) {
            val current = this.categoryBudgets.value.toMutableList()
            current.addAll(categoryBudgets)
            this.categoryBudgets.value = current
        }
        override suspend fun updateCategoryBudget(categoryBudget: CategoryBudgetEntity) {
            val current = categoryBudgets.value.toMutableList()
            val idx = current.indexOfFirst { it.id == categoryBudget.id }
            if (idx != -1) {
                current[idx] = categoryBudget
                categoryBudgets.value = current
            }
        }
        override suspend fun deleteCategoryBudget(categoryBudget: CategoryBudgetEntity) {
            val current = categoryBudgets.value.toMutableList()
            current.removeAll { it.id == categoryBudget.id }
            categoryBudgets.value = current
        }
        override suspend fun deleteCategoryBudgetsForBudget(budgetId: Long) {
            val current = categoryBudgets.value.toMutableList()
            current.removeAll { it.budgetId == budgetId }
            categoryBudgets.value = current
        }
        override suspend fun deleteAllCategoryBudgets() {
            categoryBudgets.value = emptyList()
        }
    }

    private class FakeTransactionDao : TransactionDao {
        val txList = MutableStateFlow<List<TransactionEntity>>(emptyList())

        override fun getAllTransactions(userId: Long): Flow<List<TransactionEntity>> = txList
        override fun getTransactionsByDateRange(startDate: Long, endDate: Long, userId: Long): Flow<List<TransactionEntity>> {
            return txList.map { list -> list.filter { it.date in startDate..endDate } }
        }
        override fun getRecentTransactions(limit: Int, userId: Long): Flow<List<TransactionEntity>> =
            txList.map { list -> list.sortedByDescending { it.date }.take(limit) }
        override fun getTransactionById(id: Long): Flow<TransactionEntity?> =
            txList.map { list -> list.find { it.id == id } }
        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val current = txList.value.toMutableList()
            val newId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            val entity = transaction.copy(id = newId)
            current.add(entity)
            txList.value = current
            return newId
        }
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
            val current = txList.value.toMutableList()
            current.addAll(transactions)
            txList.value = current
        }
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override suspend fun deleteAllTransactions(userId: Long) {}
        override suspend fun getTransactionCount(userId: Long): Int = txList.value.size
    }

    private class FakeUserDao : UserDao {
        override fun getUser(): Flow<UserEntity?> = flowOf(null)
        override suspend fun getUserSync(): UserEntity? = null
        override fun getUserById(userId: Long): Flow<UserEntity?> = flowOf(null)
        override suspend fun getUserByIdSync(userId: Long): UserEntity? = null
        override suspend fun getUserByEmail(email: String): UserEntity? = null
        override suspend fun insertUser(user: UserEntity): Long = 1L
        override suspend fun updateUser(user: UserEntity) {}
        override suspend fun deleteUser(user: UserEntity) {}
        override suspend fun deleteAllUsers() {}
        override suspend fun getUserCount(): Int = 0
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        val paydayDayFlow = MutableStateFlow(1)
        val lastCycleFlow = MutableStateFlow("")

        override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
        override val isLoggedIn: Flow<Boolean> = flowOf(true)
        override val activeUserId: Flow<Long> = flowOf(0L)
        override val sessionToken: Flow<String?> = flowOf("token-123")
        override val userNamePreference: Flow<String?> = flowOf("Test User")
        override val userEmailPreference: Flow<String?> = flowOf("test@example.com")
        override val currencySymbol: Flow<String> = flowOf("Rp")
        override val themePreference: Flow<String> = flowOf("SYSTEM")
        override val notificationsEnabled: Flow<Boolean> = flowOf(true)
        override val paydayStartDay: Flow<Int> = paydayDayFlow
        override val lastProcessedPaydayCycle: Flow<String> = lastCycleFlow
        override val lastActiveTimestamp: Flow<Long> = flowOf(0L)

        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setSession(isLoggedIn: Boolean, token: String, name: String, email: String, userId: Long) {}
        override suspend fun clearSession() {}
        override suspend fun updateLastActiveTimestamp(timestamp: Long) {}
        override suspend fun checkAndInvalidateSessionIfExpired(timeoutMillis: Long, currentTime: Long): Boolean = false
        override suspend fun setCurrencySymbol(symbol: String) {}
        override suspend fun setThemePreference(theme: String) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
        override suspend fun setPaydayStartDay(day: Int) { paydayDayFlow.value = day }
        override suspend fun setLastProcessedPaydayCycle(cycleKey: String) { lastCycleFlow.value = cycleKey }
        override suspend fun clearPreferences() {}
    }

    private lateinit var budgetDao: FakeBudgetDao
    private lateinit var catBudgetDao: FakeCategoryBudgetDao
    private lateinit var txDao: FakeTransactionDao
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: BudgetViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        budgetDao = FakeBudgetDao()
        catBudgetDao = FakeCategoryBudgetDao()
        txDao = FakeTransactionDao()
        repository = FinanceRepository(
            transactionDao = txDao,
            budgetDao = budgetDao,
            categoryBudgetDao = catBudgetDao,
            userDao = FakeUserDao(),
            userPreferencesRepository = FakeUserPreferencesRepository()
        )
        viewModel = BudgetViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun budgetViewModel_initialState_loadsDefaultMonthAndZeroBudget() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(DateUtils.getCurrentMonth(), state.selectedMonth)
        assertEquals(DateUtils.getCurrentYear(), state.selectedYear)
        assertEquals(0L, state.totalMonthlyBudget)
        assertEquals(0L, state.totalAllocated)
        assertEquals(0L, state.unallocatedBudget)
        assertTrue(state.categoryAllocations.isEmpty())
        assertTrue(state.budgetAlerts.isEmpty())
    }

    @Test
    fun budgetViewModel_setTotalMonthlyBudget_updatesStateAndUnallocated() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setTotalMonthlyBudget(5000000L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5000000L, state.totalMonthlyBudget)
        assertEquals(5000000L, state.unallocatedBudget)
    }

    @Test
    fun budgetViewModel_saveCategoryAllocation_calculatesAllocationsAndAlerts() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val currentMonth = DateUtils.getCurrentMonth()
        val currentYear = DateUtils.getCurrentYear()
        val startOfMonth = DateUtils.getStartOfMonth(currentYear, currentMonth)

        // Set total budget
        viewModel.setTotalMonthlyBudget(4000000L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Add category allocations
        viewModel.saveCategoryAllocation("Food & Drinks", 1000000L)
        viewModel.saveCategoryAllocation("Transportation", 500000L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Insert expenses:
        // Food & Drinks: 800,000 spent out of 1,000,000 = 80% (APPROACHING_75)
        // Transportation: 550,000 spent out of 500,000 = 110% (EXCEEDED_100)
        txDao.insertTransactions(
            listOf(
                TransactionEntity(id = 1L, title = "Lunch", amount = 800000L, type = "EXPENSE", category = "Food & Drinks", date = startOfMonth + 1000),
                TransactionEntity(id = 2L, title = "Fuel", amount = 550000L, type = "EXPENSE", category = "Transportation", date = startOfMonth + 2000)
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4000000L, state.totalMonthlyBudget)
        assertEquals(1500000L, state.totalAllocated)
        assertEquals(2500000L, state.unallocatedBudget)
        assertEquals(1350000L, state.totalActualExpenses)

        val foodAlloc = state.categoryAllocations.find { it.categoryName == "Food & Drinks" }
        assertNotNull(foodAlloc)
        assertEquals(AlertLevel.APPROACHING_75, foodAlloc?.alertLevel)

        val transportAlloc = state.categoryAllocations.find { it.categoryName == "Transportation" }
        assertNotNull(transportAlloc)
        assertEquals(AlertLevel.EXCEEDED_100, transportAlloc?.alertLevel)

        assertEquals(2, state.budgetAlerts.size)
    }

    @Test
    fun budgetViewModel_deleteCategoryAllocation_removesCategory() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setTotalMonthlyBudget(5000000L)
        viewModel.saveCategoryAllocation("Shopping", 1000000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.categoryAllocations.size)

        viewModel.deleteCategoryAllocation("Shopping")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.categoryAllocations.isEmpty())
    }

    @Test
    fun budgetViewModel_monthNavigation_changesMonthAndYear() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setMonthYear(1, 2024)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.selectedMonth)
        assertEquals(2024, viewModel.uiState.value.selectedYear)

        viewModel.previousMonth()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(12, viewModel.uiState.value.selectedMonth)
        assertEquals(2023, viewModel.uiState.value.selectedYear)

        viewModel.nextMonth()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.selectedMonth)
        assertEquals(2024, viewModel.uiState.value.selectedYear)
    }

    @Test
    fun budgetViewModel_setNegativeBudget_setsErrorMessage() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setTotalMonthlyBudget(-1000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Budget amount cannot be negative", viewModel.uiState.value.errorMessage)
    }
}
