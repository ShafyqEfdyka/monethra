package com.example.monethra.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.monethra.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE (:userId = 0 OR userId = :userId) ORDER BY date DESC")
    fun getAllTransactions(userId: Long = 0L): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE (:userId = 0 OR userId = :userId) AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long, userId: Long = 0L): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE (:userId = 0 OR userId = :userId) ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int, userId: Long = 0L): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT COUNT(*) FROM transactions WHERE (:userId = 0 OR userId = :userId)")
    suspend fun getTransactionCount(userId: Long = 0L): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE (:userId = 0 OR userId = :userId)")
    suspend fun deleteAllTransactions(userId: Long = 0L)
}
