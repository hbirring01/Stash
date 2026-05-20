package com.app.stash.android.data.mapper

import com.app.stash.android.data.local.RewardBalanceEntity
import com.app.stash.android.data.local.RotatingCategoryEntity
import com.app.stash.android.domain.model.RewardBalance
import com.app.stash.android.domain.model.RewardCategory
import com.app.stash.android.domain.model.RotatingCategory

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
