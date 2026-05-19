package com.example.creditcardapp.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Cached LLM verdict on whether a normalized merchant string matches a given
 * statement credit. Keyed by `(creditId, merchantNorm)` so we never ask the
 * model the same question twice — Plaid resyncs and re-runs are free.
 *
 * Cache is invalidated when a credit's [matchPattern]/[matchCategory] changes
 * (the repository's `save` path clears it). We intentionally do NOT cache by
 * full transaction id — many transactions share a merchant (e.g. weekly Uber
 * rides) and we want all of them to inherit the model's verdict.
 */
@Entity(
    tableName = "ai_match_cache",
    primaryKeys = ["creditId", "merchantNorm"],
)
data class AiMatchCacheEntity(
    val creditId: Long,
    /** Lowercased, alphanumeric-only merchant key — see normalizeMerchant(). */
    val merchantNorm: String,
    val matched: Boolean,
    /** For future invalidation if we change prompt or model. */
    val modelVersion: String,
    val createdAt: Long,
)

@Dao
interface AiMatchCacheDao {
    @Query(
        "SELECT matched FROM ai_match_cache " +
            "WHERE creditId = :creditId AND merchantNorm = :merchantNorm LIMIT 1"
    )
    suspend fun lookup(creditId: Long, merchantNorm: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: AiMatchCacheEntity)

    @Query("DELETE FROM ai_match_cache WHERE creditId = :creditId")
    suspend fun clearForCredit(creditId: Long)

    @Query("DELETE FROM ai_match_cache")
    suspend fun clearAll()
}

/**
 * Normalize a Plaid merchant or descriptor for cache keying:
 *  - lowercase
 *  - strip non-alphanumerics (collapses "TST* MARRIOTT BONVOY NYC" and
 *    "marriott bonvoy nyc" to the same key)
 *  - clamp to 64 chars to bound key size
 */
fun normalizeMerchant(raw: String): String =
    raw.lowercase()
        .filter { it.isLetterOrDigit() }
        .take(64)
