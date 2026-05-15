package com.example.creditcardapp.domain.model

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
}
