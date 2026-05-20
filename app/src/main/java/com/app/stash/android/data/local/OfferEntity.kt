package com.app.stash.android.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A card-linked merchant offer (Amex Offers, Chase Offers, Citi Merchant
 * Offers, etc). StashApp surfaces these to remind the user to activate them
 * in the issuer's own app — we never auto-enroll (no public API exists and
 * scraping the issuer would violate their ToS and risk account closure).
 *
 * Matching: [merchantPattern] is a lowercase substring matched against a
 * place's display name. Curated patterns are deliberately short (e.g.
 * "starbucks") so we catch every store regardless of OSM/Foursquare naming.
 */
@Entity(
    tableName = "offers",
    indices = [Index("merchantPattern"), Index("issuer"), Index("expiresAt")],
)
data class OfferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Lowercase substring matched against merchant display names. */
    val merchantPattern: String,
    /** Display name shown in the UI (e.g. "Starbucks"). */
    val merchantDisplay: String,
    /** Issuer name: Chase / Amex / Citi / Capital One / BoA / Discover / Wells Fargo. */
    val issuer: String,
    /** Optional: lock this offer to a specific cardLast4 the user owns. Null = any card from this issuer. */
    val cardLast4: String? = null,
    /** PERCENT (0-100), FLAT (dollars), POINTS_MULT (multiplier). */
    val rewardKind: String,
    val rewardValue: Double,
    /** Max bonus dollars. Null = uncapped. */
    val capDollars: Double? = null,
    /** Minimum spend to qualify. */
    val minSpendDollars: Double = 0.0,
    /** Epoch millis when this offer expires. */
    val expiresAt: Long,
    /** Epoch millis when the user tapped "Mark activated". Null = not activated. */
    val activatedAt: Long? = null,
    /** Provenance: CURATED / MANUAL / COMMUNITY / EMAIL. */
    val source: String = "CURATED",
    /** Deep-link URI to open the issuer's offers screen (try app scheme first, then https fallback). */
    val deepLinkUri: String? = null,
    val description: String? = null,
)
