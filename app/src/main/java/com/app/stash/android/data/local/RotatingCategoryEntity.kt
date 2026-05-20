package com.app.stash.android.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A time-bounded bonus category for a card (e.g. Freedom Flex 5% on gas
 * Apr–Jun). Stored separately from the card's permanent [rewardsJson] so the
 * Rewards Calendar can surface "what's hot this quarter" without polluting the
 * card's baseline multipliers.
 */
@Entity(tableName = "rotating_categories", indices = [Index("cardId")])
data class RotatingCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    /** Enum name of [com.app.stash.android.domain.model.RewardCategory]. */
    val category: String,
    val multiplier: Double,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    /** Optional friendly description shown in the Calendar (e.g. "Q2 2026 — Gas & Streaming"). */
    val label: String? = null,
)
