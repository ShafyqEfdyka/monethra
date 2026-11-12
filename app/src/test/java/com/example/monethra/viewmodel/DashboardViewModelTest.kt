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
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeTransactionDao : TransactionDao {
        val txList = mutableListOf<TransactionEntity>()
        override fun getAllTransactions(userId: Long): Flow<List<TransactionEntity>> = flowOf(txList)
        override fun getTransactionsByDateRange(startDate: Long, endDate: Long, userId: Long): Flow<List<TransactionEntity>> =
            flowOf(txList.filter { it.date in startDate..endDate })
        override fun getRecentTransactions(limit: Int, userId: Long): Flow<List<TransactionEntity>> =
            flowOf(txList.sortedByDescending { it.date }.take(limit))
        override fun getTransactionById(id: Long): Flow<TransactionEntity?> = flowOf(txList.find { it.id == id })
        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            txList.add(transaction)
            return txList.size.toLong()
        }
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
            txList.addAll(transactions)
        }
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override suspend fun deleteAllTransactions(userId: Long) { txList.clear() }
        override suspend fun getTransactionCount(userId: Long): Int = txList.size
    }

    private class FakeBudgetDao : BudgetDao {
        var budget: BudgetEntity? = null
        override fun getBudgetForMonth(month: Int, year: Int, userId: Long): Flow<BudgetEntity?> = flowOf(budget)
        override suspend fun getBudgetForMonthSync(month: Int, year: Int, userId: Long): BudgetEntity? = budget
        override fun getAllBudgets(userId: Long): Flow<List<BudgetEntity>> = flowOf(listOfNotNull(budget))
        override suspend fun getBudgetCount(userId: Long): Int = if (budget != null) 1 else 0
        override suspend fun insertBudget(budget: BudgetEntity): Long {
            this.budget = budget
            return 1L
        }
        override suspend fun updateBudget(budget: BudgetEntity) { this.budget = budget }
        override suspend fun deleteBudget(budget: BudgetEntity) { this.budget = null }
        override suspend fun deleteAllBudgets(userId: Long) { this.budget = null }
    }

    private class FakeCategoryBudgetDao : CategoryBudgetDao {
        val list = mutableListOf<CategoryBudgetEntity>()
        override fun getCategoryBudgetsForBudget(budgetId: Long): Flow<List<CategoryBudgetEntity>> = flowOf(list)
        override suspend fun getCategoryBudgetsForBudgetSync(budgetId: Long): List<CategoryBudgetEntity> = list
        override suspend fun insertCategoryBudget(categoryBudget: CategoryBudgetEntity): Long {
            list.add(categoryBudget)
            return 1L
        }
        override suspend fun insertCategoryBudgets(categoryBudgets: List<CategoryBudgetEntity>) { list.addAll(categoryBudgets) }
        override suspend fun updateCategoryBudget(categoryBudget: CategoryBudgetEntity) {}
        override suspend fun deleteCategoryBudget(categoryBudget: CategoryBudgetEntity) {}
        override suspend fun deleteCategoryBudgetsForBudget(budgetId: Long) { list.clear() }
        override suspend fun deleteAllCategoryBudgets() { list.clear() }
    }

    private class FakeUserDao : UserDao {
        var user: UserEntity? = UserEntity(1L, "Test User", "test@example.com")
        override fun getUser(): Flow<UserEntity?> = flowOf(user)
        override suspend fun getUserSync(): UserEntity? = user
        override fun getUserById(userId: Long): Flow<UserEntity?> = flowOf(if (user?.id == userId) user else null)
        override suspend fun getUserByIdSync(userId: Long): UserEntity? = if (user?.id == userId) user else null
        override suspend fun getUserByEmail(email: String): UserEntity? = if (user?.email == email) user else null
        override suspend fun insertUser(user: UserEntity): Long {
            this.user = user
            return 1L
        }
        override suspend fun updateUser(user: UserEntity) { this.user = user }
        override suspend fun deleteUser(user: UserEntity) { this.user = null }
        override suspend fun deleteAllUsers() { this.user = null }
        override suspend fun getUserCount(): Int = if (user != null) 1 else 0
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        val paydayDayFlow = MutableStateFlow(1)
        val lastCycleFlow = MutableStateFlow("")

        override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
        override val isLoggedIn: Flow<Boolean> = flowOf(true)
        override val activeUserId: Flow<Long> = flowOf(1L)
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dashboardViewModel_computesUiStateCorrectly() = runTest {
        val txDao = FakeTransactionDao()
        val budgetDao = FakeBudgetDao()
        val catBudgetDao = FakeCategoryBudgetDao()
        val userDao = FakeUserDao()

        val now = System.currentTimeMillis()
        val currentMonth = DateUtils.getCurrentMonth()
        val currentYear = DateUtils.getCurrentYear()

        budgetDao.budget = BudgetEntity(id = 1L, month = currentMonth, year = currentYear, totalBudget = 5000000L)

        txDao.txList.addAll(
            listOf(
                TransactionEntity(id = 1L, title = "Salary", amount = 10000000L, type = "INCOME", category = "Salary", date = now),
                TransactionEntity(id = 2L, title = "Groceries", amount = 2000000L, type = "EXPENSE", category = "Food & Beverages", date = now),
                TransactionEntity(id = 3L, title = "Transport", amount = 500000L, type = "EXPENSE", category = "Transportation", date = now)
            )
        )

        val repository = FinanceRepository(
            transactionDao = txDao,
            budgetDao = budgetDao,
            categoryBudgetDao = catBudgetDao,
            userDao = userDao,
            userPreferencesRepository = FakeUserPreferencesRepository()
        )

        val viewModel = DashboardViewModel(repository)

        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals("Test User", state.userName)
        assertEquals(7500000L, state.totalBalance) // 10,000,000 - 2,500,000
        assertEquals(10000000L, state.monthlyIncome)
        assertEquals(2500000L, state.monthlyExpenses)
        assertEquals(5000000L, state.monthlyBudget)
        assertEquals(2500000L, state.remainingBudget)
        assertEquals(0.5f, state.budgetUsagePercentage, 0.01f)
        assertEquals(2, state.categoryExpenses.size)
        assertEquals(3, state.recentTransactions.size)
    }

    @Test
    fun dashboardViewModel_handlesNewPaydayCycleBanner() = runTest {
        val prefsRepo = FakeUserPreferencesRepository()
        prefsRepo.lastCycleFlow.value = "2024-10-25" // Previous cycle

        val repository = FinanceRepository(
            transactionDao = FakeTransactionDao(),
            budgetDao = FakeBudgetDao(),
            categoryBudgetDao = FakeCategoryBudgetDao(),
            userDao = FakeUserDao(),
            userPreferencesRepository = prefsRepo
        )

        val viewModel = DashboardViewModel(repository)

        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showPaydayBanner)
        assertEquals("New Payday Cycle Started!", state.paydayBannerMessage)

        viewModel.dismissPaydayBanner()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showPaydayBanner)
    }
}
