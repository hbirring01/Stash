package com.app.stash.android.data.ai

import com.app.stash.android.domain.model.RewardCategory

/**
 * Free, instant, offline merchant-name pattern matcher. Catches the long tail
 * of well-known brands that Plaid mis-categorizes (or categorizes as
 * UNKNOWN / GENERAL_MERCHANDISE). Runs before any LLM call.
 *
 * Order matters — first match wins. Patterns are matched against the
 * lowercased merchant/description string with simple `contains`.
 */
internal object MerchantCategoryRules {

    private data class Rule(val needle: String, val category: RewardCategory)

    // Curated, not exhaustive — focused on chains Plaid commonly mis-tags.
    // Keep needles lowercased and short to avoid false negatives on prefixes
    // like "TST*" or "SQ *".
    private val rules: List<Rule> = listOf(
        // DINING
        "starbucks", "dunkin", "mcdonald", "chipotle", "doordash", "uber eats",
        "ubereats", "grubhub", "postmates", "panera", "chick-fil-a", "chickfila",
        "subway", "wendy", "burger king", "taco bell", "kfc", "popeyes",
        "five guys", "shake shack", "domino", "papa john", "pizza hut",
        "olive garden", "applebee", "denny", "ihop", "cheesecake factory",
        "panda express", "sweetgreen", "cava", "caribou coffee", "peet",
        ).map { Rule(it, RewardCategory.DINING) } + listOf(
        // GROCERIES
        "whole foods", "trader joe", "kroger", "safeway", "publix", "aldi",
        "wegmans", "stop & shop", "stop and shop", "shoprite", "food lion",
        "h-e-b", "heb ", "sprouts", "fresh market", "instacart",
        "harris teeter", "giant eagle", "albertsons", "ralphs", "vons",
        ).map { Rule(it, RewardCategory.GROCERIES) } + listOf(
        // GAS
        "shell", "exxon", "chevron", "bp ", "mobil", "marathon", "sunoco",
        "valero", "speedway", "citgo", "76 ", "phillips 66", "wawa",
        "sheetz", "qt ", "quiktrip", "circle k",
        ).map { Rule(it, RewardCategory.GAS) } + listOf(
        // TRAVEL
        "uber", "lyft", "airbnb", "vrbo", "expedia", "booking.com", "kayak",
        "marriott", "hilton", "hyatt", "ihg", "wyndham", "delta air",
        "american air", "united air", "southwest air", "jetblue", "alaska air",
        "spirit air", "frontier", "amtrak", "greyhound", "hertz", "avis",
        "enterprise rent", "budget rent", "national rental", "sixt", "turo",
        "priceline", "tripadvisor", "ryanair", "easyjet",
        ).map { Rule(it, RewardCategory.TRAVEL) } + listOf(
        // SHOPPING
        "amazon", "amzn", "walmart", "target", "costco", "best buy",
        "home depot", "lowe", "ikea", "macys", "macy's", "nordstrom",
        "kohls", "kohl's", "tj maxx", "tjmaxx", "marshalls", "ross stores",
        "old navy", "gap ", "banana republic", "h&m", "zara", "uniqlo",
        "sephora", "ulta", "etsy", "ebay", "wayfair", "apple.com/bill",
        "apple store",
        ).map { Rule(it, RewardCategory.SHOPPING) } + listOf(
        // ENTERTAINMENT
        "netflix", "spotify", "hulu", "disney plus", "disney+", "hbo",
        "max ", "paramount", "peacock", "apple tv", "youtube premium",
        "amc theatre", "amc theater", "regal cinemas", "cinemark",
        "ticketmaster", "stubhub", "axs ", "live nation", "steam ",
        "playstation", "xbox ", "nintendo", "twitch",
        ).map { Rule(it, RewardCategory.ENTERTAINMENT) }

    /**
     * Returns the matching category for [merchant] (preferring the cleaner
     * `merchant_name` field but tolerating raw `name` strings like
     * "TST* STARBUCKS #1234"). Null when no rule matches.
     */
    fun classify(merchant: String): RewardCategory? {
        if (merchant.isBlank()) return null
        val haystack = merchant.lowercase()
        return rules.firstOrNull { haystack.contains(it.needle) }?.category
    }
}
