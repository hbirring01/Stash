package com.example.creditcardapp.data.repository

import com.example.creditcardapp.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow

interface CreditCardRepository {
    fun observeCards(): Flow<List<CreditCard>>
    suspend fun getCard(id: Long): CreditCard?
    suspend fun saveCard(card: CreditCard): Long
    suspend fun deleteCard(id: Long)
    suspend fun refreshFromRemote(): Result<Unit>
}
