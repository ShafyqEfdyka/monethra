package com.example.monethra.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.monethra.data.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {
    @Query("SELECT * FROM category_budgets WHERE budgetId = :budgetId")
    fun getCategoryBudgetsForBudget(budgetId: Long): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM category_budgets WHERE budgetId = :budgetId")
    suspend fun getCategoryBudgetsForBudgetSync(budgetId: Long): List<CategoryBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudget(categoryBudget: CategoryBudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudgets(categoryBudgets: List<CategoryBudgetEntity>)

    @Update
    suspend fun updateCategoryBudget(categoryBudget: CategoryBudgetEntity)

    @Query("DELETE FROM category_budgets WHERE budgetId = :budgetId")
    suspend fun deleteCategoryBudgetsForBudget(budgetId: Long)

    @Delete
    suspend fun deleteCategoryBudget(categoryBudget: CategoryBudgetEntity)

    @Query("DELETE FROM category_budgets")
    suspend fun deleteAllCategoryBudgets()
}
