package com.example.monethra.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.monethra.data.dao.BudgetDao
import com.example.monethra.data.dao.CategoryBudgetDao
import com.example.monethra.data.dao.CustomCurrencyDao
import com.example.monethra.data.dao.TransactionDao
import com.example.monethra.data.dao.UserDao
import com.example.monethra.data.entity.BudgetEntity
import com.example.monethra.data.entity.CategoryBudgetEntity
import com.example.monethra.data.entity.CustomCurrencyEntity
import com.example.monethra.data.entity.TransactionEntity
import com.example.monethra.data.entity.UserEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        CategoryBudgetEntity::class,
        UserEntity::class,
        CustomCurrencyEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class MonethraDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao
    abstract fun userDao(): UserDao
    abstract fun customCurrencyDao(): CustomCurrencyDao

    companion object {
        @Volatile
        private var INSTANCE: MonethraDatabase? = null

        val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createCustomCurrenciesTable(db)
            }
        }

        val MIGRATION_2_5 = object : Migration(2, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createCustomCurrenciesTable(db)
            }
        }

        val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createCustomCurrenciesTable(db)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createCustomCurrenciesTable(db)
            }
        }

        private fun createCustomCurrenciesTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `custom_currencies` (
                    `code` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `symbol` TEXT NOT NULL,
                    `rateFromIdr` REAL NOT NULL,
                    PRIMARY KEY(`code`)
                )
                """.trimIndent()
            )
        }

        fun getDatabase(context: Context): MonethraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MonethraDatabase::class.java,
                    "monethra_db",
                )
                    .addMigrations(MIGRATION_1_5, MIGRATION_2_5, MIGRATION_3_5, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
