package com.example.creditcardapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.creditcardapp.BuildConfig
import com.example.creditcardapp.data.local.AppDatabase
import com.example.creditcardapp.data.local.CreditCardDao
import com.example.creditcardapp.data.local.DatabaseKeyStore
import com.example.creditcardapp.data.local.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseKeyStore(@ApplicationContext context: Context): DatabaseKeyStore =
        DatabaseKeyStore(context)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyStore: DatabaseKeyStore,
    ): AppDatabase {
        // Load SQLCipher's native libraries before any DB call.
        SQLiteDatabase.loadLibs(context)

        // If we've never written a passphrase, any database file on disk is a
        // legacy (unencrypted) one from a previous build. Delete it so SQLCipher
        // doesn't fail to open it with the new key. fallbackToDestructiveMigration
        // handles schema mismatches but not the encrypted/unencrypted boundary.
        if (!keyStore.hasKey()) {
            context.deleteDatabase(AppDatabase.DB_NAME)
        }
        val passphrase = keyStore.getOrCreatePassphrase()
        val factory = SupportFactory(passphrase)

        val builder = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
        if (BuildConfig.DEBUG) builder.addCallback(DebugSeed)
        return builder.build()
    }

    @Provides
    fun provideCreditCardDao(db: AppDatabase): CreditCardDao = db.creditCardDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    private object DebugSeed : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val sql = """
                INSERT INTO credit_cards
                  (cardholderName, last4, brand, expiryMonth, expiryYear,
                   balance, creditLimit, nickname, updatedAt, rewardsJson)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            // Sapphire-style: dining + travel
            db.execSQL(sql, arrayOf("", "4242", "Visa", 12, 2028, 1240.55, 8000.0, "Sapphire", now,
                """{"DINING":3.0,"TRAVEL":2.0,"ENTERTAINMENT":2.0,"OTHER":1.0}"""))
            // Cash Preferred-style: groceries + gas
            db.execSQL(sql, arrayOf("", "8810", "Mastercard", 7, 2027, 320.10, 5000.0, "Cash Preferred", now,
                """{"GROCERIES":6.0,"GAS":3.0,"OTHER":1.0}"""))
            // Platinum-style: travel-heavy
            db.execSQL(sql, arrayOf("", "1009", "Amex", 3, 2029, 4275.00, 12000.0, "Platinum", now,
                """{"TRAVEL":5.0,"DINING":4.0,"SHOPPING":2.0,"OTHER":1.0}"""))
        }
    }
}
