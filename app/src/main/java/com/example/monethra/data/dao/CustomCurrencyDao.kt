package com.example.monethra.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.monethra.data.entity.CustomCurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCurrencyDao {
    @Query("SELECT * FROM custom_currencies ORDER BY code ASC")
    fun getAllCustomCurrencies(): Flow<List<CustomCurrencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCurrency(currency: CustomCurrencyEntity)

    @Delete
    suspend fun deleteCustomCurrency(currency: CustomCurrencyEntity)

    @Query("DELETE FROM custom_currencies WHERE code = :code")
    suspend fun deleteCustomCurrencyByCode(code: String)
}
