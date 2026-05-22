package com.app.stash.android.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.stash.android.data.preferences.ThemeMode
import com.app.stash.android.domain.model.CreditCard
import com.app.stash.android.domain.model.RewardCategory
import com.app.stash.android.ui.components.CreditCardTile
import com.android.tools.screenshot.PreviewTest
import com.app.stash.android.ui.theme.StashTheme

// ---------------------------------------------------------------------------
// Screenshot test previews. Each @Preview function in this source set
// generates a PNG under app/build/outputs/screenshotTest-results/preview/
// when `./gradlew :app:updateDebugScreenshotTest` runs. The CI workflow
// copies a curated subset of those PNGs into `/screenshots/` on a release.
//
// These previews intentionally render stateless Composables only â€” full
// screen Composables (HomeScreen, SettingsScreen, etc.) still take Hilt
// ViewModels and need a stateless-content refactor before they can be
// snapshotted here. See the PR for the follow-up plan.
// ---------------------------------------------------------------------------

private val sampleCard = CreditCard(
    id = 1L,
    cardholderName = "ALEX MORGAN",
    last4 = "4242",
    brand = "VISA",
    expiryMonth = 8,
    expiryYear = 2029,
    balance = 1_284.50,
    creditLimit = 12_500.00,
    nickname = "Travel rewards",
    rewards = mapOf(
        RewardCategory.DINING to 3.0,
        RewardCategory.TRAVEL to 5.0,
        RewardCategory.GROCERIES to 2.0,
        RewardCategory.OTHER to 1.0,
    ),
    annualFee = 95.0,
    pointValueCents = 1.5,
    signupBonusRequiredSpend = 4_000.0,
    signupBonusEarnedSpend = 1_750.0,
    signupBonusValue = 750.0,
)

private val secondaryCard = CreditCard(
    id = 2L,
    cardholderName = "ALEX MORGAN",
    last4 = "0117",
    brand = "MASTERCARD",
    expiryMonth = 3,
    expiryYear = 2028,
    balance = 412.10,
    creditLimit = 8_000.00,
    nickname = "Cash back",
    rewards = mapOf(
        RewardCategory.GROCERIES to 3.0,
        RewardCategory.GAS to 2.0,
        RewardCategory.OTHER to 1.0,
    ),
)

/**
 * Single card tile, rendered at typical wallet width. Mapped onto
 * `screenshots/wallet.png` by the release workflow.
 */
@PreviewTest
@Preview(
    name = "Wallet card",
    showBackground = true,
    widthDp = 380,
    heightDp = 240,
)
@Composable
fun WalletCardPreview() {
    StashTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(16.dp)) {
                CreditCardTile(
                    card = sampleCard,
                    enableFlip = false,
                    showBadges = true,
                )
            }
        }
    }
}

/**
 * Two cards stacked vertically â€” gives a flavour of the wallet list view
 * without depending on the Hilt-injected `CardListScreen`. Mapped onto
 * `screenshots/home2.png`.
 */
@PreviewTest
@Preview(
    name = "Wallet stack",
    showBackground = true,
    widthDp = 380,
    heightDp = 560,
)
@Composable
fun WalletStackPreview() {
    StashTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "Wallet",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                item {
                    CreditCardTile(
                        card = sampleCard,
                        enableFlip = false,
                        showBadges = true,
                    )
                }
                item {
                    CreditCardTile(
                        card = secondaryCard,
                        enableFlip = false,
                        showBadges = true,
                    )
                }
            }
        }
    }
}

/**
 * Dark-theme variant of the wallet stack. Same Composable, theme flipped â€”
 * a cheap way to verify both colour schemes survive future Material updates.
 */
@PreviewTest
@Preview(
    name = "Wallet stack (dark)",
    showBackground = true,
    widthDp = 380,
    heightDp = 560,
)
@Composable
fun WalletStackDarkPreview() {
    StashTheme(themeMode = ThemeMode.DARK, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "Wallet",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                item {
                    CreditCardTile(
                        card = sampleCard,
                        enableFlip = false,
                        showBadges = true,
                    )
                }
                item {
                    CreditCardTile(
                        card = secondaryCard,
                        enableFlip = false,
                        showBadges = true,
                    )
                }
            }
        }
    }
}



