package com.app.stash.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreditCardDto(
    @SerialName("id") val id: Long,
    @SerialName("cardholder_name") val cardholderName: String,
    @SerialName("last4") val last4: String,
    @SerialName("brand") val brand: String,
    @SerialName("expiry_month") val expiryMonth: Int,
    @SerialName("expiry_year") val expiryYear: Int,
    @SerialName("balance") val balance: Double,
    @SerialName("credit_limit") val creditLimit: Double,
    @SerialName("nickname") val nickname: String? = null
)
