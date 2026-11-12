package com.example.monethra.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.monethra.data.dao.BudgetDao
import com.example.monethra.data.dao.CategoryBudgetDao
import com.example.monethra.data.dao.TransactionDao
import com.example.monethra.data.dao.UserDao
import com.example.monethra.data.entity.BudgetEntity
import com.example.monethra.data.entity.CategoryBudgetEntity
import com.example.monethra.data.entity.TransactionEntity
import com.example.monethra.data.entity.UserEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        CategoryBudgetEntity::class,
        UserEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class MonethraDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: MonethraDatabase? = null

        fun getDatabase(context: Context): MonethraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MonethraDatabase::class.java,
                    "monethra_db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
