package com.example.monethra.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.monethra.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE (:userId = 0 OR userId = :userId) AND month = :month AND year = :year LIMIT 1")
    fun getBudgetForMonth(month: Int, year: Int, userId: Long = 0L): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE (:userId = 0 OR userId = :userId) AND month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetForMonthSync(month: Int, year: Int, userId: Long = 0L): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE (:userId = 0 OR userId = :userId) ORDER BY year DESC, month DESC")
    fun getAllBudgets(userId: Long = 0L): Flow<List<BudgetEntity>>

    @Query("SELECT COUNT(*) FROM budgets WHERE (:userId = 0 OR userId = :userId)")
    suspend fun getBudgetCount(userId: Long = 0L): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE (:userId = 0 OR userId = :userId)")
    suspend fun deleteAllBudgets(userId: Long = 0L)
}
