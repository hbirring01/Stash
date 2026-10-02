package com.app.stash.android.data.repository

import com.app.stash.android.data.local.CreditCardDao
import com.app.stash.android.data.mapper.toDomain
import com.app.stash.android.data.mapper.toEntity
import com.app.stash.android.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditCardRepositoryImpl @Inject constructor(
    private val dao: CreditCardDao,
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
}
