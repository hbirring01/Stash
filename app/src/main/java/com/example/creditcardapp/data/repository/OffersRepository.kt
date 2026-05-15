package com.example.creditcardapp.data.repository

import com.example.creditcardapp.data.local.OfferDao
import com.example.creditcardapp.data.mapper.toDomain
import com.example.creditcardapp.data.mapper.toEntity
import com.example.creditcardapp.domain.model.Offer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence + lookup for card-linked offers. Curated seed data is loaded in
 * [com.example.creditcardapp.di.DatabaseModule]; users can also add/edit/delete
 * offers manually via the Offers screen.
 */
@Singleton
class OffersRepository @Inject constructor(
    private val dao: OfferDao,
) {
    fun observeAll(): Flow<List<Offer>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(now: Long = System.currentTimeMillis()): Flow<List<Offer>> =
        dao.observeActive(now).map { list -> list.map { it.toDomain() } }

    fun observeUnactivated(now: Long = System.currentTimeMillis()): Flow<List<Offer>> =
        dao.observeUnactivated(now).map { list -> list.map { it.toDomain() } }

    suspend fun save(offer: Offer): Long = dao.insert(offer.toEntity())

    suspend fun setActivated(id: Long, activated: Boolean) {
        dao.setActivated(id, if (activated) System.currentTimeMillis() else null)
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun pruneExpired(cutoff: Long = System.currentTimeMillis()) = dao.pruneExpired(cutoff)

    /** Find the first active, unactivated offer matching [merchantName] (case-insensitive). */
    fun findActiveOfferForMerchant(offers: List<Offer>, merchantName: String): Offer? =
        offers.firstOrNull { it.isActive() && !it.isActivated && it.matchesMerchant(merchantName) }
}
