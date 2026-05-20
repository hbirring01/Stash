package com.app.stash.android.data.repository

import com.app.stash.android.data.ai.AiBudget
import com.app.stash.android.data.ai.AiMatchClient
import com.app.stash.android.data.local.CreditCardDao
import com.app.stash.android.data.local.DismissedCreditMatchEntity
import com.app.stash.android.data.local.StatementCreditDao
import com.app.stash.android.data.local.StatementCreditUsageEntity
import com.app.stash.android.data.local.TransactionDao
import com.app.stash.android.data.local.TransactionEntity
import com.app.stash.android.data.mapper.toDomain
import com.app.stash.android.domain.model.StatementCredit
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Auto-tracks statement-credit usage by scanning Plaid transactions and
 * logging matching purchases against their associated [StatementCredit].
 *
 * Idempotency: the `(creditId, transactionId)` unique index on
 * `statement_credit_usages` blocks duplicate inserts on resync. Once a user
 * manually deletes an auto-logged usage, that (creditId, transactionId) pair
 * is recorded in `dismissed_credit_matches` so we never re-add it.
 *
 * Spend cap: usage stops growing past the credit's period amount — we don't
 * silently log a $250 hotel charge against a $200 credit as $250.
 */
@Singleton
class StatementCreditAutoMatcher @Inject constructor(
    private val cardDao: CreditCardDao,
    private val creditDao: StatementCreditDao,
    private val transactionDao: TransactionDao,
    private val aiMatchClient: AiMatchClient,
) {
    /** Soft cap on LLM calls per [matchTransactions] invocation so a Plaid
     *  resync of a year of history can't burn 1000 API calls in one go. */
    private val aiBudgetPerBatch = 25
    /**
     * Scan [transactions] (typically the freshly upserted batch from Plaid)
     * and auto-log any matching credit usage.
     */
    suspend fun matchTransactions(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) return

        // Resolve sourceAccountId -> cardId once per batch.
        val accountIds = transactions.map { it.accountId }.distinct()
        val accountIdToCardId = accountIds.mapNotNull { aid ->
            cardDao.getBySourceAccountId(aid)?.let { aid to it.id }
        }.toMap()
        if (accountIdToCardId.isEmpty()) return

        val zone = ZoneId.systemDefault()
        val budget = AiBudget(aiBudgetPerBatch)

        // Group by card and process credits per card.
        val byCard = transactions
            .filter { !it.pending } // posted only, so amounts are final
            .groupBy { accountIdToCardId[it.accountId] }
            .filterKeys { it != null }

        for ((cardId, txs) in byCard) {
            cardId ?: continue
            val credits = creditDao.observeForCard(cardId).firstSync()
                .map { it.toDomain() }
                .filter { it.autoTrack && (it.matchPattern != null || it.matchCategory != null) }
            if (credits.isEmpty()) continue

            for (credit in credits) {
                processCredit(credit, txs, zone, budget)
            }
        }
    }

    private suspend fun processCredit(
        credit: StatementCredit,
        candidateTxs: List<TransactionEntity>,
        zone: ZoneId,
        budget: AiBudget = AiBudget(0),
    ) {
        val window = credit.periodWindow()
        val dismissed = creditDao.dismissedTransactionIds(credit.id).toHashSet()

        // Already-used in this period — used to cap the credit so we don't
        // log a $250 hotel charge as $250 against a $200 credit.
        var used = creditDao
            .observeUsedInPeriod(credit.id, window.first, window.last + 1)
            .firstSync()
        if (used >= credit.amountDollars) return

        for (tx in candidateTxs) {
            if (tx.transactionId in dismissed) continue
            val txMillis = tx.date.toEpochMillisOrNull(zone) ?: continue
            if (txMillis !in window) continue

            val displayName = tx.merchantName ?: tx.name

            // Tier 1: deterministic rule (pattern OR category) — free, instant.
            // Tier 2: LLM fallback — only when literal rules miss AND user has
            // AI Assist enabled AND we're under the per-batch budget. Cache hits
            // don't consume the budget.
            val literalMatch = credit.matchesTransaction(displayName, tx.categoryPrimary)
            if (!literalMatch) {
                val aiVerdict = aiMatchClient.matchesCredit(
                    credit, displayName, tx.categoryPrimary, budget
                )
                if (aiVerdict != true) continue
            }

            // Plaid posts credit-card spend as positive amounts; ignore
            // refunds (negative) which would otherwise eat into the credit.
            val txAmount = tx.amount
            if (txAmount <= 0.0) continue

            val remaining = credit.amountDollars - used
            if (remaining <= 0.0) break
            val logged = minOf(txAmount, remaining)

            val row = StatementCreditUsageEntity(
                creditId = credit.id,
                amountDollars = logged,
                usedAt = txMillis,
                description = displayName,
                transactionId = tx.transactionId,
                source = if (literalMatch) "AUTO" else "AI",
            )
            val newId = creditDao.insertUsageIfNew(row)
            if (newId != -1L) {
                used += logged
                if (used >= credit.amountDollars) break
            }
        }
    }

    /**
     * Called by the repository when the user deletes an auto-logged usage.
     * Records the (creditId, transactionId) pair so the matcher doesn't
     * re-add it on the next sync.
     */
    suspend fun dismiss(creditId: Long, transactionId: String) {
        creditDao.insertDismissed(
            DismissedCreditMatchEntity(creditId = creditId, transactionId = transactionId)
        )
    }

    /**
     * Reverses a prior [dismiss], allowing the matcher to re-log the usage on
     * the next sync. Used when the user undoes a usage deletion before the
     * snackbar window expires.
     */
    suspend fun undismiss(creditId: Long, transactionId: String) {
        creditDao.deleteDismissed(creditId, transactionId)
    }

    /**
     * Backfill: rescan all existing transactions on the card linked to
     * [credit] against just that credit. Useful right after the user adds
     * or edits a credit's match rules so the progress bar reflects the
     * current period immediately, without waiting for the next Plaid sync.
     */
    suspend fun rescanForCredit(credit: StatementCredit) {
        if (!credit.autoTrack) return
        if (credit.matchPattern == null && credit.matchCategory == null) return

        val card = cardDao.getById(credit.cardId) ?: return
        val accountId = card.sourceAccountId ?: return // manual cards have no tx history

        val window = credit.periodWindow()
        // Plaid date strings are YYYY-MM-DD; we filter by string lexically
        // by passing the start-of-window date.
        val zone = ZoneId.systemDefault()
        val startDate = java.time.Instant.ofEpochMilli(window.first)
            .atZone(zone)
            .toLocalDate()
            .toString()

        val txs = transactionDao.getByAccountSince(accountId, startDate)
        // Backfill is user-initiated (Save in editor) so we allow a small AI
        // budget; rule changes also wipe the AI cache for this credit so any
        // previously-cached verdicts using stale rules are re-asked.
        aiMatchClient.invalidate(credit.id)
        processCredit(credit, txs, zone, AiBudget(aiBudgetPerBatch))
    }
}

/**
 * Best-effort parse of a Plaid transaction date (`YYYY-MM-DD`) into epoch
 * millis at the local zone's start-of-day. Returns null on malformed input
 * instead of throwing — we'd rather skip one tx than abort the whole batch.
 */
private fun String.toEpochMillisOrNull(zone: ZoneId): Long? = runCatching {
    LocalDate.parse(this).atStartOfDay(zone).toInstant().toEpochMilli()
}.getOrNull()

private suspend fun <T> Flow<T>.firstSync(): T = first()
