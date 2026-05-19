package com.example.creditcardapp.ui.credits

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.creditcardapp.domain.model.CreditCategory
import com.example.creditcardapp.domain.model.CreditPeriod
import com.example.creditcardapp.domain.model.StatementCredit
import com.example.creditcardapp.ui.format.asCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBack: () -> Unit,
    showBack: Boolean = true,
    viewModel: StatementCreditsViewModel = hiltViewModel(),
) {
    val credits by viewModel.credits.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<StatementCredit?>(null) }
    var loggingFor by remember { mutableStateOf<CreditUiState?>(null) }
    var addingNew by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Statement credits") },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addingNew = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add credit")
            }
        },
    ) { padding ->
        if (credits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Tap + to track a credit (Amex Plat hotel, CSR travel, Gold dining…).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(credits, key = { it.credit.id }) { state ->
                CreditRow(
                    state = state,
                    onLog = { loggingFor = state },
                    onEdit = { editing = state.credit },
                    onDelete = { viewModel.deleteCredit(state.credit.id) },
                )
            }
        }
    }

    if (addingNew) {
        CreditEditorDialog(
            initial = null,
            onDismiss = { addingNew = false },
            onSave = {
                viewModel.saveCredit(it)
                addingNew = false
            },
        )
    }
    editing?.let { current ->
        CreditEditorDialog(
            initial = current,
            onDismiss = { editing = null },
            onSave = {
                viewModel.saveCredit(it)
                editing = null
            },
        )
    }
    loggingFor?.let { state ->
        LogUsageDialog(
            state = state,
            onDismiss = { loggingFor = null },
            onConfirm = { amount, desc ->
                viewModel.logUsage(state.credit.id, amount, desc)
                loggingFor = null
            },
        )
    }
}

@Composable
private fun CreditRow(
    state: CreditUiState,
    onLog: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val credit = state.credit
    var menuOpen by remember { mutableStateOf(false) }
    val resetDate = remember(credit.id, credit.periodKind) {
        val end = credit.periodWindow().last + 1
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(end))
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        credit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val cardLabel = state.card?.let { it.nickname ?: "${it.brand} •••• ${it.last4}" }
                    if (cardLabel != null) {
                        Text(
                            cardLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (credit.autoTrack && (credit.matchPattern != null || credit.matchCategory != null)) {
                        Text(
                            "Auto-tracked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${state.usedInPeriod.asCurrency()} used of ${credit.amountDollars.asCurrency()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { state.percentUsed },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val days = credit.daysRemaining()
                Text(
                    if (days >= 0) "$days days left · resets $resetDate" else "Period ended",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = onLog) { Text("Mark used") }
            }
        }
    }
}

@Composable
private fun LogUsageDialog(
    state: CreditUiState,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, description: String?) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val parsed = amount.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log usage") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${state.credit.name} — ${state.remaining.asCurrency()} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") },
                    prefix = { Text("$") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { parsed?.let { onConfirm(it, description.ifBlank { null }) } },
                enabled = parsed != null && parsed > 0.0,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditEditorDialog(
    initial: StatementCredit?,
    onDismiss: () -> Unit,
    onSave: (StatementCredit) -> Unit,
    viewModel: StatementCreditsViewModel = hiltViewModel(),
) {
    // Local form state, seeded from `initial` when editing.
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var amount by remember { mutableStateOf(initial?.amountDollars?.toString().orEmpty()) }
    var period by remember { mutableStateOf(initial?.periodKind ?: CreditPeriod.ANNUAL) }
    var category by remember { mutableStateOf(initial?.category ?: CreditCategory.OTHER) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var matchPattern by remember { mutableStateOf(initial?.matchPattern.orEmpty()) }
    var matchCategory by remember { mutableStateOf(initial?.matchCategory.orEmpty()) }
    var autoTrack by remember { mutableStateOf(initial?.autoTrack ?: true) }
    var cardId by remember { mutableStateOf(initial?.cardId ?: 0L) }
    var periodMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var cardMenu by remember { mutableStateOf(false) }

    // Pull the card list directly from the VM so the picker is populated even
    // before any credits exist.
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    // When creating new, default to the first available card.
    if (cardId == 0L && cards.isNotEmpty()) {
        cardId = cards.first().id
    }
    val selectedCard = cards.firstOrNull { it.id == cardId }

    val parsedAmount = amount.toDoubleOrNull()
    val canSave = name.isNotBlank() && parsedAmount != null && parsedAmount > 0.0 && cardId > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New credit" else "Edit credit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cards.isEmpty()) {
                    Text(
                        "Add a card first — credits attach to a card.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = cardMenu,
                        onExpandedChange = { cardMenu = !cardMenu },
                    ) {
                        OutlinedTextField(
                            value = selectedCard?.let { it.nickname ?: "${it.brand} •••• ${it.last4}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Card") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        DropdownMenu(
                            expanded = cardMenu,
                            onDismissRequest = { cardMenu = false },
                        ) {
                            cards.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.nickname ?: "${c.brand} •••• ${c.last4}") },
                                    onClick = { cardId = c.id; cardMenu = false },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Hotel credit)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount per period") },
                    prefix = { Text("$") },
                    singleLine = true,
                )
                ExposedDropdownMenuBox(
                    expanded = periodMenu,
                    onExpandedChange = { periodMenu = !periodMenu },
                ) {
                    OutlinedTextField(
                        value = period.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Period") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = periodMenu,
                        onDismissRequest = { periodMenu = false },
                    ) {
                        CreditPeriod.values().forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.label()) },
                                onClick = { period = p; periodMenu = false },
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = categoryMenu,
                    onExpandedChange = { categoryMenu = !categoryMenu },
                ) {
                    OutlinedTextField(
                        value = category.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = categoryMenu,
                        onDismissRequest = { categoryMenu = false },
                    ) {
                        CreditCategory.values().forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.label()) },
                                onClick = { category = c; categoryMenu = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                )

                // --- Auto-tracking ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-track", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Log matching Plaid transactions automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = autoTrack,
                        onCheckedChange = { autoTrack = it },
                    )
                }
                if (autoTrack) {
                    OutlinedTextField(
                        value = matchPattern,
                        onValueChange = { matchPattern = it },
                        label = { Text("Match merchants (e.g. uber|lyft)") },
                        supportingText = { Text("Pipe-separated. Case-insensitive substring match.") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = matchCategory,
                        onValueChange = { matchCategory = it.uppercase() },
                        label = { Text("Match Plaid category (e.g. TRAVEL)") },
                        supportingText = { Text("Primary PFC. Leave blank to skip.") },
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val saved = (initial ?: StatementCredit(
                        cardId = cardId,
                        name = name,
                        amountDollars = parsedAmount!!,
                        periodKind = period,
                    )).copy(
                        cardId = cardId,
                        name = name.trim(),
                        amountDollars = parsedAmount!!,
                        periodKind = period,
                        category = category,
                        notes = notes.ifBlank { null },
                        matchPattern = matchPattern.trim().ifBlank { null },
                        matchCategory = matchCategory.trim().ifBlank { null },
                        autoTrack = autoTrack,
                    )
                    onSave(saved)
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun CreditPeriod.label(): String = when (this) {
    CreditPeriod.ANNUAL -> "Annual"
    CreditPeriod.SEMI_ANNUAL -> "Semi-annual"
    CreditPeriod.QUARTERLY -> "Quarterly"
    CreditPeriod.MONTHLY -> "Monthly"
}

private fun CreditCategory.label(): String = when (this) {
    CreditCategory.TRAVEL -> "Travel"
    CreditCategory.DINING -> "Dining"
    CreditCategory.RIDESHARE -> "Rideshare"
    CreditCategory.ENTERTAINMENT -> "Entertainment"
    CreditCategory.SHOPPING -> "Shopping"
    CreditCategory.WELLNESS -> "Wellness"
    CreditCategory.OTHER -> "Other"
}
