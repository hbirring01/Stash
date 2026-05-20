package com.app.stash.android.data.local

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
    val rewardsJson: String? = null,
    // --- Annual-fee ROI ---
    /** Annual fee charged for holding the card. 0 = no fee. */
    val annualFee: Double = 0.0,
    /** Estimated cash value of 1 reward "point" in cents (default 1¢/pt). */
    val pointValueCents: Double = 1.0,
    // --- Signup bonus tracking ---
    /** Spend (in $) required to earn the signup bonus. 0 = no active bonus. */
    val signupBonusRequiredSpend: Double = 0.0,
    /** Spend ($) already counted toward the signup bonus (manual or aggregated). */
    val signupBonusEarnedSpend: Double = 0.0,
    /** Cash value ($) of the signup bonus once earned. */
    val signupBonusValue: Double = 0.0,
    /** Epoch millis deadline by which the requirement must be met. Null = no deadline. */
    val signupBonusDeadline: Long? = null,
)
