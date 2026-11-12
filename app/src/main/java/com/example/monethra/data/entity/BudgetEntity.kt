package com.example.monethra.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: Int, // 1..12
    val year: Int,
    val totalBudget: Long,
    val userId: Long = 0L
)
