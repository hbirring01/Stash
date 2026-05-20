package com.app.stash.android.data.mapper

import com.app.stash.android.data.local.TransactionEntity
import com.app.stash.android.data.plaid.PlaidTransaction

fun PlaidTransaction.toEntity(itemId: String): TransactionEntity = TransactionEntity(
    transactionId = transactionId,
    itemId = itemId,
    accountId = accountId,
    amount = amount,
    isoCurrencyCode = isoCurrencyCode,
    date = date,
    authorizedDate = authorizedDate,
    name = name,
    merchantName = merchantName,
    pending = pending,
    categoryPrimary = personalFinanceCategory?.primary ?: category.firstOrNull(),
    categoryDetailed = personalFinanceCategory?.detailed,
    paymentChannel = paymentChannel,
)
