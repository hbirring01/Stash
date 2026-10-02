package com.app.stash.android.data.repository

import com.app.stash.android.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow

interface CreditCardRepository {
    fun observeCards(): Flow<List<CreditCard>>
    suspend fun getCard(id: Long): CreditCard?
    suspend fun saveCard(card: CreditCard): Long
    suspend fun deleteCard(id: Long)
}
