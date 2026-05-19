package com.example.creditcardapp.data.mapper

import com.example.creditcardapp.data.local.StatementCreditEntity
import com.example.creditcardapp.data.local.StatementCreditUsageEntity
import com.example.creditcardapp.domain.model.CreditCategory
import com.example.creditcardapp.domain.model.CreditPeriod
import com.example.creditcardapp.domain.model.StatementCredit
import com.example.creditcardapp.domain.model.StatementCreditUsage

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
    createdAt = createdAt,
)

fun StatementCreditUsageEntity.toDomain(): StatementCreditUsage = StatementCreditUsage(
    id = id,
    creditId = creditId,
    amountDollars = amountDollars,
    usedAt = usedAt,
    description = description,
)

fun StatementCreditUsage.toEntity(): StatementCreditUsageEntity = StatementCreditUsageEntity(
    id = id,
    creditId = creditId,
    amountDollars = amountDollars,
    usedAt = usedAt,
    description = description,
)
