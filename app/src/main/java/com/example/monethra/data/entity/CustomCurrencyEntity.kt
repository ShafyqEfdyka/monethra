package com.example.monethra.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_currencies")
data class CustomCurrencyEntity(
    @PrimaryKey
    val code: String,          // 3-letter ISO code uppercase (e.g. "JPY", "EUR")
    val name: String,          // e.g. "Japanese Yen"
    val symbol: String,        // e.g. "¥"
    val rateFromIdr: Double    // Rate relative to 1 IDR (1 IDR = rateFromIdr in this currency)
)
