package com.app.stash.android.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index("accountId"), Index("date")]
)
data class TransactionEntity(
    @PrimaryKey val transactionId: String,
    val itemId: String,
    val accountId: String,
    val amount: Double,
    val isoCurrencyCode: String?,
    val date: String,
    val authorizedDate: String?,
    val name: String,
    val merchantName: String?,
    val pending: Boolean,
    val categoryPrimary: String?,
    val categoryDetailed: String?,
    val paymentChannel: String?,
    val updatedAt: Long = System.currentTimeMillis(),
)
