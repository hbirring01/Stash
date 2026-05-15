package com.example.creditcardapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardholderName: String,
    val last4: String,
    val brand: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val balance: Double,
    val creditLimit: Double,
    val nickname: String?,
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceItemId: String? = null,
    val sourceAccountId: String? = null,
    /** JSON object: {"DINING":3.0,"TRAVEL":2.0}. Null means base 1x on everything. */
    val rewardsJson: String? = null
)
