package com.app.stash.android.data.remote

import com.app.stash.android.data.remote.dto.CreditCardDto
import retrofit2.http.GET

interface CreditCardApi {
    @GET("cards")
    suspend fun getCards(): List<CreditCardDto>
}
