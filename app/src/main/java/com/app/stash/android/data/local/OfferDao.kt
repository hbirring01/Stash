package com.app.stash.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {

    @Query("SELECT * FROM offers ORDER BY expiresAt ASC")
    fun observeAll(): Flow<List<OfferEntity>>

    /** Active (not expired) offers, oldest-expiring first. */
    @Query("SELECT * FROM offers WHERE expiresAt >= :now ORDER BY expiresAt ASC")
    fun observeActive(now: Long): Flow<List<OfferEntity>>

    /** Active and not yet activated. Used for the "needs your attention" badge / notifications. */
    @Query("SELECT * FROM offers WHERE expiresAt >= :now AND activatedAt IS NULL ORDER BY expiresAt ASC")
    fun observeUnactivated(now: Long): Flow<List<OfferEntity>>

    @Query("SELECT * FROM offers WHERE id = :id")
    suspend fun get(id: Long): OfferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OfferEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<OfferEntity>): List<Long>

    @Update
    suspend fun update(entity: OfferEntity)

    @Query("UPDATE offers SET activatedAt = :timestamp WHERE id = :id")
    suspend fun setActivated(id: Long, timestamp: Long?)

    @Delete
    suspend fun delete(entity: OfferEntity)

    @Query("DELETE FROM offers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM offers WHERE expiresAt < :cutoff")
    suspend fun pruneExpired(cutoff: Long)
}
