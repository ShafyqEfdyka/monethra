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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeUserDao : UserDao {
        val userFlow = MutableStateFlow<UserEntity?>(UserEntity(1L, "Alice", "alice@example.com"))

        override fun getUser(): Flow<UserEntity?> = userFlow
        override suspend fun getUserSync(): UserEntity? = userFlow.value
        override fun getUserById(userId: Long): Flow<UserEntity?> = userFlow
        override suspend fun getUserByIdSync(userId: Long): UserEntity? = if (userFlow.value?.id == userId) userFlow.value else null
        override suspend fun getUserByEmail(email: String): UserEntity? = if (userFlow.value?.email == email) userFlow.value else null
        override suspend fun insertUser(user: UserEntity): Long {
            userFlow.value = user
            return 1L
        }
        override suspend fun updateUser(user: UserEntity) {
            userFlow.value = user
        }
        override suspend fun deleteUser(user: UserEntity) {
            userFlow.value = null
        }
        override suspend fun deleteAllUsers() {
            userFlow.value = null
        }
        override suspend fun getUserCount(): Int = if (userFlow.value != null) 1 else 0
    }

    private class FakeTransactionDao : TransactionDao {
        val txList = MutableStateFlow<List<TransactionEntity>>(emptyList())
        override fun getAllTransactions(userId: Long): Flow<List<TransactionEntity>> = txList
        override fun getTransactionsByDateRange(startDate: Long, endDate: Long, userId: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getRecentTransactions(limit: Int, userId: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionById(id: Long): Flow<TransactionEntity?> = flowOf(null)
        override suspend fun insertTransaction(transaction: TransactionEntity): Long = 1L
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
        override fun getBudgetForMonth(month: Int, year: Int, userId: Long): Flow<BudgetEntity?> = flowOf(null)
        override suspend fun getBudgetForMonthSync(month: Int, year: Int, userId: Long): BudgetEntity? = null
        override fun getAllBudgets(userId: Long): Flow<List<BudgetEntity>> = budgets
        override suspend fun getBudgetCount(userId: Long): Int = budgets.value.size
        override suspend fun insertBudget(budget: BudgetEntity): Long = 1L
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

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        val symbolFlow = MutableStateFlow("Rp")
        val themeFlow = MutableStateFlow("SYSTEM")
        val notificationsFlow = MutableStateFlow(true)
        val paydayFlow = MutableStateFlow(1)
        val lastCycleFlow = MutableStateFlow("")

        override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
        override val isLoggedIn: Flow<Boolean> = flowOf(true)
        override val activeUserId: Flow<Long> = flowOf(1L)
        override val sessionToken: Flow<String?> = flowOf("token-123")
        override val userNamePreference: Flow<String?> = flowOf("Alice")
        override val userEmailPreference: Flow<String?> = flowOf("alice@example.com")
        override val currencySymbol: Flow<String> = symbolFlow
        override val themePreference: Flow<String> = themeFlow
        override val notificationsEnabled: Flow<Boolean> = notificationsFlow
        override val paydayStartDay: Flow<Int> = paydayFlow
        override val lastProcessedPaydayCycle: Flow<String> = lastCycleFlow
        override val lastActiveTimestamp: Flow<Long> = flowOf(0L)

        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setSession(isLoggedIn: Boolean, token: String, name: String, email: String, userId: Long) {}
        override suspend fun clearSession() {}
        override suspend fun updateLastActiveTimestamp(timestamp: Long) {}
        override suspend fun checkAndInvalidateSessionIfExpired(timeoutMillis: Long, currentTime: Long): Boolean = false
        override suspend fun setCurrencySymbol(symbol: String) {
            symbolFlow.value = symbol
        }
        override suspend fun setThemePreference(theme: String) {
            themeFlow.value = theme
        }
        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            notificationsFlow.value = enabled
        }
        override suspend fun setPaydayStartDay(day: Int) {
            paydayFlow.value = day
        }
        override suspend fun setLastProcessedPaydayCycle(cycleKey: String) {
            lastCycleFlow.value = cycleKey
        }
        override suspend fun clearPreferences() {}
    }

    private lateinit var userDao: FakeUserDao
    private lateinit var txDao: FakeTransactionDao
    private lateinit var prefsRepo: FakeUserPreferencesRepository
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userDao = FakeUserDao()
        txDao = FakeTransactionDao()
        prefsRepo = FakeUserPreferencesRepository()
        repository = FinanceRepository(
            transactionDao = txDao,
            budgetDao = FakeBudgetDao(),
            categoryBudgetDao = FakeCategoryBudgetDao(),
            userDao = userDao,
            userPreferencesRepository = prefsRepo
        )
        viewModel = ProfileViewModel(repository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun profileViewModel_initialState_loadsProfileAndPreferences() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Alice", state.userName)
        assertEquals("alice@example.com", state.userEmail)
        assertEquals("Rp", state.currencySymbol)
        assertEquals("SYSTEM", state.themePreference)
        assertTrue(state.notificationsEnabled)
    }

    @Test
    fun profileViewModel_updateProfile_savesNewProfileDetails() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateProfile("Bob Marley", "bob@example.com")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Bob Marley", state.userName)
        assertEquals("bob@example.com", state.userEmail)
        assertNotNull(state.userMessage)
    }

    @Test
    fun profileViewModel_updateCurrency_updatesPreference() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateCurrency("$")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("$", viewModel.uiState.value.currencySymbol)
    }

    @Test
    fun profileViewModel_updateTheme_updatesThemePreference() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTheme("DARK")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("DARK", viewModel.uiState.value.themePreference)
    }

    @Test
    fun profileViewModel_resetFinancialData_clearsTransactionsAndNotifies() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetFinancialData()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("All financial data has been reset", viewModel.uiState.value.userMessage)
    }

    @Test
    fun profileViewModel_loadSampleData_populatesDataAndNotifies() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadSampleData()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Sample data loaded successfully", viewModel.uiState.value.userMessage)
        assertTrue(txDao.txList.value.isNotEmpty())
    }

    @Test
    fun profileViewModel_updatePaydayStartDay_updatesPreference() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updatePaydayStartDay(25)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(25, viewModel.uiState.value.paydayStartDay)
    }
}
