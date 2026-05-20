package com.app.stash.android.ui.rewards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.stash.android.domain.model.Offer

/**
 * AI-style "best card here" banner. Surfaces:
 *  - the recommended card and multiplier as a prominent badge,
 *  - a short reason (e.g. "5x dining via Q2 rotating bonus"),
 *  - an estimated cash-back rate when the card has a known point value,
 *  - a signup-bonus progress hint when the AI is nudging the user toward MSR.
 */
@Composable
fun BestCardHero(
    recommendation: PlaceRecommendation?,
    modifier: Modifier = Modifier,
    onActivateOffer: (Offer) -> Unit = {},
    onMarkOfferActivated: (Offer) -> Unit = {},
) {
    if (recommendation == null) return
    val card = recommendation.bestCard ?: return
    val place = recommendation.place

    val scheme = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(
        colors = listOf(scheme.primary, scheme.tertiary)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp)),
        color = scheme.primaryContainer,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SparkleBadge(gradient)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Use ${card.nickname?.takeIf { it.isNotBlank() } ?: card.brand} at ${place.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onPrimaryContainer,
                        maxLines = 2,
                    )
                    Text(
                        text = recommendation.reason.ifBlank {
                            "${formatMult(recommendation.multiplier)} points · •••• ${card.last4}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.88f),
                    )
                    if (recommendation.cashBackCentsPerDollar > 0.0) {
                        Text(
                            text = "≈ ${formatCents(recommendation.cashBackCentsPerDollar)} value per \$1 · •••• ${card.last4}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onPrimaryContainer.copy(alpha = 0.72f),
                        )
                    }
                }

                MultiplierBadge(
                    multiplier = recommendation.multiplier,
                    boosted = recommendation.boostedByRotating,
                    gradient = gradient,
                )
            }

            // Signup-bonus nudge: only shown when the AI picked this card partly
            // because it has an in-progress signup bonus.
            AnimatedVisibility(
                visible = recommendation.signupBonusProgress != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val progress = recommendation.signupBonusProgress ?: 0f
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.RocketLaunch,
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = "Signup bonus · ${(progress * 100).toInt()}% to goal",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = scheme.onPrimaryContainer,
                        trackColor = scheme.onPrimaryContainer.copy(alpha = 0.20f),
                    )
                }
            }

            // Card-linked offer (Amex/Chase/Citi) banner. Two actions:
            //  - "Open <issuer>" deep-links into the issuer's offers screen so
            //    the user can tap Add to activate (we never auto-enroll).
            //  - "Mark activated" updates local state so the offer stops
            //    triggering notifications and is reflected in the offers list.
            AnimatedVisibility(
                visible = recommendation.activeOffer != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val offer = recommendation.activeOffer ?: return@AnimatedVisibility
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocalOffer,
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = "${offer.issuer} offer · ${offer.shortLabel()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onPrimaryContainer,
                        )
                    }
                    offer.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onPrimaryContainer.copy(alpha = 0.80f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { onActivateOffer(offer) }) {
                            Text("Open ${offer.issuer}")
                        }
                        OutlinedButton(onClick = { onMarkOfferActivated(offer) }) {
                            Text("Mark activated")
                        }
                    }
                }
            }
        }
    }
}

/** Soft pulsing sparkle to telegraph "AI suggestion" without being annoying. */
@Composable
private fun SparkleBadge(gradient: Brush) {
    val transition = rememberInfiniteTransition(label = "sparkle")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sparkleAngle",
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(22.dp)
                .rotate(angle * 0.05f), // a very subtle wobble
        )
    }
}

@Composable
private fun MultiplierBadge(multiplier: Double, boosted: Boolean, gradient: Brush) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (boosted) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(2.dp))
                }
                Text(
                    text = formatMult(multiplier),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

private fun formatMult(m: Double): String {
    val s = if (m % 1.0 == 0.0) m.toInt().toString() else "%.1f".format(m)
    return "${s}×"
}

private fun formatCents(c: Double): String =
    if (c >= 1.0) "%.1f¢".format(c) else "%.2f¢".format(c)
