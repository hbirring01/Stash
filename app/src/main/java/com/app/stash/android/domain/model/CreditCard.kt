package com.app.stash.android.domain.model

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
    val rewards: Map<RewardCategory, Double> = emptyMap(),
    /** Annual fee in dollars (0 = no fee). */
    val annualFee: Double = 0.0,
    /** Estimated cash value of 1 reward "point" in cents. Used for ROI math. */
    val pointValueCents: Double = 1.0,
    /** Spend required to earn the signup bonus. 0 = no active bonus. */
    val signupBonusRequiredSpend: Double = 0.0,
    /** Spend already counted toward the signup bonus (manual or aggregated). */
    val signupBonusEarnedSpend: Double = 0.0,
    /** Dollar value of the signup bonus once earned. */
    val signupBonusValue: Double = 0.0,
    /** Epoch millis deadline for earning the bonus. Null = no deadline. */
    val signupBonusDeadline: Long? = null,
) {
    fun multiplierFor(category: RewardCategory): Double =
        rewards[category] ?: rewards[RewardCategory.OTHER] ?: 1.0

    /** Has an unmet (still in-progress) signup bonus. */
    val hasActiveSignupBonus: Boolean
        get() = signupBonusRequiredSpend > 0.0 &&
            signupBonusEarnedSpend < signupBonusRequiredSpend

    /** Progress [0..1] toward the signup bonus. */
    val signupBonusProgress: Float
        get() = if (signupBonusRequiredSpend <= 0.0) 0f
        else (signupBonusEarnedSpend / signupBonusRequiredSpend).coerceIn(0.0, 1.0).toFloat()
}
