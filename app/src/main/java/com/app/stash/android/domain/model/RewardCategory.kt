package com.app.stash.android.domain.model

/**
 * High-level spending category used to score which card earns the most points
 * at a given merchant. Mapped from OpenStreetMap POI tags in PlacesRepository.
 */
enum class RewardCategory {
    DINING,
    GROCERIES,
    GAS,
    TRAVEL,
    SHOPPING,
    ENTERTAINMENT,
    OTHER;

    val displayName: String
        get() = when (this) {
            DINING -> "Dining"
            GROCERIES -> "Groceries"
            GAS -> "Gas"
            TRAVEL -> "Travel"
            SHOPPING -> "Shopping"
            ENTERTAINMENT -> "Entertainment"
            OTHER -> "Everything else"
        }

    companion object {
        /**
         * Maps a Plaid personal-finance category (the `primary` field — e.g.
         * "FOOD_AND_DRINK", "TRANSPORTATION") to one of our high-level reward
         * categories. Returns [OTHER] for unknown / null inputs.
         */
        fun fromPlaidPrimary(primary: String?): RewardCategory {
            if (primary.isNullOrBlank()) return OTHER
            val p = primary.uppercase()
            return when {
                "FOOD_AND_DRINK" in p || "RESTAURANT" in p -> DINING
                "GROCER" in p -> GROCERIES
                "GAS" in p -> GAS
                "TRAVEL" in p || "AIRLINE" in p || "HOTEL" in p || "TRANSPORTATION" in p -> TRAVEL
                "ENTERTAIN" in p || "RECREATION" in p -> ENTERTAINMENT
                "GENERAL_MERCHANDISE" in p || "SHOPPING" in p || "CLOTHING" in p -> SHOPPING
                else -> OTHER
            }
        }
    }
}
