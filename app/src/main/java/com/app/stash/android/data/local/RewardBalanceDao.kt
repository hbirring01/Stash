package com.app.stash.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardBalanceDao {

    @Query("SELECT * FROM reward_balances ORDER BY programName ASC")
    fun observeAll(): Flow<List<RewardBalanceEntity>>

    @Query("SELECT * FROM reward_balances WHERE cardId = :cardId")
    fun observeForCard(cardId: Long): Flow<List<RewardBalanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(balance: RewardBalanceEntity): Long

    @Update
    suspend fun update(balance: RewardBalanceEntity)

    @Delete
    suspend fun delete(balance: RewardBalanceEntity)

    @Query("DELETE FROM reward_balances WHERE id = :id")
    suspend fun deleteById(id: Long)
}
