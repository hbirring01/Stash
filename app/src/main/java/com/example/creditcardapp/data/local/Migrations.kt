package com.example.creditcardapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v4 → v5 migration.
 *
 * v4 had only `credit_cards` and `transactions`. v5 adds six new columns to
 * `credit_cards` (annual fee, point value, signup bonus tracking) and introduces
 * two new tables (`reward_balances`, `rotating_categories`) for the Rewards Hub.
 *
 * Without this migration, upgrades from v1.2.x → v1.3.0 crashed Room with
 * `IllegalStateException("A migration from 4 to 5 was required but not found")`
 * on first DB access immediately after biometric unlock — looked to the user
 * like the app crashed straight to launcher.
 */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. New columns on credit_cards (all REAL NOT NULL with safe defaults,
        //    plus a nullable deadline). REAL DEFAULT 0 / 1.0 matches the entity.
        db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `annualFee` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `pointValueCents` REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `signupBonusRequiredSpend` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `signupBonusEarnedSpend` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `signupBonusValue` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `signupBonusDeadline` INTEGER")

        // 2. New tables — copied verbatim from schemas/5.json so identityHash matches.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reward_balances` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cardId` INTEGER NOT NULL, " +
                "`programName` TEXT NOT NULL, " +
                "`points` REAL NOT NULL, " +
                "`valuePerPointCents` REAL NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reward_balances_cardId` ON `reward_balances` (`cardId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `rotating_categories` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cardId` INTEGER NOT NULL, " +
                "`category` TEXT NOT NULL, " +
                "`multiplier` REAL NOT NULL, " +
                "`startEpochMillis` INTEGER NOT NULL, " +
                "`endEpochMillis` INTEGER NOT NULL, " +
                "`label` TEXT)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rotating_categories_cardId` ON `rotating_categories` (`cardId`)")
    }
}

/**
 * v5 → v6 migration. Adds the `offers` table for card-linked offers
 * (Amex Offers / Chase Offers / Citi Merchant Offers tracking).
 */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `offers` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`merchantPattern` TEXT NOT NULL, " +
                "`merchantDisplay` TEXT NOT NULL, " +
                "`issuer` TEXT NOT NULL, " +
                "`cardLast4` TEXT, " +
                "`rewardKind` TEXT NOT NULL, " +
                "`rewardValue` REAL NOT NULL, " +
                "`capDollars` REAL, " +
                "`minSpendDollars` REAL NOT NULL DEFAULT 0, " +
                "`expiresAt` INTEGER NOT NULL, " +
                "`activatedAt` INTEGER, " +
                "`source` TEXT NOT NULL DEFAULT 'CURATED', " +
                "`deepLinkUri` TEXT, " +
                "`description` TEXT)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_offers_merchantPattern` ON `offers` (`merchantPattern`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_offers_issuer` ON `offers` (`issuer`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_offers_expiresAt` ON `offers` (`expiresAt`)")
    }
}

/**
 * v6 → v7 migration. Adds the `statement_credits` and `statement_credit_usages`
 * tables for tracking recurring per-card statement credits (Amex Platinum
 * hotel / Uber Cash / airline incidental, CSR travel, Amex Gold dining, etc.)
 * and their per-period usage history.
 */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `statement_credits` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cardId` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`amountDollars` REAL NOT NULL, " +
                "`periodKind` TEXT NOT NULL, " +
                "`periodStartMonth` INTEGER NOT NULL DEFAULT 1, " +
                "`periodStartDay` INTEGER NOT NULL DEFAULT 1, " +
                "`category` TEXT NOT NULL DEFAULT 'OTHER', " +
                "`notes` TEXT, " +
                "`source` TEXT NOT NULL DEFAULT 'MANUAL', " +
                "`createdAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_statement_credits_cardId` ON `statement_credits` (`cardId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `statement_credit_usages` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`creditId` INTEGER NOT NULL, " +
                "`amountDollars` REAL NOT NULL, " +
                "`usedAt` INTEGER NOT NULL, " +
                "`description` TEXT)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_statement_credit_usages_creditId` ON `statement_credit_usages` (`creditId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_statement_credit_usages_usedAt` ON `statement_credit_usages` (`usedAt`)")
    }
}

/**
 * v8 — Auto-tracking for statement credits.
 *
 * Adds matching rules (pattern + Plaid category) to credits, and a
 * (transactionId, source) pair to usage rows so transactions are linked back
 * to their issuer credit. A separate `dismissed_credit_matches` table records
 * matches the user explicitly removed so we never auto-add them again.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // statement_credits: matching rules + auto-track flag
        db.execSQL("ALTER TABLE `statement_credits` ADD COLUMN `matchPattern` TEXT")
        db.execSQL("ALTER TABLE `statement_credits` ADD COLUMN `matchCategory` TEXT")
        db.execSQL("ALTER TABLE `statement_credits` ADD COLUMN `autoTrack` INTEGER NOT NULL DEFAULT 1")

        // statement_credit_usages: link to source transaction + source tag
        db.execSQL("ALTER TABLE `statement_credit_usages` ADD COLUMN `transactionId` TEXT")
        db.execSQL("ALTER TABLE `statement_credit_usages` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'MANUAL'")
        // Unique on (creditId, transactionId) so a single tx can't be logged
        // twice against the same credit on resync. NULL transactionId rows
        // (manual entries) are allowed to repeat — SQLite treats each NULL as
        // distinct in a unique index.
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_statement_credit_usages_creditId_transactionId` " +
                "ON `statement_credit_usages` (`creditId`, `transactionId`)"
        )

        // Persistently dismissed matches: user deleted an auto-logged usage,
        // we record (creditId, txId) so resync doesn't re-create it.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `dismissed_credit_matches` (" +
                "`creditId` INTEGER NOT NULL, " +
                "`transactionId` TEXT NOT NULL, " +
                "`dismissedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`creditId`, `transactionId`))"
        )

        // Seed match rules on the sample credits so the auto-tracker has
        // something to do out of the box.
        // Travel credit -> Plaid PFC category TRAVEL
        db.execSQL("UPDATE statement_credits SET matchCategory = 'TRAVEL' WHERE name = 'Travel Credit' AND matchCategory IS NULL")
        // Hotel + airline credits also fall under TRAVEL
        db.execSQL("UPDATE statement_credits SET matchCategory = 'TRAVEL' WHERE name IN ('Hotel Credit', 'Airline Incidental') AND matchCategory IS NULL")
        // Uber Cash -> merchant pattern
        db.execSQL("UPDATE statement_credits SET matchPattern = 'uber' WHERE name = 'Uber Cash' AND matchPattern IS NULL")
        // Digital Entertainment -> known merchants
        db.execSQL("UPDATE statement_credits SET matchPattern = 'disney|hulu|nytimes|wall street journal|peacock|sirius' WHERE name = 'Digital Entertainment' AND matchPattern IS NULL")
    }
}
