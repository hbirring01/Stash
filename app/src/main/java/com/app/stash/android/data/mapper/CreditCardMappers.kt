package com.app.stash.android.data.mapper

import com.app.stash.android.data.local.CreditCardEntity
import com.app.stash.android.domain.model.CreditCard
import com.app.stash.android.domain.model.RewardCategory
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

private val rewardsJson = Json { ignoreUnknownKeys = true }
private val rewardsSerializer = MapSerializer(String.serializer(), Double.serializer())

private fun parseRewards(raw: String?): Map<RewardCategory, Double> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
        rewardsJson.decodeFromString(rewardsSerializer, raw).mapNotNull { (k, v) ->
            runCatching { RewardCategory.valueOf(k) }.getOrNull()?.let { it to v }
        }.toMap()
    }.getOrDefault(emptyMap())
}

private fun encodeRewards(map: Map<RewardCategory, Double>): String? =
    if (map.isEmpty()) null
    else rewardsJson.encodeToString(rewardsSerializer, map.mapKeys { it.key.name })

fun CreditCardEntity.toDomain(): CreditCard = CreditCard(
    id = id,
    cardholderName = cardholderName,
    last4 = last4,
    brand = brand,
    expiryMonth = expiryMonth,
    expiryYear = expiryYear,
    balance = balance,
    creditLimit = creditLimit,
    nickname = nickname,
    sourceItemId = sourceItemId,
    sourceAccountId = sourceAccountId,
    rewards = parseRewards(rewardsJson),
    annualFee = annualFee,
    pointValueCents = pointValueCents,
    signupBonusRequiredSpend = signupBonusRequiredSpend,
    signupBonusEarnedSpend = signupBonusEarnedSpend,
    signupBonusValue = signupBonusValue,
    signupBonusDeadline = signupBonusDeadline,
)

fun CreditCard.toEntity(): CreditCardEntity = CreditCardEntity(
    id = id,
    cardholderName = cardholderName,
    last4 = last4,
    brand = brand,
    expiryMonth = expiryMonth,
    expiryYear = expiryYear,
    balance = balance,
    creditLimit = creditLimit,
    nickname = nickname,
    sourceItemId = sourceItemId,
    sourceAccountId = sourceAccountId,
    rewardsJson = encodeRewards(rewards),
    annualFee = annualFee,
    pointValueCents = pointValueCents,
    signupBonusRequiredSpend = signupBonusRequiredSpend,
    signupBonusEarnedSpend = signupBonusEarnedSpend,
    signupBonusValue = signupBonusValue,
    signupBonusDeadline = signupBonusDeadline,
)


