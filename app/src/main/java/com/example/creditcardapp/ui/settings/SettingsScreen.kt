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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.Switch
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
import com.example.creditcardapp.BuildConfig
import com.example.creditcardapp.data.ai.AiProvider
import com.example.creditcardapp.data.preferences.AiConfigState
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
    aiSettingsViewModel: AiSettingsViewModel = hiltViewModel(),
) {
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val hasFoursquareKey by apiKeyViewModel.hasFoursquareKey.collectAsStateWithLifecycle()
    val hasPlaidKeys by plaidKeyViewModel.hasPlaidKeys.collectAsStateWithLifecycle()
    val aiState by aiSettingsViewModel.state.collectAsStateWithLifecycle()
    val aiCacheCount by aiSettingsViewModel.cacheCount.collectAsStateWithLifecycle()

    var showFoursquareDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

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
            // ---- AI Assist ----
            // Opt-in LLM fallback for statement-credit matching. Off by default;
            // user provides their own free-tier API key (Gemini / Groq / etc.).
            item {
                SettingsCard {
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAiDialog = true },
                        headlineContent = { Text("AI Assist") },
                        supportingContent = { Text(aiState.supportingText()) },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingContent = {
                            if (aiState.hasKey) {
                                Switch(
                                    checked = aiState.enabled,
                                    onCheckedChange = { aiSettingsViewModel.setEnabled(it) },
                                )
                            } else {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    )
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "StashApp · Built with Compose & Material 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

    if (showAiDialog) {
        AiSettingsDialog(
            state = aiState,
            cacheCount = aiCacheCount,
            onSave = { provider, key, baseUrl, model ->
                aiSettingsViewModel.save(provider, key, baseUrl, model)
            },
            onClear = { aiSettingsViewModel.clear() },
            onClearCache = { aiSettingsViewModel.clearCache() },
            onDismiss = { showAiDialog = false },
        )
    }
}

private fun AiConfigState.supportingText(): String = when {
    !hasKey -> "Off · tap to add a free API key"
    !enabled -> "Paused · " + provider.displayName
    else -> "On · " + (modelOverride ?: provider.defaultModel)
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

/**
 * AI Assist setup modal. Mirrors the security stance of [ApiKeyDialog]:
 *  - never displays the currently saved key,
 *  - password-mask while typing,
 *  - "Advanced" disclosure for users who want to point at Ollama / a custom
 *    OpenAI-compatible endpoint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSettingsDialog(
    state: AiConfigState,
    cacheCount: Int,
    onSave: (AiProvider, String, String?, String?) -> Unit,
    onClear: () -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit,
) {
    var provider by remember(state) { mutableStateOf(state.provider) }
    var providerMenuOpen by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    var advancedOpen by remember { mutableStateOf(false) }
    var baseUrl by remember(state) { mutableStateOf(state.baseUrlOverride.orEmpty()) }
    var model by remember(state) { mutableStateOf(state.modelOverride.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Assist") },
        text = {
            Column {
                Text(
                    "Smarter credit matching: when a transaction doesn't match a credit's " +
                        "literal rules, an LLM is asked one short question (\"does this merchant " +
                        "qualify?\"). Off by default \u2014 you provide the API key.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = providerMenuOpen,
                    onExpandedChange = { providerMenuOpen = it },
                ) {
                    OutlinedTextField(
                        value = provider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuOpen)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = providerMenuOpen,
                        onDismissRequest = { providerMenuOpen = false },
                    ) {
                        AiProvider.values().forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.displayName) },
                                onClick = {
                                    provider = p
                                    providerMenuOpen = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    provider.signupHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(if (state.hasKey) "New API key" else "API key") },
                    placeholder = {
                        if (state.hasKey) Text("(keep current \u2014 leave blank)") else null
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { advancedOpen = !advancedOpen }) {
                    Text(if (advancedOpen) "Hide advanced" else "Show advanced")
                }
                if (advancedOpen) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL override") },
                        placeholder = { Text(provider.defaultBaseUrl.ifEmpty { "https://\u2026/v1/" }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model override") },
                        placeholder = { Text(provider.defaultModel.ifEmpty { "model-name" }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Cache management. Only meaningful once the user has actually
                // exercised the matcher, so hide the row entirely at zero.
                if (cacheCount > 0) {
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Cached matches",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            val suffix = if (cacheCount == 1) "" else "s"
                            Text(
                                "$cacheCount merchant verdict$suffix \u2014 reused so the model isn't re-asked.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onClearCache) { Text("Clear") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = apiKey.isNotBlank() || (state.hasKey && state.provider == provider),
                onClick = {
                    // If user didn't paste a new key but has one saved AND the provider
                    // didn't change, leave the key alone \u2014 only update overrides via
                    // an explicit clear-then-save flow (kept simple here: require new key
                    // to change provider).
                    if (apiKey.isNotBlank()) {
                        onSave(
                            provider,
                            apiKey,
                            baseUrl.takeIf { it.isNotBlank() },
                            model.takeIf { it.isNotBlank() },
                        )
                    }
                    onDismiss()
                },
            ) { Text(if (state.hasKey) "Replace" else "Save") }
        },
        dismissButton = {
            if (state.hasKey) {
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
