package com.example.creditcardapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creditcardapp.domain.model.CreditCard
import com.example.creditcardapp.domain.model.RewardCategory

/**
 * Row of small badges showing the top reward multipliers for a card
 * (e.g. "3× Dining", "2× Travel"). Anything ≥ 2x is considered a "bonus" worth
 * surfacing; cards with only the base 1x rate render nothing.
 *
 * @param maxBadges How many of the highest multipliers to show (defaults to 3).
 * @param onSurface The text/background tint, defaults to the surrounding scheme.
 */
@Composable
fun RewardBadges(
    card: CreditCard,
    modifier: Modifier = Modifier,
    maxBadges: Int = 3,
    onSurface: Color = Color(0xFFF7F7F2),
) {
    val bonuses = card.rewards
        .filter { (k, v) -> v >= 2.0 && k != RewardCategory.OTHER }
        .toList()
        .sortedByDescending { it.second }
        .take(maxBadges)
    if (bonuses.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        bonuses.forEach { (category, mult) ->
            Badge(
                text = "${formatMultiplier(mult)}× ${category.displayName}",
                tint = onSurface,
            )
        }
    }
}

@Composable
private fun Badge(text: String, tint: Color) {
    Text(
        text = text,
        color = tint,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun formatMultiplier(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
