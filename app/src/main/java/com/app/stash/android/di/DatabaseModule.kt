package com.app.stash.android.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.stash.android.BuildConfig
import com.app.stash.android.data.local.AiMatchCacheDao
import com.app.stash.android.data.local.AppDatabase
import com.app.stash.android.data.local.CreditCardDao
import com.app.stash.android.data.local.DatabaseKeyStore
import com.app.stash.android.data.local.MIGRATION_4_5
import com.app.stash.android.data.local.MIGRATION_5_6
import com.app.stash.android.data.local.MIGRATION_6_7
import com.app.stash.android.data.local.MIGRATION_7_8
import com.app.stash.android.data.local.MIGRATION_8_9
import com.app.stash.android.data.local.OfferDao
import com.app.stash.android.data.local.RewardBalanceDao
import com.app.stash.android.data.local.RotatingCategoryDao
import com.app.stash.android.data.local.StatementCreditDao
import com.app.stash.android.data.local.TransactionDao
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
            // Explicit migrations preserve user data across upgrades. Fall back to
            // destructive wipe only when no migration path is available (e.g. unknown
            // version coming from a much older install) or on downgrade.
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
        if (BuildConfig.DEBUG) builder.addCallback(DebugSeed)
        return builder.build()
    }

    @Provides
    fun provideCreditCardDao(db: AppDatabase): CreditCardDao = db.creditCardDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideRewardBalanceDao(db: AppDatabase): RewardBalanceDao = db.rewardBalanceDao()

    @Provides
    fun provideRotatingCategoryDao(db: AppDatabase): RotatingCategoryDao = db.rotatingCategoryDao()

    @Provides
    fun provideOfferDao(db: AppDatabase): OfferDao = db.offerDao()

    @Provides
    fun provideStatementCreditDao(db: AppDatabase): StatementCreditDao = db.statementCreditDao()

    @Provides
    fun provideAiMatchCacheDao(db: AppDatabase): AiMatchCacheDao = db.aiMatchCacheDao()

    private object DebugSeed : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val sql = """
                INSERT INTO credit_cards
                  (cardholderName, last4, brand, expiryMonth, expiryYear,
                   balance, creditLimit, nickname, updatedAt, rewardsJson,
                   annualFee, pointValueCents,
                   signupBonusRequiredSpend, signupBonusEarnedSpend,
                   signupBonusValue, signupBonusDeadline)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            // Day-30 from now in epoch ms — used to seed a still-active signup bonus.
            val plus60d = now + 60L * 24 * 60 * 60 * 1000

            // Sapphire-style: dining + travel, $95 AF, UR points (1.5¢/pt with travel)
            db.execSQL(sql, arrayOf(
                "", "4242", "Visa", 12, 2028, 1240.55, 8000.0, "Sapphire", now,
                """{"DINING":3.0,"TRAVEL":2.0,"ENTERTAINMENT":2.0,"OTHER":1.0}""",
                95.0, 1.5, 4000.0, 1240.55, 900.0, plus60d
            ))
            // Cash Preferred-style: groceries + gas, $0 AF, cash (1¢/pt)
            db.execSQL(sql, arrayOf(
                "", "8810", "Mastercard", 7, 2027, 320.10, 5000.0, "Cash Preferred", now,
                """{"GROCERIES":6.0,"GAS":3.0,"OTHER":1.0}""",
                0.0, 1.0, 0.0, 0.0, 0.0, null
            ))
            // Platinum-style: travel-heavy, $695 AF, MR (1.5¢/pt)
            db.execSQL(sql, arrayOf(
                "", "1009", "Amex", 3, 2029, 4275.00, 12000.0, "Platinum", now,
                """{"TRAVEL":5.0,"DINING":4.0,"SHOPPING":2.0,"OTHER":1.0}""",
                695.0, 1.5, 8000.0, 4275.00, 1500.0, plus60d
            ))

            // Seed reward-program balances for the Sapphire & Platinum (their cardId
            // = autoGenerated 1 and 3 respectively, in insertion order).
            val balSql = """
                INSERT INTO reward_balances
                  (cardId, programName, points, valuePerPointCents, updatedAt)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
            db.execSQL(balSql, arrayOf(1L, "Chase Ultimate Rewards", 84_500.0, 1.5, now))
            db.execSQL(balSql, arrayOf(3L, "Amex Membership Rewards", 142_300.0, 1.5, now))

            // Seed a rotating-category window: Q2 2026 = 5% gas + streaming on
            // (hypothetical) Freedom-style card.
            val rotSql = """
                INSERT INTO rotating_categories
                  (cardId, category, multiplier, startEpochMillis, endEpochMillis, label)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
            val q2Start = 1_743_465_600_000L // 2026-04-01 UTC
            val q2End = 1_751_414_399_000L   // 2026-06-30 23:59:59 UTC
            db.execSQL(rotSql, arrayOf(2L, "GAS", 5.0, q2Start, q2End, "Q2 2026 — Gas & Streaming"))

            // Curated card-linked offers. These are sample/illustrative — real
            // offers should be refreshed manually or via the user submitting them.
            // Expires 60 days from now so the seed survives a few months of dev.
            val plus30d = now + 30L * 24 * 60 * 60 * 1000
            val offerSql = """
                INSERT INTO offers
                  (merchantPattern, merchantDisplay, issuer, cardLast4, rewardKind,
                   rewardValue, capDollars, minSpendDollars, expiresAt, activatedAt,
                   source, deepLinkUri, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            val chaseUri = "https://secure.chase.com/web/auth/dashboard#/dashboard/offers"
            val amexUri = "https://global.americanexpress.com/offers/eligible"
            val citiUri = "https://online.citi.com/US/ag/dashboard/offers"
            val discoverUri = "https://card.discover.com/cardmembersvcs/deals"
            // (pattern, display, issuer, cardLast4, kind, value, cap, minSpend, expiresAt, source, uri, desc)
            val offers = listOf(
                arrayOf("starbucks", "Starbucks", "Chase", null, "PERCENT", 10.0, 5.0, 0.0, plus30d, null, "CURATED", chaseUri, "10% back on Starbucks purchases"),
                arrayOf("target", "Target", "Amex", null, "PERCENT", 8.0, 25.0, 0.0, plus30d, null, "CURATED", amexUri, "8% back at Target"),
                arrayOf("walmart", "Walmart", "Citi", null, "PERCENT", 5.0, 15.0, 25.0, plus30d, null, "CURATED", citiUri, "5% back on $25+ at Walmart"),
                arrayOf("whole foods", "Whole Foods", "Amex", null, "FLAT", 10.0, null, 50.0, plus30d, null, "CURATED", amexUri, "$10 back on $50+ at Whole Foods"),
                arrayOf("home depot", "The Home Depot", "Chase", null, "PERCENT", 6.0, 30.0, 0.0, plus30d, null, "CURATED", chaseUri, "6% back at Home Depot"),
                arrayOf("uber", "Uber", "Amex", null, "PERCENT", 12.0, 10.0, 0.0, plus30d, null, "CURATED", amexUri, "12% back on Uber rides"),
                arrayOf("doordash", "DoorDash", "Chase", null, "PERCENT", 10.0, 8.0, 0.0, plus30d, null, "CURATED", chaseUri, "10% back on DoorDash"),
                arrayOf("best buy", "Best Buy", "Citi", null, "FLAT", 20.0, null, 100.0, plus30d, null, "CURATED", citiUri, "$20 back on $100+ at Best Buy"),
                arrayOf("cvs", "CVS Pharmacy", "Discover", null, "PERCENT", 5.0, 10.0, 0.0, plus30d, null, "CURATED", discoverUri, "5% back at CVS"),
                arrayOf("shell", "Shell", "Chase", null, "POINTS_MULT", 3.0, null, 0.0, plus30d, null, "CURATED", chaseUri, "3× points at Shell gas stations"),
            )
            offers.forEach { db.execSQL(offerSql, it) }

            // Seed common statement credits on the Sapphire (cardId 1) and
            // Platinum (cardId 3) sample cards so the Credits screen has
            // something to show on first launch.
            //
            // (cardId, name, amountDollars, periodKind, periodStartMonth,
            //  periodStartDay, category, notes, source, createdAt)
            val creditSql = """
                INSERT INTO statement_credits
                  (cardId, name, amountDollars, periodKind, periodStartMonth,
                   periodStartDay, category, notes, source,
                   matchPattern, matchCategory, autoTrack, createdAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            val sampleCredits = listOf(
                arrayOf(1L, "Travel Credit", 300.0, "ANNUAL", 1, 1, "TRAVEL", "Auto-applies to any travel charge", "CURATED", null, "TRAVEL", 1, now),
                arrayOf(3L, "Hotel Credit", 200.0, "ANNUAL", 1, 1, "TRAVEL", "Fine Hotels + Resorts and The Hotel Collection", "CURATED", "marriott|hilton|hyatt|ihg|four seasons", "TRAVEL", 1, now),
                arrayOf(3L, "Uber Cash", 200.0, "ANNUAL", 1, 1, "RIDESHARE", "Monthly $15 / December $35 Uber Cash", "CURATED", "uber", null, 1, now),
                arrayOf(3L, "Airline Incidental", 200.0, "ANNUAL", 1, 1, "TRAVEL", "Select one airline at start of year", "CURATED", "delta|american airlines|united|southwest|jetblue", "TRAVEL", 1, now),
                arrayOf(3L, "Digital Entertainment", 240.0, "ANNUAL", 1, 1, "ENTERTAINMENT", "Disney+ / Hulu / NYT / WSJ etc.", "CURATED", "disney|hulu|nytimes|wall street journal|peacock|sirius", null, 1, now),
            )
            sampleCredits.forEach { db.execSQL(creditSql, it) }
        }
    }
}
