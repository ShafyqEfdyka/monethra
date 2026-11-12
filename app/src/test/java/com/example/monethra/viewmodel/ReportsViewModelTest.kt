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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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
        override suspend fun deleteAllTransactions(userId: Long) {
            txList.value = emptyList()
        }
        override suspend fun getTransactionCount(userId: Long): Int = txList.value.size
    }

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
        override suspend fun updateBudget(budget: BudgetEntity) {}
        override suspend fun deleteBudget(budget: BudgetEntity) {}
        override suspend fun deleteAllBudgets(userId: Long) {
            budgets.value = emptyList()
        }
    }

    private class FakeCategoryBudgetDao : CategoryBudgetDao {
        override fun getCategoryBudgetsForBudget(budgetId: Long): Flow<List<CategoryBudgetEntity>> = flowOf(emptyList())
        override suspend fun getCategoryBudgetsForBudgetSync(budgetId: Long): List<CategoryBudgetEntity> = emptyList()
        override suspend fun insertCategoryBudget(categoryBudget: CategoryBudgetEntity): Long = 1L
        override suspend fun insertCategoryBudgets(categoryBudgets: List<CategoryBudgetEntity>) {}
        override suspend fun updateCategoryBudget(categoryBudget: CategoryBudgetEntity) {}
        override suspend fun deleteCategoryBudget(categoryBudget: CategoryBudgetEntity) {}
        override suspend fun deleteCategoryBudgetsForBudget(budgetId: Long) {}
        override suspend fun deleteAllCategoryBudgets() {}
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

    private lateinit var txDao: FakeTransactionDao
    private lateinit var budgetDao: FakeBudgetDao
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: ReportsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        txDao = FakeTransactionDao()
        budgetDao = FakeBudgetDao()
        repository = FinanceRepository(
            transactionDao = txDao,
            budgetDao = budgetDao,
            categoryBudgetDao = FakeCategoryBudgetDao(),
            userDao = FakeUserDao(),
            userPreferencesRepository = FakeUserPreferencesRepository()
        )
        viewModel = ReportsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reportsViewModel_initialState_computesMonthlyTotalsAndInsights() = runTest {
        val currentMonth = DateUtils.getCurrentMonth()
        val currentYear = DateUtils.getCurrentYear()
        val startOfMonth = DateUtils.getStartOfMonth(currentYear, currentMonth)

        budgetDao.insertBudget(BudgetEntity(id = 1L, month = currentMonth, year = currentYear, totalBudget = 5000000L))

        txDao.insertTransactions(
            listOf(
                TransactionEntity(id = 1L, title = "Salary", amount = 8000000L, type = "INCOME", category = "Salary", date = startOfMonth + 1000),
                TransactionEntity(id = 2L, title = "Supermarket", amount = 1500000L, type = "EXPENSE", category = "Food & Beverages", date = startOfMonth + 2000),
                TransactionEntity(id = 3L, title = "Electric Bill", amount = 500000L, type = "EXPENSE", category = "Bills & Utilities", date = startOfMonth + 3000)
            )
        )

        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(currentMonth, state.selectedMonth)
        assertEquals(currentYear, state.selectedYear)
        assertEquals(8000000L, state.totalIncome)
        assertEquals(2000000L, state.totalExpenses)
        assertEquals(6000000L, state.netSavings)
        assertEquals(3000000L, state.remainingBudget)
        assertEquals(40.0f, state.budgetUtilizationPercentage, 0.1f)
        assertEquals(3, state.transactionCount)
        assertEquals("Food & Beverages", state.highestSpendingCategory)
        assertEquals("Bills & Utilities", state.lowestSpendingCategory)
        assertTrue(state.textualFinancialInsight.contains("Food & Beverages"))
    }

    @Test
    fun reportsViewModel_selectMonthYear_updatesCalculations() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectMonthYear(5, 2024)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.selectedMonth)
        assertEquals(2024, state.selectedYear)
        assertEquals(0L, state.totalIncome)
        assertEquals(0L, state.totalExpenses)
    }

    @Test
    fun reportsViewModel_monthNavigation_movesPreviousAndNext() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectMonthYear(1, 2024)
        testDispatcher.scheduler.advanceUntilIdle()

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
    fun reportsViewModel_paydayCycleLabelAndRecap_computesCorrectly() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.paydayCycleLabel.isNotBlank())

        val subject = com.example.monethra.utils.EmailRecapHelper.generateEmailSubject(state.paydayCycleLabel)
        assertTrue(subject.startsWith("[Monethra] Financial Recap"))

        val body = com.example.monethra.utils.EmailRecapHelper.generateEmailBody(
            userName = "Alice",
            cycleLabel = state.paydayCycleLabel,
            totalIncome = 5000000L,
            totalExpense = 2000000L,
            netSavings = 3000000L,
            budgetUtilization = 40,
            topCategory = "Food & Beverages",
            insight = "Good savings this month."
        )
        assertTrue(body.contains("Alice"))
        assertTrue(body.contains("Food & Beverages"))
    }
}
