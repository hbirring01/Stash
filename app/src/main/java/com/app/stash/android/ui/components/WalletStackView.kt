package com.app.stash.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.app.stash.android.domain.model.CreditCard

/**
 * Apple-Wallet-style stacked deck of cards. By default the cards are
 * collapsed: each one peeks ~52 dp below the one above it. Tapping the deck
 * fans the cards out vertically into a scrollable list; tapping again
 * recollapses. The currently-selected card (default: top of the deck) appears
 * fully on top.
 *
 * The collapsed look uses a tiny per-row scaleX falloff so cards further down
 * the stack appear slightly smaller, mimicking Wallet's perspective trick.
 *
 * @param cards The cards in display order (top-of-stack first).
 * @param logoBase64 Institution logo to render on each card (front face).
 * @param onCardTap Invoked when an *expanded* card is tapped (so the parent can
 *   navigate to a detail view). In the collapsed state, taps just expand the deck.
 */
@Composable
fun WalletStackView(
    cards: List<CreditCard>,
    logoBase64: String?,
    modifier: Modifier = Modifier,
    onCardTap: (CreditCard) -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    if (cards.isEmpty()) return

    Box(modifier = modifier.fillMaxSize()) {
        if (!expanded) {
            CollapsedStack(
                cards = cards,
                logoBase64 = logoBase64,
                onExpand = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp,
                ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
            ) {
                items(cards, key = { it.id }) { card ->
                    CreditCardTile(
                        card = card,
                        logoBase64 = logoBase64,
                        onClick = { onCardTap(card) },
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Tap stack icon to collapse",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedStack(
    cards: List<CreditCard>,
    logoBase64: String?,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Peek offset: each subsequent card is shifted down by this much, exposing
    // a thin horizontal strip of the card beneath. 56 dp matches the natural
    // "label strip" at the top of the standard credit-card tile.
    val peekDp = 56.dp
    val perCardScaleStep = 0.04f  // each deeper card is ~4% narrower

    Box(modifier = modifier) {
        // Render top-of-stack LAST so it sits on top.
        cards.reversed().forEachIndexed { reverseIdx, card ->
            val depth = cards.size - 1 - reverseIdx // 0 = top, N-1 = bottom
            val scale = (1f - depth * perCardScaleStep).coerceAtLeast(0.8f)
            val yOffset = peekDp * depth
            CreditCardTile(
                card = card,
                logoBase64 = logoBase64,
                enableFlip = false, // suppress flip while collapsed
                onClick = onExpand,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = yOffset)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        // Subtle shadow makes the layering visually crisp.
                        shadowElevation = (10f * (1f - depth / (cards.size.coerceAtLeast(1)).toFloat()))
                    }
                    .shadow(elevation = 4.dp),
            )
        }
        // Spacer at the bottom so the box reserves room for the whole stack
        // even though all children are z-positioned.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(peekDp * (cards.size - 1).coerceAtLeast(0) + 200.dp)
                .background(androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}
