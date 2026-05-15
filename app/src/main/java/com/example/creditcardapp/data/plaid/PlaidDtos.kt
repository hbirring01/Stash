package com.example.creditcardapp.data.plaid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkTokenCreateRequest(
    @SerialName("client_id") val clientId: String,
    val secret: String,
    @SerialName("client_name") val clientName: String,
    val language: String = "en",
    @SerialName("country_codes") val countryCodes: List<String> = listOf("US"),
    val user: PlaidUser,
    val products: List<String> = listOf("liabilities", "transactions"),
)

@Serializable
data class PlaidUser(@SerialName("client_user_id") val clientUserId: String)

@Serializable
data class LinkTokenCreateResponse(
    @SerialName("link_token") val linkToken: String,
    val expiration: String? = null,
)

@Serializable
data class PublicTokenExchangeRequest(
    @SerialName("client_id") val clientId: String,
    val secret: String,
    @SerialName("public_token") val publicToken: String,
)

@Serializable
data class PublicTokenExchangeResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("item_id") val itemId: String,
)

@Serializable
data class LiabilitiesGetRequest(
    @SerialName("client_id") val clientId: String,
    val secret: String,
    @SerialName("access_token") val accessToken: String,
)

@Serializable
data class LiabilitiesGetResponse(
    val accounts: List<PlaidAccount> = emptyList(),
    val liabilities: PlaidLiabilities = PlaidLiabilities(),
    val item: PlaidItem? = null,
)

@Serializable
data class PlaidItem(
    @SerialName("item_id") val itemId: String,
    @SerialName("institution_id") val institutionId: String? = null,
)

@Serializable
data class PlaidAccount(
    @SerialName("account_id") val accountId: String,
    val name: String,
    @SerialName("official_name") val officialName: String? = null,
    val mask: String? = null,
    val balances: PlaidBalances = PlaidBalances(),
    val subtype: String? = null,
    val type: String? = null,
)

@Serializable
data class PlaidBalances(
    val available: Double? = null,
    val current: Double? = null,
    val limit: Double? = null,
    @SerialName("iso_currency_code") val isoCurrencyCode: String? = null,
)

@Serializable
data class PlaidLiabilities(
    val credit: List<PlaidCreditLiability> = emptyList(),
)

@Serializable
data class PlaidCreditLiability(
    @SerialName("account_id") val accountId: String,
    @SerialName("last_payment_amount") val lastPaymentAmount: Double? = null,
    @SerialName("last_statement_balance") val lastStatementBalance: Double? = null,
    @SerialName("minimum_payment_amount") val minimumPaymentAmount: Double? = null,
    @SerialName("next_payment_due_date") val nextPaymentDueDate: String? = null,
    @SerialName("is_overdue") val isOverdue: Boolean? = null,
)

@Serializable
data class InstitutionGetRequest(
    @SerialName("client_id") val clientId: String,
    val secret: String,
    @SerialName("institution_id") val institutionId: String,
    @SerialName("country_codes") val countryCodes: List<String> = listOf("US"),
    val options: InstitutionGetOptions = InstitutionGetOptions(),
)

@Serializable
data class InstitutionGetOptions(
    @SerialName("include_optional_metadata") val includeOptionalMetadata: Boolean = true,
)

@Serializable
data class InstitutionGetResponse(
    val institution: PlaidInstitution? = null,
)

@Serializable
data class PlaidInstitution(
    @SerialName("institution_id") val institutionId: String,
    val name: String,
    val logo: String? = null,
    @SerialName("primary_color") val primaryColor: String? = null,
    val url: String? = null,
)

@Serializable
data class TransactionsSyncRequest(
    @SerialName("client_id") val clientId: String,
    val secret: String,
    @SerialName("access_token") val accessToken: String,
    val cursor: String? = null,
    val count: Int = 250,
)

@Serializable
data class TransactionsSyncResponse(
    val added: List<PlaidTransaction> = emptyList(),
    val modified: List<PlaidTransaction> = emptyList(),
    val removed: List<RemovedTransaction> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String = "",
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class RemovedTransaction(
    @SerialName("transaction_id") val transactionId: String,
)

@Serializable
data class PlaidTransaction(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("account_id") val accountId: String,
    val amount: Double,
    @SerialName("iso_currency_code") val isoCurrencyCode: String? = null,
    val date: String,
    @SerialName("authorized_date") val authorizedDate: String? = null,
    val name: String,
    @SerialName("merchant_name") val merchantName: String? = null,
    val pending: Boolean = false,
    @SerialName("personal_finance_category") val personalFinanceCategory: PfCategory? = null,
    val category: List<String> = emptyList(),
    @SerialName("payment_channel") val paymentChannel: String? = null,
)

@Serializable
data class PfCategory(
    val primary: String? = null,
    val detailed: String? = null,
)
