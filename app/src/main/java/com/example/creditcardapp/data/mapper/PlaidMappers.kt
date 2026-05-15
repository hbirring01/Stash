package com.example.creditcardapp.data.mapper

import com.example.creditcardapp.data.local.CreditCardEntity
import com.example.creditcardapp.data.plaid.LiabilitiesGetResponse
import com.example.creditcardapp.data.plaid.PlaidAccount

fun LiabilitiesGetResponse.toCreditCardEntities(itemId: String): List<CreditCardEntity> {
    val creditAccountIds = liabilities.credit.map { it.accountId }.toSet()
    val now = System.currentTimeMillis()
    return accounts
        .filter { it.accountId in creditAccountIds || it.type == "credit" }
        .map { acct -> acct.toCreditCardEntity(itemId, now) }
}

private fun PlaidAccount.toCreditCardEntity(itemId: String, now: Long): CreditCardEntity {
    val displayName = officialName ?: name
    return CreditCardEntity(
        cardholderName = "",
        last4 = mask?.padStart(4, '0')?.takeLast(4) ?: "0000",
        brand = guessBrandFromName(displayName),
        expiryMonth = 0,
        expiryYear = 0,
        balance = balances.current ?: 0.0,
        creditLimit = balances.limit ?: 0.0,
        nickname = displayName,
        updatedAt = now,
        sourceItemId = itemId,
        sourceAccountId = accountId,
    )
}

private fun guessBrandFromName(name: String): String {
    val n = name.lowercase()
    return when {
        "visa" in n -> "Visa"
        "master" in n -> "Mastercard"
        "amex" in n || "american express" in n -> "Amex"
        "discover" in n -> "Discover"
        else -> "Card"
    }
}
