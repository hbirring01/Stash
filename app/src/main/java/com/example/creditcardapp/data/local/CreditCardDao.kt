package com.example.creditcardapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    @Query("SELECT * FROM credit_cards ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getById(id: Long): CreditCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CreditCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CreditCardEntity>)

    @Update
    suspend fun update(card: CreditCardEntity)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM credit_cards")
    suspend fun clear()

    @Query("SELECT * FROM credit_cards WHERE sourceAccountId = :accountId LIMIT 1")
    suspend fun getBySourceAccountId(accountId: String): CreditCardEntity?

    @Query("DELETE FROM credit_cards WHERE sourceItemId = :itemId")
    suspend fun deleteBySourceItemId(itemId: String)
}
