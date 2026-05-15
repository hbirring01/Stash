package com.example.creditcardapp.data.remote

import com.example.creditcardapp.data.remote.dto.CreditCardDto
import retrofit2.http.GET

interface CreditCardApi {
    @GET("cards")
    suspend fun getCards(): List<CreditCardDto>
}
