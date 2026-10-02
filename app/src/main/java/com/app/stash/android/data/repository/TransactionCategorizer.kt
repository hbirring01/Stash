package com.app.stash.android.data.repository

import com.app.stash.android.data.ai.AiBudget
import com.app.stash.android.data.ai.AiCategoryClient
import com.app.stash.android.data.ai.MerchantCategoryRules
import com.app.stash.android.data.local.TransactionDao
import com.app.stash.android.data.local.TransactionEntity
import com.app.stash.android.domain.model.RewardCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a high-level [RewardCategory] for each transaction using a 4-tier
 * pipeline (cheapest tier wins):
 *
 *   1. Plaid `personal_finance_category.primary` (free, already on the row) —
 *      used when [RewardCategory.fromPlaidPrimary] yields anything other than
 *      [RewardCategory.OTHER]. Plaid's PFC v2 is fairly accurate for the big
 *      buckets (FOOD_AND_DRINK, TRAVEL, etc.).
 *
 *   2. Local merchant-name rules ([MerchantCategoryRules]) — catches the long
 *      tail of well-known brands Plaid mis-tags (e.g. "DOORDASH" → DINING
 *      instead of GENERAL_MERCHANDISE).
 *
 *   3. AI cache lookup (per normalized merchant) — repeated transactions from
 *      the same merchant inherit the prior verdict for free.
 *
 *   4. AI call to an OpenAI-compatible LLM (Gemini Flash by default). Budgeted
 *      per batch so a backfill of years of history can't drain a free quota
 *      in one go.
 *
 * The resolved category is persisted to [TransactionEntity.aiCategory] so the
 * UI can render a tag without re-running the pipeline on every recomposition.
 */
@Singleton
class TransactionCategorizer @Inject constructor(
    private val transactionDao: TransactionDao,
    private val aiClient: AiCategoryClient,
) {
    /** Soft cap on LLM calls per sync. Cache hits don't count. */
    private val aiBudgetPerBatch = 25

    /**
     * Resolve and persist a category for every transaction in [transactions]
     * that doesn't already have a non-null [TransactionEntity.aiCategory].
     */
    suspend fun categorize(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) return

        val budget = AiBudget(aiBudgetPerBatch)
        val updates = mutableListOf<TransactionEntity>()

        for (tx in transactions) {
            if (tx.aiCategory != null) continue
            val resolved = resolveOne(tx, budget) ?: continue
            updates += tx.copy(aiCategory = resolved.name)
        }

        if (updates.isNotEmpty()) {
            transactionDao.upsertAll(updates)
        }
    }

    private suspend fun resolveOne(tx: TransactionEntity, budget: AiBudget): RewardCategory? {
        val merchant = tx.merchantName?.takeIf { it.isNotBlank() } ?: tx.name

        // Tier 1: Plaid PFC primary. Use it when it produces a concrete bucket.
        val plaidGuess = RewardCategory.fromPlaidPrimary(tx.categoryPrimary)
        if (plaidGuess != RewardCategory.OTHER) return plaidGuess

        // Tier 2: local rules. When a rule hits, persist it to the cache so
        // future syncs short-circuit at tier 3 without re-scanning the rule
        // table (cheap, but avoids reloading rules on every transaction).
        MerchantCategoryRules.classify(merchant)?.let { ruleCategory ->
            aiClient.cacheRuleVerdict(merchant, ruleCategory)
            return ruleCategory
        }

        // Tiers 3 & 4: cache + LLM, gated by budget. Returns null when AI is
        // disabled / over budget / network error — caller leaves aiCategory null
        // so we'll retry on a future sync rather than poisoning the row with OTHER.
        return aiClient.categorize(merchant, tx.categoryPrimary, budget)
    }
}
