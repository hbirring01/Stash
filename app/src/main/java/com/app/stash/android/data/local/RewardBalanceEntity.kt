package com.app.stash.android.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A manually-tracked rewards-program balance (e.g. Chase UR points, Amex MR
 * points, AAdvantage miles). Linked to a [CreditCardEntity] by [cardId]; if the
 * card is deleted, balances are orphaned (kept as soft history rather than
 * cascade-deleted, since the user might still own the points after closing).
 */
@Entity(tableName = "reward_balances", indices = [Index("cardId")])
data class RewardBalanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    /** e.g. "Chase Ultimate Rewards", "Amex Membership Rewards", "Citi ThankYou". */
    val programName: String,
    val points: Double,
    /** Estimated cash value of 1 point in cents (1.0 = 1¢/pt; 1.5 = 1.5¢/pt). */
    val valuePerPointCents: Double = 1.0,
    val updatedAt: Long = System.currentTimeMillis(),
)
