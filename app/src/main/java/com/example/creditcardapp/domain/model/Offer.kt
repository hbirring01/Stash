package com.example.creditcardapp.domain.model

/**
 * A curated or user-tracked card-linked offer (Amex Offers, Chase Offers,
 * etc). StashApp surfaces these and deep-links to the issuer's own app — it
 * never auto-enrolls.
 */
data class Offer(
    val id: Long = 0,
    val merchantPattern: String,
    val merchantDisplay: String,
    val issuer: String,
    val cardLast4: String? = null,
    val rewardKind: RewardKind,
    val rewardValue: Double,
    val capDollars: Double? = null,
    val minSpendDollars: Double = 0.0,
    val expiresAt: Long,
    val activatedAt: Long? = null,
    val source: String = "CURATED",
    val deepLinkUri: String? = null,
    val description: String? = null,
) {
    val isActivated: Boolean get() = activatedAt != null
    fun isActive(now: Long = System.currentTimeMillis()): Boolean = expiresAt >= now
    fun matchesMerchant(displayName: String): Boolean =
        displayName.lowercase().contains(merchantPattern)

    /** Human-readable summary like "10% back, up to $5". */
    fun shortLabel(): String = when (rewardKind) {
        RewardKind.PERCENT -> {
            val pct = if (rewardValue % 1.0 == 0.0) rewardValue.toInt().toString() else "%.1f".format(rewardValue)
            buildString {
                append("$pct% back")
                capDollars?.let { append(", up to $${it.toInt()}") }
            }
        }
        RewardKind.FLAT -> buildString {
            append("$${rewardValue.toInt()} back")
            if (minSpendDollars > 0) append(" on $${minSpendDollars.toInt()}+")
        }
        RewardKind.POINTS_MULT -> {
            val m = if (rewardValue % 1.0 == 0.0) rewardValue.toInt().toString() else "%.1f".format(rewardValue)
            buildString {
                append("${m}× points")
                capDollars?.let { append(", up to $${it.toInt()}") }
            }
        }
    }
}

enum class RewardKind { PERCENT, FLAT, POINTS_MULT }
