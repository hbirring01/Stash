package com.app.stash.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RotatingCategoryDao {

    @Query("SELECT * FROM rotating_categories ORDER BY startEpochMillis DESC")
    fun observeAll(): Flow<List<RotatingCategoryEntity>>

    @Query("SELECT * FROM rotating_categories WHERE cardId = :cardId ORDER BY startEpochMillis DESC")
    fun observeForCard(cardId: Long): Flow<List<RotatingCategoryEntity>>

    @Query(
        "SELECT * FROM rotating_categories " +
            "WHERE startEpochMillis <= :now AND endEpochMillis >= :now " +
            "ORDER BY multiplier DESC"
    )
    fun observeActive(now: Long): Flow<List<RotatingCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RotatingCategoryEntity): Long

    @Update
    suspend fun update(entity: RotatingCategoryEntity)

    @Delete
    suspend fun delete(entity: RotatingCategoryEntity)

    @Query("DELETE FROM rotating_categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
