package com.app.stash.android.domain.model

/**
 * A time-bounded bonus category attached to a card. Used by the Rewards
 * Calendar to surface "5% on gas this quarter"-style promos.
 */
data class RotatingCategory(
    val id: Long = 0,
    val cardId: Long,
    val category: RewardCategory,
    val multiplier: Double,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val label: String? = null,
) {
    fun isActive(now: Long = System.currentTimeMillis()): Boolean =
        now in startEpochMillis..endEpochMillis

    fun isUpcoming(now: Long = System.currentTimeMillis()): Boolean =
        now < startEpochMillis
}
