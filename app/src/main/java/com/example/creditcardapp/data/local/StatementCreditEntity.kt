package com.example.creditcardapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recurring statement credit attached to a card (e.g. Amex Platinum's
 * $200 hotel credit, $200 Uber Cash, $200 airline incidental; Chase
 * Sapphire Reserve's $300 travel credit; Amex Gold's $120 dining credit).
 *
 * The user logs each draw against the credit in [StatementCreditUsageEntity].
 * StashApp computes "used so far" by summing usages whose `usedAt` falls in
 * the current period (defined by [periodKind] + [periodStartMonth]/[periodStartDay]).
 *
 * The credit itself is just the recurring definition — usage history is
 * stored separately so we can show a per-period progress without losing
 * historical data when the credit rolls over.
 */
@Entity(
    tableName = "statement_credits",
    indices = [Index("cardId")],
)
data class StatementCreditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** FK to [CreditCardEntity.id]. */
    val cardId: Long,
    /** Display name, e.g. "Hotel Credit", "Uber Cash", "Dining Credit". */
    val name: String,
    /** Total credit amount per period, in dollars. */
    val amountDollars: Double,
    /** ANNUAL | SEMI_ANNUAL | QUARTERLY | MONTHLY. */
    val periodKind: String,
    /**
     * Month (1-12) the period starts on for ANNUAL / SEMI_ANNUAL / QUARTERLY.
     * For MONTHLY this is ignored. For ANNUAL on Amex Platinum this is
     * usually January (calendar year).
     */
    val periodStartMonth: Int = 1,
    /** Day of month the period starts on. Default 1. */
    val periodStartDay: Int = 1,
    /** Optional category tag for grouping (TRAVEL, DINING, RIDESHARE, ENTERTAINMENT, OTHER). */
    val category: String = "OTHER",
    /** Free-form notes (eligible merchants, fine print). */
    val notes: String? = null,
    /** Provenance: CURATED / MANUAL. */
    val source: String = "MANUAL",
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * One draw against a [StatementCreditEntity]. Multiple usages can exist per
 * period — e.g. $4.99 Uber Eats today + $7.50 Uber ride tomorrow.
 */
@Entity(
    tableName = "statement_credit_usages",
    indices = [Index("creditId"), Index("usedAt")],
)
data class StatementCreditUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** FK to [StatementCreditEntity.id]. */
    val creditId: Long,
    /** Dollar amount of this draw. */
    val amountDollars: Double,
    /** Epoch millis when the credit was used. */
    val usedAt: Long,
    /** Free-form description (merchant, transaction id). */
    val description: String? = null,
)
