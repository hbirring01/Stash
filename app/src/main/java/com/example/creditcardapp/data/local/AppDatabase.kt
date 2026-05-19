package com.example.creditcardapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CreditCardEntity::class,
        TransactionEntity::class,
        RewardBalanceEntity::class,
        RotatingCategoryEntity::class,
        OfferEntity::class,
        StatementCreditEntity::class,
        StatementCreditUsageEntity::class,
        DismissedCreditMatchEntity::class,
        AiMatchCacheEntity::class,
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    abstract fun transactionDao(): TransactionDao
    abstract fun rewardBalanceDao(): RewardBalanceDao
    abstract fun rotatingCategoryDao(): RotatingCategoryDao
    abstract fun offerDao(): OfferDao
    abstract fun statementCreditDao(): StatementCreditDao
    abstract fun aiMatchCacheDao(): AiMatchCacheDao

    companion object {
        const val DB_NAME = "creditcard.db"
    }
}
