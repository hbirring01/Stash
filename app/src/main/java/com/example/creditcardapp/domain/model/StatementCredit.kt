package com.example.creditcardapp.domain.model

import java.time.LocalDate
import java.time.ZoneId

/**
 * A recurring statement credit attached to a card (e.g. Amex Platinum's
 * $200 hotel credit, $200 Uber Cash, $200 airline incidental; CSR's $300
 * travel credit; Amex Gold's $120 dining credit).
 *
 * [amountDollars] is the credit per period. Usage history is tracked
 * separately in [StatementCreditUsage] — call [periodWindow] to compute the
 * current period bounds for "used so far" calculations.
 */
data class StatementCredit(
    val id: Long = 0,
    val cardId: Long,
    val name: String,
    val amountDollars: Double,
    val periodKind: CreditPeriod,
    val periodStartMonth: Int = 1,
    val periodStartDay: Int = 1,
    val category: CreditCategory = CreditCategory.OTHER,
    val notes: String? = null,
    val source: String = "MANUAL",
    val createdAt: Long = System.currentTimeMillis(),
) {
    /**
     * Half-open [start, end) epoch-millis window of the period containing [now].
     * Implementations are deliberately calendar-based (not 365-day rolling)
     * because that's how issuers actually credit them — Amex Platinum's
     * airline credit resets on Jan 1 in your local time, not 365 days after
     * your last purchase.
     */
    fun periodWindow(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): LongRange {
        val localToday = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when (periodKind) {
            CreditPeriod.MONTHLY -> {
                val start = localToday.withDayOfMonth(1)
                val end = start.plusMonths(1)
                start.toMillis(zone)..(end.toMillis(zone) - 1)
            }
            CreditPeriod.QUARTERLY -> {
                // Quarters anchored on periodStartMonth: e.g. start=1 → Jan/Apr/Jul/Oct.
                val anchor = ((periodStartMonth - 1).coerceIn(0, 11))
                val monthIndex = localToday.monthValue - 1
                val offsetFromAnchor = ((monthIndex - anchor) % 3 + 3) % 3
                val quarterStartMonth = monthIndex - offsetFromAnchor + 1
                val start = localToday.withMonth(quarterStartMonth).withDayOfMonth(1)
                val end = start.plusMonths(3)
                start.toMillis(zone)..(end.toMillis(zone) - 1)
            }
            CreditPeriod.SEMI_ANNUAL -> {
                val anchor = ((periodStartMonth - 1).coerceIn(0, 11))
                val monthIndex = localToday.monthValue - 1
                val offsetFromAnchor = ((monthIndex - anchor) % 6 + 6) % 6
                val halfStartMonth = monthIndex - offsetFromAnchor + 1
                val start = localToday.withMonth(halfStartMonth).withDayOfMonth(1)
                val end = start.plusMonths(6)
                start.toMillis(zone)..(end.toMillis(zone) - 1)
            }
            CreditPeriod.ANNUAL -> {
                val startThisYear = localToday
                    .withMonth(periodStartMonth.coerceIn(1, 12))
                    .withDayOfMonth(periodStartDay.coerceIn(1, 28))
                val start = if (!localToday.isBefore(startThisYear)) startThisYear else startThisYear.minusYears(1)
                val end = start.plusYears(1)
                start.toMillis(zone)..(end.toMillis(zone) - 1)
            }
        }
    }

    /** Days remaining in the current period, floored. Negative if [now] is past the end. */
    fun daysRemaining(now: Long = System.currentTimeMillis()): Int {
        val end = periodWindow(now).last + 1 // exclusive
        return ((end - now) / MILLIS_PER_DAY).toInt()
    }

    companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}

private fun LocalDate.toMillis(zone: ZoneId): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

/** Recurrence period of a statement credit. */
enum class CreditPeriod { ANNUAL, SEMI_ANNUAL, QUARTERLY, MONTHLY }

/** Coarse category for grouping & filtering in the UI. */
enum class CreditCategory { TRAVEL, DINING, RIDESHARE, ENTERTAINMENT, SHOPPING, WELLNESS, OTHER }

/** One draw against a [StatementCredit]. */
data class StatementCreditUsage(
    val id: Long = 0,
    val creditId: Long,
    val amountDollars: Double,
    val usedAt: Long,
    val description: String? = null,
)
