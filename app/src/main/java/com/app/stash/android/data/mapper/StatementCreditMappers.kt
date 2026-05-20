package com.app.stash.android.data.mapper

import com.app.stash.android.data.local.StatementCreditEntity
import com.app.stash.android.data.local.StatementCreditUsageEntity
import com.app.stash.android.domain.model.CreditCategory
import com.app.stash.android.domain.model.CreditPeriod
import com.app.stash.android.domain.model.StatementCredit
import com.app.stash.android.domain.model.StatementCreditUsage

fun StatementCreditEntity.toDomain(): StatementCredit = StatementCredit(
    id = id,
    cardId = cardId,
    name = name,
    amountDollars = amountDollars,
    periodKind = runCatching { CreditPeriod.valueOf(periodKind) }.getOrDefault(CreditPeriod.ANNUAL),
    periodStartMonth = periodStartMonth,
    periodStartDay = periodStartDay,
    category = runCatching { CreditCategory.valueOf(category) }.getOrDefault(CreditCategory.OTHER),
    notes = notes,
    source = source,
    matchPattern = matchPattern,
    matchCategory = matchCategory,
    autoTrack = autoTrack,
    createdAt = createdAt,
)

fun StatementCredit.toEntity(): StatementCreditEntity = StatementCreditEntity(
    id = id,
    cardId = cardId,
    name = name,
    amountDollars = amountDollars,
    periodKind = periodKind.name,
    periodStartMonth = periodStartMonth,
    periodStartDay = periodStartDay,
    category = category.name,
    notes = notes,
    source = source,
    matchPattern = matchPattern,
    matchCategory = matchCategory,
    autoTrack = autoTrack,
    createdAt = createdAt,
)

fun StatementCreditUsageEntity.toDomain(): StatementCreditUsage = StatementCreditUsage(
    id = id,
    creditId = creditId,
    amountDollars = amountDollars,
    usedAt = usedAt,
    description = description,
    transactionId = transactionId,
    source = source,
)

fun StatementCreditUsage.toEntity(): StatementCreditUsageEntity = StatementCreditUsageEntity(
    id = id,
    creditId = creditId,
    amountDollars = amountDollars,
    usedAt = usedAt,
    description = description,
    transactionId = transactionId,
    source = source,
)
