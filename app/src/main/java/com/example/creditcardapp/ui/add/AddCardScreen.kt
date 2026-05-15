package com.example.creditcardapp.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.creditcardapp.domain.model.CreditCard
import com.example.creditcardapp.domain.model.RewardCategory
import com.example.creditcardapp.ui.components.CreditCardTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    onDone: () -> Unit,
    viewModel: AddCardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Track a card", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 20.dp))
        ) {
            CreditCardTile(
                card = CreditCard(
                    cardholderName = "",
                    last4 = state.last4.padEnd(4, '•'),
                    brand = state.nickname.ifBlank { "CARD" },
                    expiryMonth = 0,
                    expiryYear = 0,
                    balance = state.balance.toDoubleOrNull() ?: 0.0,
                    creditLimit = state.creditLimit.toDoubleOrNull() ?: 0.0
                ),
                enableFlip = false,
                showBadges = false,
            )
            Spacer(Modifier.height(24.dp))

            Field(
                value = state.nickname,
                onValueChange = { v -> viewModel.update { it.copy(nickname = v) } },
                label = "Nickname (optional)"
            )
            Field(
                value = state.last4,
                onValueChange = { v -> viewModel.update { it.copy(last4 = v.filter(Char::isDigit).take(4)) } },
                label = "Last 4 digits",
                keyboardType = KeyboardType.NumberPassword
            )
            Field(
                value = state.creditLimit,
                onValueChange = { v -> viewModel.update { it.copy(creditLimit = v) } },
                label = "Credit limit",
                keyboardType = KeyboardType.Decimal
            )
            Field(
                value = state.balance,
                onValueChange = { v -> viewModel.update { it.copy(balance = v) } },
                label = "Current balance (optional)",
                keyboardType = KeyboardType.Decimal
            )

            Spacer(Modifier.height(20.dp))
            Text(
                "Bonus categories",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Tap a multiplier for each category your card earns extra on. Anything you leave blank earns 1x.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            RewardCategory.entries.forEach { category ->
                CategoryMultiplierRow(
                    category = category,
                    selected = state.rewards[category],
                    onSelect = { mult -> viewModel.setMultiplier(category, mult) },
                )
            }

            state.error?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save(onDone) },
                enabled = state.canSave && !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (state.saving) "Saving…" else "Save card")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}

/** Common bonus multipliers offered on consumer credit cards. */
private val MULTIPLIER_OPTIONS = listOf(2.0, 3.0, 4.0, 5.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryMultiplierRow(
    category: RewardCategory,
    selected: Double?,
    onSelect: (Double) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MULTIPLIER_OPTIONS.forEach { value ->
                val isSelected = selected == value
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        // Tap selected chip again → clear (back to 1x).
                        if (isSelected) onSelect(1.0) else onSelect(value)
                    },
                    label = { Text("${value.toInt()}x") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}
