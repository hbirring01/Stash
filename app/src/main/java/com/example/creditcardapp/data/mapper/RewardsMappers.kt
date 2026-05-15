package com.example.creditcardapp.data.mapper

import com.example.creditcardapp.data.local.RewardBalanceEntity
import com.example.creditcardapp.data.local.RotatingCategoryEntity
import com.example.creditcardapp.domain.model.RewardBalance
import com.example.creditcardapp.domain.model.RewardCategory
import com.example.creditcardapp.domain.model.RotatingCategory

fun RewardBalanceEntity.toDomain(): RewardBalance = RewardBalance(
    id = id,
    cardId = cardId,
    programName = programName,
    points = points,
    valuePerPointCents = valuePerPointCents,
    updatedAt = updatedAt,
)

fun RewardBalance.toEntity(): RewardBalanceEntity = RewardBalanceEntity(
    id = id,
    cardId = cardId,
    programName = programName,
    points = points,
    valuePerPointCents = valuePerPointCents,
    updatedAt = updatedAt,
)

fun RotatingCategoryEntity.toDomain(): RotatingCategory = RotatingCategory(
    id = id,
    cardId = cardId,
    category = runCatching { RewardCategory.valueOf(category) }.getOrDefault(RewardCategory.OTHER),
    multiplier = multiplier,
    startEpochMillis = startEpochMillis,
    endEpochMillis = endEpochMillis,
    label = label,
)

fun RotatingCategory.toEntity(): RotatingCategoryEntity = RotatingCategoryEntity(
    id = id,
    cardId = cardId,
    category = category.name,
    multiplier = multiplier,
    startEpochMillis = startEpochMillis,
    endEpochMillis = endEpochMillis,
    label = label,
)
