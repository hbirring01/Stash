package com.example.creditcardapp.data.mapper

import com.example.creditcardapp.data.local.OfferEntity
import com.example.creditcardapp.domain.model.Offer
import com.example.creditcardapp.domain.model.RewardKind

fun OfferEntity.toDomain(): Offer = Offer(
    id = id,
    merchantPattern = merchantPattern,
    merchantDisplay = merchantDisplay,
    issuer = issuer,
    cardLast4 = cardLast4,
    rewardKind = runCatching { RewardKind.valueOf(rewardKind) }.getOrDefault(RewardKind.PERCENT),
    rewardValue = rewardValue,
    capDollars = capDollars,
    minSpendDollars = minSpendDollars,
    expiresAt = expiresAt,
    activatedAt = activatedAt,
    source = source,
    deepLinkUri = deepLinkUri,
    description = description,
)

fun Offer.toEntity(): OfferEntity = OfferEntity(
    id = id,
    merchantPattern = merchantPattern,
    merchantDisplay = merchantDisplay,
    issuer = issuer,
    cardLast4 = cardLast4,
    rewardKind = rewardKind.name,
    rewardValue = rewardValue,
    capDollars = capDollars,
    minSpendDollars = minSpendDollars,
    expiresAt = expiresAt,
    activatedAt = activatedAt,
    source = source,
    deepLinkUri = deepLinkUri,
    description = description,
)
