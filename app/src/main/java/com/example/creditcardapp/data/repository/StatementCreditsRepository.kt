package com.example.creditcardapp.data.repository

import com.example.creditcardapp.data.local.StatementCreditDao
import com.example.creditcardapp.data.mapper.toDomain
import com.example.creditcardapp.data.mapper.toEntity
import com.example.creditcardapp.domain.model.StatementCredit
import com.example.creditcardapp.domain.model.StatementCreditUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence + lookup for per-card statement credits and their usage history.
 * Credits are user-defined (no remote/curated source — issuer perks change too
 * often). Usage entries are short, immutable rows; deleting the parent credit
 * cascades manually via [deleteCredit].
 */
@Singleton
class StatementCreditsRepository @Inject constructor(
    private val dao: StatementCreditDao,
) {
    fun observeAll(): Flow<List<StatementCredit>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeForCard(cardId: Long): Flow<List<StatementCredit>> =
        dao.observeForCard(cardId).map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): StatementCredit? = dao.get(id)?.toDomain()

    suspend fun save(credit: StatementCredit): Long = dao.insert(credit.toEntity())

    suspend fun deleteCredit(id: Long) {
        dao.deleteUsagesForCredit(id)
        dao.deleteById(id)
    }

    // --- Usages ---

    fun observeUsages(creditId: Long): Flow<List<StatementCreditUsage>> =
        dao.observeUsagesForCredit(creditId).map { list -> list.map { it.toDomain() } }

    fun observeUsedInPeriod(creditId: Long, periodStart: Long, periodEnd: Long): Flow<Double> =
        dao.observeUsedInPeriod(creditId, periodStart, periodEnd)

    fun observeUsagesInPeriod(
        creditId: Long,
        periodStart: Long,
        periodEnd: Long,
    ): Flow<List<StatementCreditUsage>> =
        dao.observeUsagesInPeriod(creditId, periodStart, periodEnd)
            .map { list -> list.map { it.toDomain() } }

    suspend fun logUsage(usage: StatementCreditUsage): Long = dao.insertUsage(usage.toEntity())

    suspend fun deleteUsage(id: Long) = dao.deleteUsageById(id)
}
