package com.example.monethra.data.repository

import com.example.monethra.data.dao.BudgetDao
import com.example.monethra.data.dao.CategoryBudgetDao
import com.example.monethra.data.dao.TransactionDao
import com.example.monethra.data.dao.UserDao
import com.example.monethra.data.datastore.UserPreferencesRepository
import com.example.monethra.data.entity.BudgetEntity
import com.example.monethra.data.entity.CategoryBudgetEntity
import com.example.monethra.data.entity.TransactionEntity
import com.example.monethra.data.entity.UserEntity
import com.example.monethra.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val categoryBudgetDao: CategoryBudgetDao,
    private val userDao: UserDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    val activeUserId: Flow<Long> = userPreferencesRepository.activeUserId

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = activeUserId.flatMapLatest { userId ->
        transactionDao.getAllTransactions(userId)
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> {
        return activeUserId.flatMapLatest { userId ->
            transactionDao.getTransactionsByDateRange(startDate, endDate, userId)
        }
    }

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> {
        return activeUserId.flatMapLatest { userId ->
            transactionDao.getRecentTransactions(limit, userId)
        }
    }

    fun getTransactionById(id: Long): Flow<TransactionEntity?> {
        return transactionDao.getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val currentUserId = activeUserId.first()
        val txToSave = if (transaction.userId == 0L && currentUserId > 0L) {
            transaction.copy(userId = currentUserId)
        } else {
            transaction
        }
        return transactionDao.insertTransaction(txToSave)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        val currentUserId = activeUserId.first()
        val txToSave = if (transaction.userId == 0L && currentUserId > 0L) {
            transaction.copy(userId = currentUserId)
        } else {
            transaction
        }
        transactionDao.updateTransaction(txToSave)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun deleteAllTransactions() {
        val currentUserId = activeUserId.first()
        transactionDao.deleteAllTransactions(currentUserId)
    }

    // Budgets
    fun getBudgetForMonth(month: Int, year: Int): Flow<BudgetEntity?> {
        return activeUserId.flatMapLatest { userId ->
            budgetDao.getBudgetForMonth(month, year, userId)
        }
    }

    fun getCategoryBudgetsForBudget(budgetId: Long): Flow<List<CategoryBudgetEntity>> {
        return categoryBudgetDao.getCategoryBudgetsForBudget(budgetId)
    }

    suspend fun saveBudget(month: Int, year: Int, totalBudget: Long, categoryAllocations: Map<String, Long>): Long {
        val currentUserId = activeUserId.first()
        val existingBudget = budgetDao.getBudgetForMonthSync(month, year, currentUserId)
        val budgetId = if (existingBudget != null) {
            val updatedBudget = existingBudget.copy(totalBudget = totalBudget)
            budgetDao.updateBudget(updatedBudget)
            existingBudget.id
        } else {
            budgetDao.insertBudget(BudgetEntity(userId = currentUserId, month = month, year = year, totalBudget = totalBudget))
        }

        categoryBudgetDao.deleteCategoryBudgetsForBudget(budgetId)
        val categoryEntities = categoryAllocations.map { (category, amount) ->
            CategoryBudgetEntity(
                budgetId = budgetId,
                categoryName = category,
                allocatedAmount = amount
            )
        }
        categoryBudgetDao.insertCategoryBudgets(categoryEntities)
        return budgetId
    }

    suspend fun setTotalBudget(month: Int, year: Int, totalBudget: Long): Long {
        val currentUserId = activeUserId.first()
        val existingBudget = budgetDao.getBudgetForMonthSync(month, year, currentUserId)
        return if (existingBudget != null) {
            val updatedBudget = existingBudget.copy(totalBudget = totalBudget)
            budgetDao.updateBudget(updatedBudget)
            existingBudget.id
        } else {
            budgetDao.insertBudget(BudgetEntity(userId = currentUserId, month = month, year = year, totalBudget = totalBudget))
        }
    }

    suspend fun saveCategoryAllocation(month: Int, year: Int, categoryName: String, allocatedAmount: Long): Long {
        val currentUserId = activeUserId.first()
        val budget = budgetDao.getBudgetForMonthSync(month, year, currentUserId)
        val budgetId = if (budget != null) {
            budget.id
        } else {
            budgetDao.insertBudget(BudgetEntity(userId = currentUserId, month = month, year = year, totalBudget = 0L))
        }

        val existingCategories = categoryBudgetDao.getCategoryBudgetsForBudgetSync(budgetId)
        val existing = existingCategories.find { it.categoryName.equals(categoryName, ignoreCase = true) }

        if (existing != null) {
            categoryBudgetDao.updateCategoryBudget(existing.copy(allocatedAmount = allocatedAmount))
        } else {
            categoryBudgetDao.insertCategoryBudget(
                CategoryBudgetEntity(
                    budgetId = budgetId,
                    categoryName = categoryName,
                    allocatedAmount = allocatedAmount
                )
            )
        }
        return budgetId
    }

    suspend fun deleteCategoryAllocation(month: Int, year: Int, categoryName: String) {
        val currentUserId = activeUserId.first()
        val budget = budgetDao.getBudgetForMonthSync(month, year, currentUserId) ?: return
        val existingCategories = categoryBudgetDao.getCategoryBudgetsForBudgetSync(budget.id)
        val existing = existingCategories.find { it.categoryName.equals(categoryName, ignoreCase = true) }
        if (existing != null) {
            categoryBudgetDao.deleteCategoryBudget(existing)
        }
    }

    // User Profile
    val userProfile: Flow<UserEntity?> = activeUserId.flatMapLatest { userId ->
        if (userId > 0) userDao.getUserById(userId) else userDao.getUser()
    }

    suspend fun saveUserProfile(name: String, email: String?) {
        val currentUserId = activeUserId.first()
        val existingUser = if (currentUserId > 0) userDao.getUserByIdSync(currentUserId) else userDao.getUserSync()
        if (existingUser != null) {
            userDao.updateUser(existingUser.copy(name = name, email = email))
        } else {
            userDao.insertUser(UserEntity(name = name, email = email))
        }
    }

    // User Preferences
    val isOnboardingCompleted: Flow<Boolean> = userPreferencesRepository.isOnboardingCompleted
    val currencySymbol: Flow<String> = userPreferencesRepository.currencySymbol
    val themePreference: Flow<String> = userPreferencesRepository.themePreference
    val notificationsEnabled: Flow<Boolean> = userPreferencesRepository.notificationsEnabled
    val paydayStartDay: Flow<Int> = userPreferencesRepository.paydayStartDay
    val lastProcessedPaydayCycle: Flow<String> = userPreferencesRepository.lastProcessedPaydayCycle

    suspend fun setOnboardingCompleted(completed: Boolean) {
        userPreferencesRepository.setOnboardingCompleted(completed)
    }

    suspend fun setCurrencySymbol(symbol: String) {
        userPreferencesRepository.setCurrencySymbol(symbol)
    }

    suspend fun setThemePreference(theme: String) {
        userPreferencesRepository.setThemePreference(theme)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        userPreferencesRepository.setNotificationsEnabled(enabled)
    }

    suspend fun setPaydayStartDay(day: Int) {
        userPreferencesRepository.setPaydayStartDay(day)
    }

    suspend fun setLastProcessedPaydayCycle(cycleKey: String) {
        userPreferencesRepository.setLastProcessedPaydayCycle(cycleKey)
    }

    suspend fun carryOverBudgetIfNecessary(targetMonth: Int, targetYear: Int): Boolean {
        val currentUserId = activeUserId.first()
        val existingBudget = budgetDao.getBudgetForMonthSync(targetMonth, targetYear, currentUserId)
        if (existingBudget != null) return false

        val prevMonth = if (targetMonth == 1) 12 else targetMonth - 1
        val prevYear = if (targetMonth == 1) targetYear - 1 else targetYear

        val prevBudget = budgetDao.getBudgetForMonthSync(prevMonth, prevYear, currentUserId) ?: return false

        val newBudgetId = budgetDao.insertBudget(
            BudgetEntity(userId = currentUserId, month = targetMonth, year = targetYear, totalBudget = prevBudget.totalBudget)
        )

        val prevAllocations = categoryBudgetDao.getCategoryBudgetsForBudgetSync(prevBudget.id)
        val newAllocations = prevAllocations.map {
            CategoryBudgetEntity(budgetId = newBudgetId, categoryName = it.categoryName, allocatedAmount = it.allocatedAmount)
        }
        categoryBudgetDao.insertCategoryBudgets(newAllocations)
        return true
    }

    suspend fun clearPreferences() {
        userPreferencesRepository.clearPreferences()
    }

    // Reset & Sample Data
    suspend fun resetFinancialData() {
        val currentUserId = activeUserId.first()
        transactionDao.deleteAllTransactions(currentUserId)
        categoryBudgetDao.deleteAllCategoryBudgets()
        budgetDao.deleteAllBudgets(currentUserId)
    }

    suspend fun loadSampleData() {
        resetFinancialData()
        seedSampleData()
    }

    private suspend fun seedSampleData() {
        val currentUserId = activeUserId.first()
        val existingUser = if (currentUserId > 0) userDao.getUserByIdSync(currentUserId) else userDao.getUserSync()
        val targetUserId = existingUser?.id ?: userDao.insertUser(
            UserEntity(name = "Monethra User", email = "user@monethra.app")
        )

        val currentMonth = DateUtils.getCurrentMonth()
        val currentYear = DateUtils.getCurrentYear()

        val budgetId = budgetDao.insertBudget(
            BudgetEntity(
                userId = targetUserId,
                month = currentMonth,
                year = currentYear,
                totalBudget = 5000000L
            )
        )

        val categoryAllocations = listOf(
            CategoryBudgetEntity(budgetId = budgetId, categoryName = "Food & Beverages", allocatedAmount = 1500000L),
            CategoryBudgetEntity(budgetId = budgetId, categoryName = "Transportation", allocatedAmount = 800000L),
            CategoryBudgetEntity(budgetId = budgetId, categoryName = "Shopping", allocatedAmount = 1000000L),
            CategoryBudgetEntity(budgetId = budgetId, categoryName = "Bills & Utilities", allocatedAmount = 1200000L),
            CategoryBudgetEntity(budgetId = budgetId, categoryName = "Entertainment", allocatedAmount = 500000L)
        )
        categoryBudgetDao.insertCategoryBudgets(categoryAllocations)

        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        val sampleTransactions = listOf(
            // Current month income
            TransactionEntity(
                userId = targetUserId,
                title = "Monthly Salary",
                amount = 7500000L,
                type = "INCOME",
                category = "Salary",
                date = now - (2 * dayMillis),
                notes = "Monthly payroll"
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "Freelance Project",
                amount = 1500000L,
                type = "INCOME",
                category = "Side Income",
                date = now - (5 * dayMillis),
                notes = "UI Design Consultation"
            ),
            // Current month expenses
            TransactionEntity(
                userId = targetUserId,
                title = "Grocery Shopping",
                amount = 350000L,
                type = "EXPENSE",
                category = "Food & Beverages",
                date = now - (1 * dayMillis),
                notes = "Weekly groceries at supermarket"
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "Electricity & Water Bill",
                amount = 450000L,
                type = "EXPENSE",
                category = "Bills & Utilities",
                date = now - (3 * dayMillis),
                notes = "Monthly utilities"
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "Coffee & Pastry",
                amount = 45000L,
                type = "EXPENSE",
                category = "Food & Beverages",
                date = now - (4 * dayMillis),
                notes = "Morning coffee"
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "Fuel Refill",
                amount = 100000L,
                type = "EXPENSE",
                category = "Transportation",
                date = now - (6 * dayMillis),
                notes = "Pertamax full tank"
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "Dinner with Friends",
                amount = 180000L,
                type = "EXPENSE",
                category = "Food & Beverages",
                date = now - (7 * dayMillis),
                notes = "Weekend gathering"
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "Streaming Subscription",
                amount = 150000L,
                type = "EXPENSE",
                category = "Bills & Utilities",
                date = now - (10 * dayMillis)
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "New Wireless Earbuds",
                amount = 450000L,
                type = "EXPENSE",
                category = "Shopping",
                date = now - (12 * dayMillis)
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "Movie & Snacks",
                amount = 120000L,
                type = "EXPENSE",
                category = "Entertainment",
                date = now - (14 * dayMillis)
            ),

            // Previous month transactions
            TransactionEntity(
                userId = targetUserId,
                title = "Previous Salary",
                amount = 7500000L,
                type = "INCOME",
                category = "Salary",
                date = now - (32 * dayMillis)
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "House Rent",
                amount = 1500000L,
                type = "EXPENSE",
                category = "Bills & Utilities",
                date = now - (33 * dayMillis)
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "Monthly Supermarket",
                amount = 650000L,
                type = "EXPENSE",
                category = "Food & Beverages",
                date = now - (35 * dayMillis)
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "Train Pass",
                amount = 250000L,
                type = "EXPENSE",
                category = "Transportation",
                date = now - (38 * dayMillis)
            ),
            TransactionEntity(
                userId = targetUserId,
                title = "Clothes Purchase",
                amount = 350000L,
                type = "EXPENSE",
                category = "Shopping",
                date = now - (40 * dayMillis)
            )
        )

        transactionDao.insertTransactions(sampleTransactions)
    }

    suspend fun seedInitialDataIfEmpty() {
        // Disabled automatic initial dummy data seeding.
    }
}
