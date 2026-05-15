package com.example.creditcardapp.ui.navigation

sealed class Destination(val route: String) {
    /** The swipe-able home (Wallet / Map / Settings pager). */
    data object Home : Destination("home")
    data object CardList : Destination("cards")
    data object AddCard : Destination("cards/add")
    data object PlaidSetup : Destination("settings/plaid")
    data object Transactions : Destination("cards/{cardId}/transactions") {
        const val ARG_CARD_ID = "cardId"
        fun build(cardId: Long) = "cards/$cardId/transactions"
    }
    data object RewardsMap : Destination("rewards/map")
}
