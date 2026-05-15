package com.example.creditcardapp.domain.model

data class CreditCard(
    val id: Long = 0,
    val cardholderName: String,
    val last4: String,
    val brand: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val balance: Double,
    val creditLimit: Double,
    val nickname: String? = null,
    val sourceItemId: String? = null,
    val sourceAccountId: String? = null,
    /** Multiplier (points per $1) per category. Missing categories fall back to 1x. */
    val rewards: Map<RewardCategory, Double> = emptyMap()
) {
    fun multiplierFor(category: RewardCategory): Double =
        rewards[category] ?: rewards[RewardCategory.OTHER] ?: 1.0
}
