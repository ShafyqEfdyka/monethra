package com.example.monethra

import android.app.Application
import com.example.monethra.data.database.MonethraDatabase
import com.example.monethra.data.datastore.UserPreferencesRepository
import com.example.monethra.data.datastore.UserPreferencesRepositoryImpl
import com.example.monethra.data.repository.AuthRepository
import com.example.monethra.data.repository.FinanceRepository

class MonethraApplication : Application() {

    lateinit var database: MonethraDatabase
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var financeRepository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = MonethraDatabase.getDatabase(this)
        userPreferencesRepository = UserPreferencesRepositoryImpl(this)
        authRepository = AuthRepository(
            userDao = database.userDao(),
            userPreferencesRepository = userPreferencesRepository
        )
        financeRepository = FinanceRepository(
            transactionDao = database.transactionDao(),
            budgetDao = database.budgetDao(),
            categoryBudgetDao = database.categoryBudgetDao(),
            userDao = database.userDao(),
            userPreferencesRepository = userPreferencesRepository
        )
    }
}
