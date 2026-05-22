package com.app.stash.android.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Cached LLM verdict on the high-level [com.app.stash.android.domain.model.RewardCategory]
 * for a normalized merchant string. Keyed by `merchantNorm` so every transaction
 * from "Starbucks" inherits the same answer after a single AI call.
 *
 * Distinct from [AiMatchCacheEntity] which is keyed by `(creditId, merchantNorm)`
 * for statement-credit matching. This cache is global (merchant → category).
 */
@Entity(tableName = "merchant_category_cache")
data class MerchantCategoryCacheEntity(
    /** See [normalizeMerchant]. */
    @PrimaryKey val merchantNorm: String,
    /** [com.app.stash.android.domain.model.RewardCategory.name]. */
    val category: String,
    /** Whether this row was produced by the LLM (true) or local rules (false). */
    val fromAi: Boolean,
    /** Model name when [fromAi] = true; "rules" otherwise. */
    val modelVersion: String,
    val createdAt: Long,
)

@Dao
interface MerchantCategoryCacheDao {
    @Query("SELECT category FROM merchant_category_cache WHERE merchantNorm = :key LIMIT 1")
    suspend fun lookup(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: MerchantCategoryCacheEntity)

    @Query("DELETE FROM merchant_category_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM merchant_category_cache")
    fun observeCount(): kotlinx.coroutines.flow.Flow<Int>
}
