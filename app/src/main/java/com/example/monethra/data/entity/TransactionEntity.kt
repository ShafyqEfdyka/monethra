package com.example.monethra.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Long, // IDR in integer
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val date: Long, // Epoch millis
    val notes: String? = null,
    val userId: Long = 0L
)
