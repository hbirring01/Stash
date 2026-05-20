package com.app.stash.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StatementCreditDao {

    @Query("SELECT * FROM statement_credits ORDER BY cardId ASC, name ASC")
    fun observeAll(): Flow<List<StatementCreditEntity>>

    @Query("SELECT * FROM statement_credits WHERE cardId = :cardId ORDER BY name ASC")
    fun observeForCard(cardId: Long): Flow<List<StatementCreditEntity>>

    @Query("SELECT * FROM statement_credits WHERE id = :id")
    suspend fun get(id: Long): StatementCreditEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StatementCreditEntity): Long

    @Update
    suspend fun update(entity: StatementCreditEntity)

    @Delete
    suspend fun delete(entity: StatementCreditEntity)

    @Query("DELETE FROM statement_credits WHERE id = :id")
    suspend fun deleteById(id: Long)

    // --- Usages ---

    @Query("SELECT * FROM statement_credit_usages WHERE creditId = :creditId ORDER BY usedAt DESC")
    fun observeUsagesForCredit(creditId: Long): Flow<List<StatementCreditUsageEntity>>

    /**
     * Usages for [creditId] whose [StatementCreditUsageEntity.usedAt] falls
     * in the half-open window [[periodStart], [periodEnd]).
     */
    @Query(
        "SELECT * FROM statement_credit_usages " +
            "WHERE creditId = :creditId AND usedAt >= :periodStart AND usedAt < :periodEnd " +
            "ORDER BY usedAt DESC"
    )
    fun observeUsagesInPeriod(creditId: Long, periodStart: Long, periodEnd: Long): Flow<List<StatementCreditUsageEntity>>

    @Query(
        "SELECT IFNULL(SUM(amountDollars), 0) FROM statement_credit_usages " +
            "WHERE creditId = :creditId AND usedAt >= :periodStart AND usedAt < :periodEnd"
    )
    fun observeUsedInPeriod(creditId: Long, periodStart: Long, periodEnd: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(entity: StatementCreditUsageEntity): Long

    /**
     * Auto-tracker insert: IGNORE on conflict (the unique index on
     * (creditId, transactionId) blocks duplicates). Returns the new row id,
     * or -1 if the row was a duplicate and skipped.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsageIfNew(entity: StatementCreditUsageEntity): Long

    @Query("SELECT * FROM statement_credit_usages WHERE id = :id")
    suspend fun getUsage(id: Long): StatementCreditUsageEntity?

    @Query("DELETE FROM statement_credit_usages WHERE id = :id")
    suspend fun deleteUsageById(id: Long)

    @Query("DELETE FROM statement_credit_usages WHERE creditId = :creditId")
    suspend fun deleteUsagesForCredit(creditId: Long)

    // --- Dismissed matches ---

    @Query("SELECT transactionId FROM dismissed_credit_matches WHERE creditId = :creditId")
    suspend fun dismissedTransactionIds(creditId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDismissed(entity: DismissedCreditMatchEntity)
}
