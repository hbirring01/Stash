package com.example.creditcardapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, transactionId DESC")
    fun observeByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE transactionId IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM transactions WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)

    @Query("DELETE FROM transactions")
    suspend fun clear()
}
