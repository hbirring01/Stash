package com.example.creditcardapp.data.repository

import com.example.creditcardapp.data.local.CreditCardDao
import com.example.creditcardapp.data.mapper.toDomain
import com.example.creditcardapp.data.mapper.toEntity
import com.example.creditcardapp.data.remote.CreditCardApi
import com.example.creditcardapp.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditCardRepositoryImpl @Inject constructor(
    private val dao: CreditCardDao,
    private val api: CreditCardApi
) : CreditCardRepository {

    override fun observeCards(): Flow<List<CreditCard>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getCard(id: Long): CreditCard? =
        dao.getById(id)?.toDomain()

    override suspend fun saveCard(card: CreditCard): Long =
        dao.insert(card.toEntity())

    override suspend fun deleteCard(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun refreshFromRemote(): Result<Unit> = runCatching {
        val remote = api.getCards()
        dao.insertAll(remote.map { it.toEntity() })
    }
}
