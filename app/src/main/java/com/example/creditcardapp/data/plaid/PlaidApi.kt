package com.example.creditcardapp.data.plaid

import retrofit2.http.Body
import retrofit2.http.POST

interface PlaidApi {

    @POST("link/token/create")
    suspend fun createLinkToken(@Body body: LinkTokenCreateRequest): LinkTokenCreateResponse

    @POST("item/public_token/exchange")
    suspend fun exchangePublicToken(@Body body: PublicTokenExchangeRequest): PublicTokenExchangeResponse

    @POST("liabilities/get")
    suspend fun getLiabilities(@Body body: LiabilitiesGetRequest): LiabilitiesGetResponse

    @POST("institutions/get_by_id")
    suspend fun getInstitutionById(@Body body: InstitutionGetRequest): InstitutionGetResponse

    @POST("transactions/sync")
    suspend fun syncTransactions(@Body body: TransactionsSyncRequest): TransactionsSyncResponse
}
