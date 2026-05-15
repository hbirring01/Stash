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
