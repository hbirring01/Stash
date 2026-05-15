package com.example.creditcardapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CreditCardEntity::class, TransactionEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DB_NAME = "creditcard.db"
    }
}
