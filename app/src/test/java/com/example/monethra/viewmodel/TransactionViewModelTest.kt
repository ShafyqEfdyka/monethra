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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeTransactionDao : TransactionDao {
        val txList = MutableStateFlow<List<TransactionEntity>>(emptyList())

        override fun getAllTransactions(userId: Long): Flow<List<TransactionEntity>> = txList
        override fun getTransactionsByDateRange(startDate: Long, endDate: Long, userId: Long): Flow<List<TransactionEntity>> =
            flowOf(txList.value.filter { it.date in startDate..endDate })
        override fun getRecentTransactions(limit: Int, userId: Long): Flow<List<TransactionEntity>> =
            flowOf(txList.value.sortedByDescending { it.date }.take(limit))
        override fun getTransactionById(id: Long): Flow<TransactionEntity?> =
            flowOf(txList.value.find { it.id == id })
        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val current = txList.value.toMutableList()
            val newId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            val newEntity = transaction.copy(id = newId)
            current.add(newEntity)
            txList.value = current
            return newId
        }
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
            val current = txList.value.toMutableList()
            current.addAll(transactions)
            txList.value = current
        }
        override suspend fun updateTransaction(transaction: TransactionEntity) {
            val current = txList.value.toMutableList()
            val index = current.indexOfFirst { it.id == transaction.id }
            if (index != -1) {
                current[index] = transaction
                txList.value = current
            }
        }
        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            val current = txList.value.toMutableList()
            current.removeAll { it.id == transaction.id }
            txList.value = current
        }
        override suspend fun deleteTransactionById(id: Long) {
            val current = txList.value.toMutableList()
            current.removeAll { it.id == id }
            txList.value = current
        }
        override suspend fun deleteAllTransactions(userId: Long) {
            txList.value = emptyList()
        }
        override suspend fun getTransactionCount(userId: Long): Int = txList.value.size
    }

    private class FakeBudgetDao : BudgetDao {
        override fun getBudgetForMonth(month: Int, year: Int, userId: Long): Flow<BudgetEntity?> = flowOf(null)
        override suspend fun getBudgetForMonthSync(month: Int, year: Int, userId: Long): BudgetEntity? = null
        override fun getAllBudgets(userId: Long): Flow<List<BudgetEntity>> = flowOf(emptyList())
        override suspend fun getBudgetCount(userId: Long): Int = 0
        override suspend fun insertBudget(budget: BudgetEntity): Long = 1L
        override suspend fun updateBudget(budget: BudgetEntity) {}
        override suspend fun deleteBudget(budget: BudgetEntity) {}
        override suspend fun deleteAllBudgets(userId: Long) {}
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
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        txDao = FakeTransactionDao()
        repository = FinanceRepository(
            transactionDao = txDao,
            budgetDao = FakeBudgetDao(),
            categoryBudgetDao = FakeCategoryBudgetDao(),
            userDao = FakeUserDao(),
            userPreferencesRepository = FakeUserPreferencesRepository()
        )
        viewModel = TransactionViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun transactionViewModel_initialState_loadsAllTransactions() = runTest {
        val now = System.currentTimeMillis()
        txDao.insertTransactions(
            listOf(
                TransactionEntity(id = 1L, title = "Salary", amount = 7500000L, type = "INCOME", category = "Salary", date = now),
                TransactionEntity(id = 2L, title = "Grocery", amount = 350000L, type = "EXPENSE", category = "Food & Drinks", date = now)
            )
        )

        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.searchQuery)
        assertEquals("ALL", state.typeFilter)
        assertEquals(2, state.filteredTransactions.size)
    }

    @Test
    fun transactionViewModel_filterByType_filtersIncomeAndExpense() = runTest {
        val now = System.currentTimeMillis()
        txDao.insertTransactions(
            listOf(
                TransactionEntity(id = 1L, title = "Salary", amount = 7500000L, type = "INCOME", category = "Salary", date = now),
                TransactionEntity(id = 2L, title = "Grocery", amount = 350000L, type = "EXPENSE", category = "Food & Drinks", date = now),
                TransactionEntity(id = 3L, title = "Freelance", amount = 1500000L, type = "INCOME", category = "Freelance", date = now)
            )
        )

        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Filter Income
        viewModel.onFilterChange("INCOME")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.filteredTransactions.size)
        assertTrue(viewModel.uiState.value.filteredTransactions.all { it.type == "INCOME" })

        // Filter Expense
        viewModel.onFilterChange("EXPENSE")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)
        assertEquals("Grocery", viewModel.uiState.value.filteredTransactions.first().title)
    }

    @Test
    fun transactionViewModel_searchQuery_filtersByKeyword() = runTest {
        val now = System.currentTimeMillis()
        txDao.insertTransactions(
            listOf(
                TransactionEntity(id = 1L, title = "Monthly Salary", amount = 7500000L, type = "INCOME", category = "Salary", date = now),
                TransactionEntity(id = 2L, title = "Grocery Shopping", amount = 350000L, type = "EXPENSE", category = "Food & Drinks", date = now),
                TransactionEntity(id = 3L, title = "Electricity Bill", amount = 450000L, type = "EXPENSE", category = "Bills & Utilities", date = now)
            )
        )

        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("grocery")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredTransactions.size)
        assertEquals("Grocery Shopping", state.filteredTransactions.first().title)
    }

    @Test
    fun transactionViewModel_addTransaction_validInput_savesSuccessfully() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val now = System.currentTimeMillis()
        viewModel.addTransaction(
            title = "New Shoes",
            amount = 500000L,
            type = "EXPENSE",
            category = "Shopping",
            date = now,
            notes = "Bought online"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.saveSuccess)
        assertNull(state.errorMessage)
        assertEquals(1, state.filteredTransactions.size)
        assertEquals("New Shoes", state.filteredTransactions.first().title)
    }

    @Test
    fun transactionViewModel_addTransaction_invalidInput_setsErrorMessage() = runTest {
        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Invalid title (empty)
        viewModel.addTransaction(
            title = "",
            amount = 500000L,
            type = "EXPENSE",
            category = "Shopping",
            date = System.currentTimeMillis(),
            notes = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Title is required", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun transactionViewModel_deleteTransaction_removesFromList() = runTest {
        val now = System.currentTimeMillis()
        txDao.insertTransactions(
            listOf(
                TransactionEntity(id = 1L, title = "Item 1", amount = 100000L, type = "EXPENSE", category = "Shopping", date = now)
            )
        )

        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val txToDelete = viewModel.uiState.value.filteredTransactions.first()
        viewModel.deleteTransaction(txToDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.filteredTransactions.isEmpty())
    }

    @Test
    fun transactionViewModel_updateTransaction_preservesIdAndUserId() = runTest {
        val now = System.currentTimeMillis()
        val original = TransactionEntity(
            id = 42L,
            title = "Monthly Salary",
            amount = 7500000L,
            type = "INCOME",
            category = "Salary",
            date = now,
            userId = 1L
        )
        txDao.insertTransactions(listOf(original))

        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openEditTransactionSheet(original)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(original, viewModel.uiState.value.selectedTransaction)

        viewModel.updateTransaction(
            id = original.id,
            title = "Monthly Salary Updated",
            amount = 8000000L,
            type = "INCOME",
            category = "Salary",
            date = now,
            notes = "Bonus added"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.saveSuccess)
        assertFalse(state.isAddEditSheetOpen)
        assertNull(state.selectedTransaction)

        val updated = txDao.txList.value.first { it.id == 42L }
        assertEquals(42L, updated.id)
        assertEquals(1L, updated.userId)
        assertEquals("Monthly Salary Updated", updated.title)
        assertEquals(8000000L, updated.amount)
        assertEquals("INCOME", updated.type)
    }

    @Test
    fun transactionViewModel_updateTransaction_entityOverload_preservesIdAndUserId() = runTest {
        val now = System.currentTimeMillis()
        val original = TransactionEntity(
            id = 99L,
            title = "Old Item",
            amount = 100000L,
            type = "EXPENSE",
            category = "Food & Dining",
            date = now,
            userId = 5L
        )
        txDao.insertTransactions(listOf(original))

        backgroundScope.launch {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openEditTransactionSheet(original)
        testDispatcher.scheduler.advanceUntilIdle()

        val modified = original.copy(title = "New Item", amount = 150000L)
        viewModel.updateTransaction(modified)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = txDao.txList.value.first { it.id == 99L }
        assertEquals(99L, updated.id)
        assertEquals(5L, updated.userId)
        assertEquals("New Item", updated.title)
        assertEquals(150000L, updated.amount)
    }
}
