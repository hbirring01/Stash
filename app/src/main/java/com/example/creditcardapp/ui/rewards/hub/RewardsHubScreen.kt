package com.example.creditcardapp.ui.rewards.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.creditcardapp.domain.model.CreditCard
import com.example.creditcardapp.domain.model.RewardBalance
import com.example.creditcardapp.domain.model.RotatingCategory
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private enum class HubTab(val label: String) {
    Calendar("Calendar"),
    Points("Points"),
    Roi("ROI"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsHubScreen(
    onBack: () -> Unit,
    viewModel: RewardsHubViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(HubTab.Calendar) }
    var showAddBalance by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rewards Hub") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (tab == HubTab.Points) {
                ExtendedFloatingActionButton(
                    onClick = { showAddBalance = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Add balance") },
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab.ordinal) {
                HubTab.values().forEach { t ->
                    Tab(
                        selected = tab == t,
                        onClick = { tab = t },
                        text = { Text(t.label) },
                    )
                }
            }
            when (tab) {
                HubTab.Calendar -> CalendarSection(state.cards, state.rotating)
                HubTab.Points -> PointsSection(
                    cards = state.cards,
                    balances = state.balances,
                    onDelete = viewModel::deleteBalance,
                )
                HubTab.Roi -> RoiSection(state.roi)
            }
        }
    }

    if (showAddBalance) {
        AddBalanceDialog(
            cards = state.cards,
            onDismiss = { showAddBalance = false },
            onSave = { b ->
                viewModel.saveBalance(b)
                showAddBalance = false
            }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Calendar tab
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CalendarSection(
    cards: List<CreditCard>,
    rotating: List<RotatingCategory>,
) {
    val now = System.currentTimeMillis()
    val active = rotating.filter { it.isActive(now) }
    val upcoming = rotating.filter { it.isUpcoming(now) }
    val signupCards = cards.filter { it.hasActiveSignupBonus }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionHeader("Active rotating categories") }
        if (active.isEmpty()) {
            item { EmptyHint("No active rotating bonuses this quarter.") }
        } else {
            items(active, key = { "act-${it.id}" }) { RotatingRow(cards, it, now) }
        }

        item { SectionHeader("Upcoming") }
        if (upcoming.isEmpty()) {
            item { EmptyHint("No upcoming bonuses scheduled.") }
        } else {
            items(upcoming, key = { "up-${it.id}" }) { RotatingRow(cards, it, now) }
        }

        item { SectionHeader("Sign-up bonus progress") }
        if (signupCards.isEmpty()) {
            item { EmptyHint("No active sign-up bonuses.") }
        } else {
            items(signupCards, key = { "sb-${it.id}" }) { SignupRow(it, now) }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun RotatingRow(cards: List<CreditCard>, rc: RotatingCategory, now: Long) {
    val card = cards.firstOrNull { it.id == rc.cardId }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = rc.label?.takeIf { it.isNotBlank() } ?: "${rc.category.name} bonus",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${formatMult(rc.multiplier)} on ${prettyCategory(rc.category.name)}" +
                    (card?.let { " · ${it.nickname?.takeIf { n -> n.isNotBlank() } ?: it.brand}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val daysLeft = TimeUnit.MILLISECONDS.toDays((rc.endEpochMillis - now).coerceAtLeast(0))
            Text(
                text = if (rc.isActive(now)) "$daysLeft days remaining" else "Starts in ${TimeUnit.MILLISECONDS.toDays(rc.startEpochMillis - now).coerceAtLeast(0)} days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SignupRow(card: CreditCard, now: Long) {
    val daysLeft = card.signupBonusDeadline?.let {
        TimeUnit.MILLISECONDS.toDays((it - now).coerceAtLeast(0))
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = card.nickname?.takeIf { it.isNotBlank() } ?: card.brand,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$${"%,.0f".format(card.signupBonusEarnedSpend)} of $${"%,.0f".format(card.signupBonusRequiredSpend)} spent" +
                    (daysLeft?.let { " · $it days left" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { card.signupBonusProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Bonus value: ${dollars(card.signupBonusValue)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Points tab
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PointsSection(
    cards: List<CreditCard>,
    balances: List<RewardBalance>,
    onDelete: (Long) -> Unit,
) {
    if (balances.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "No balances tracked yet. Tap “Add balance” to start.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val total = balances.sumOf { it.estimatedValue }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Total estimated value",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        dollars(total),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        items(balances, key = { it.id }) { b ->
            val card = cards.firstOrNull { it.id == b.cardId }
            BalanceRow(b, card, onDelete = { onDelete(b.id) })
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun BalanceRow(b: RewardBalance, card: CreditCard?, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    b.programName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${"%,.0f".format(b.points)} pts · ${dollars(b.estimatedValue)}" +
                        (card?.let { " · ${it.nickname?.takeIf { n -> n.isNotBlank() } ?: it.brand}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete balance")
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  ROI tab
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun RoiSection(roi: List<CardRoi>) {
    if (roi.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Add a card to see annual-fee ROI insights.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(roi, key = { it.card.id }) { r -> RoiRow(r) }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun RoiRow(r: CardRoi) {
    val verdictColor = when (r.verdict) {
        "Keep" -> MaterialTheme.colorScheme.primary
        "Cancel" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = r.card.nickname?.takeIf { it.isNotBlank() } ?: r.card.brand,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                AssistChip(
                    onClick = {},
                    label = { Text(r.verdict) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = verdictColor.copy(alpha = 0.15f),
                        labelColor = verdictColor,
                    ),
                )
            }
            Spacer(Modifier.height(10.dp))
            RoiLine("Annual fee", dollars(r.card.annualFee))
            RoiLine("YTD spend", dollars(r.ytdSpend))
            RoiLine("YTD rewards earned", dollars(r.ytdRewardValue))
            if (r.signupValue > 0.0) RoiLine("Sign-up bonus value", dollars(r.signupValue))
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            RoiLine(
                "Net result",
                dollars(r.net),
                emphasis = true,
                valueColor = if (r.net >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun RoiLine(
    label: String,
    value: String,
    emphasis: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            style = if (emphasis) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = if (emphasis) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Shared bits
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AddBalanceDialog(
    cards: List<CreditCard>,
    onDismiss: () -> Unit,
    onSave: (RewardBalance) -> Unit,
) {
    var program by rememberSaveable { mutableStateOf("") }
    var points by rememberSaveable { mutableStateOf("") }
    var valueCents by rememberSaveable { mutableStateOf("1.0") }
    var selectedCardId by rememberSaveable { mutableStateOf<Long?>(cards.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add reward balance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = program,
                    onValueChange = { program = it },
                    label = { Text("Program (e.g. Chase UR)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = points,
                    onValueChange = { points = it.filter { c -> c.isDigit() } },
                    label = { Text("Points") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = valueCents,
                    onValueChange = { valueCents = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Value per point (¢)") },
                    singleLine = true,
                )
                if (cards.isNotEmpty()) {
                    Text("Card", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        cards.take(4).forEach { card ->
                            AssistChip(
                                onClick = { selectedCardId = card.id },
                                label = {
                                    Text(card.nickname?.takeIf { it.isNotBlank() } ?: card.brand)
                                },
                                colors = if (selectedCardId == card.id)
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        labelColor = MaterialTheme.colorScheme.onPrimary,
                                    )
                                else AssistChipDefaults.assistChipColors(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pts = points.toDoubleOrNull() ?: return@Button
                    val vpp = valueCents.toDoubleOrNull() ?: 1.0
                    val cid = selectedCardId ?: return@Button
                    onSave(
                        RewardBalance(
                            id = 0L,
                            cardId = cid,
                            programName = program.ifBlank { "Rewards" },
                            points = pts,
                            valuePerPointCents = vpp,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                },
                enabled = points.isNotBlank() && selectedCardId != null,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ──────────────────────────────────────────────────────────────────────────────
//  Helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun dollars(v: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(v)

private fun formatMult(m: Double): String {
    val s = if (m % 1.0 == 0.0) m.toInt().toString() else "%.1f".format(m)
    return "${s}×"
}

private fun prettyCategory(name: String): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
