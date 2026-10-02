package com.app.stash.android.domain.model

/**
 * A points/miles balance for a specific rewards program. Manually tracked by
 * the user — most issuers don't expose balances via Plaid.
 */
data class RewardBalance(
    val id: Long = 0,
    val cardId: Long,
    val programName: String,
    val points: Double,
    val valuePerPointCents: Double = 1.0,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** Estimated cash value of this balance, in dollars. */
    val estimatedValue: Double
        get() = points * valuePerPointCents / 100.0
}
