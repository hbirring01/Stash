package com.app.stash.android.data.repository

import com.app.stash.android.data.local.StatementCreditDao
import com.app.stash.android.data.mapper.toDomain
import com.app.stash.android.data.mapper.toEntity
import com.app.stash.android.domain.model.StatementCredit
import com.app.stash.android.domain.model.StatementCreditUsage
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
    private val autoMatcher: StatementCreditAutoMatcher,
) {
    fun observeAll(): Flow<List<StatementCredit>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeForCard(cardId: Long): Flow<List<StatementCredit>> =
        dao.observeForCard(cardId).map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): StatementCredit? = dao.get(id)?.toDomain()

    suspend fun save(credit: StatementCredit): Long {
        val id = dao.insert(credit.toEntity())
        // Editing match rules should backfill against the existing tx history
        // so users see the progress jump as soon as they save, instead of
        // waiting for the next Plaid sync.
        autoMatcher.rescanForCredit(credit.copy(id = if (credit.id == 0L) id else credit.id))
        return id
    }

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

    /**
     * Removes a usage row. If it was auto-logged (source=AUTO or AI, has a
     * transactionId), we also record a dismissal so the auto-matcher won't
     * re-create it on the next Plaid sync. Returns the deleted usage so the
     * UI can offer an undo before fully committing.
     */
    suspend fun deleteUsage(id: Long): StatementCreditUsage? {
        val existing = dao.getUsage(id) ?: return null
        dao.deleteUsageById(id)
        if ((existing.source == "AUTO" || existing.source == "AI") &&
            existing.transactionId != null
        ) {
            autoMatcher.dismiss(existing.creditId, existing.transactionId)
        }
        return existing.toDomain()
    }

    /**
     * Re-inserts a previously-deleted usage (preserving its original id,
     * timestamp, and source). For AUTO/AI rows, also removes the dismissal
     * recorded during [deleteUsage] so the matcher's state is consistent.
     */
    suspend fun restoreUsage(usage: StatementCreditUsage) {
        dao.insertUsage(usage.toEntity())
        if ((usage.source == "AUTO" || usage.source == "AI") &&
            usage.transactionId != null
        ) {
            autoMatcher.undismiss(usage.creditId, usage.transactionId)
        }
    }

    /**
     * Manually re-run the auto-matcher against a single credit's card history.
     * Useful when the user wants to backfill the current period without
     * waiting for the next Plaid sync (e.g. after editing match rules or
     * after a transaction posts late).
     */
    suspend fun rescanCredit(creditId: Long) {
        val credit = dao.get(creditId)?.toDomain() ?: return
        autoMatcher.rescanForCredit(credit)
    }
}
