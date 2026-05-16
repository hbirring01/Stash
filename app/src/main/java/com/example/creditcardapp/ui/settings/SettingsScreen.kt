package com.example.creditcardapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.creditcardapp.data.preferences.ThemeMode
import com.example.creditcardapp.ui.AppViewModel
import com.example.creditcardapp.ui.list.CardListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPlaidSetup: () -> Unit,
    appViewModel: AppViewModel = hiltViewModel(),
    cardListViewModel: CardListViewModel = hiltViewModel(),
    apiKeyViewModel: ApiKeySettingsViewModel = hiltViewModel(),
    plaidKeyViewModel: PlaidKeySettingsViewModel = hiltViewModel(),
) {
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val hasFoursquareKey by apiKeyViewModel.hasFoursquareKey.collectAsStateWithLifecycle()
    val hasPlaidKeys by plaidKeyViewModel.hasPlaidKeys.collectAsStateWithLifecycle()

    var showFoursquareDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ThemeCard(
                    current = themeMode,
                    onSelect = { appViewModel.setThemeMode(it) },
                )
            }
            item {
                SettingsCard {
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { cardListViewModel.connectBank() },
                        headlineContent = { Text("Connect a bank") },
                        supportingContent = { Text("Link via Plaid to import cards & transactions") },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingContent = {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    )
                }
            }
            // ---- Secrets section ----
            // Both rows follow the same UX: surface only a Set / Not set status
            // and a path to update. The stored values are encrypted at rest and
            // never read back into UI.
            item {
                SettingsCard {
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlaidSetup() },
                        headlineContent = { Text("Plaid keys") },
                        supportingContent = {
                            Text(if (hasPlaidKeys) "Set · tap to replace or remove" else "Not set")
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingContent = {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    )
                }
            }
            item {
                SettingsCard {
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFoursquareDialog = true },
                        headlineContent = { Text("Foursquare API key") },
                        supportingContent = {
                            Text(if (hasFoursquareKey) "Set · tap to replace or remove" else "Not set")
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingContent = {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    )
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "StashApp · Built with Compose & Material 3",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }

    if (showFoursquareDialog) {
        ApiKeyDialog(
            title = "Foursquare API key",
            hasExistingKey = hasFoursquareKey,
            onSave = { raw -> apiKeyViewModel.saveFoursquareKey(raw) { /* swallowed */ } },
            onClear = { apiKeyViewModel.clearFoursquareKey() },
            onDismiss = { showFoursquareDialog = false },
        )
    }
}

/**
 * Modal for adding / replacing / removing an API key. Deliberately:
 *  - never receives or displays the current key value,
 *  - applies a password-mask visual transformation so the new value isn't shown
 *    while typing (so shoulder-surfing reveals nothing more than length).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyDialog(
    title: String,
    hasExistingKey: Boolean,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = if (hasExistingKey) {
                        "A key is currently saved (encrypted at rest). It can't be displayed. " +
                            "Paste a new key below to replace it, or remove the saved key."
                    } else {
                        "Paste your key below. It will be encrypted and stored on this device only."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(if (hasExistingKey) "New key" else "Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank(),
                onClick = {
                    onSave(input)
                    onDismiss()
                },
            ) { Text(if (hasExistingKey) "Replace" else "Save") }
        },
        dismissButton = {
            if (hasExistingKey) {
                TextButton(onClick = {
                    onClear()
                    onDismiss()
                }) { Text("Remove key") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeCard(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    SettingsCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Match the system, or pin a light or dark scheme",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    Triple(ThemeMode.SYSTEM, "System", Icons.Outlined.PhoneAndroid),
                    Triple(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
                    Triple(ThemeMode.DARK, "Dark", Icons.Outlined.Bedtime),
                )
                options.forEachIndexed { index, (mode, label, icon) ->
                    SegmentedButton(
                        selected = current == mode,
                        onClick = { onSelect(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = { ThemeIcon(icon) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null)
}
