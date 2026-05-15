package com.example.creditcardapp.ui.plaidsetup

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaidSetupScreen(
    onBack: () -> Unit,
    viewModel: PlaidSetupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val context = LocalContext.current
    val activity = remember(context) { context as? FragmentActivity }
    var authed by rememberSaveable { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val canAuth = remember(activity) {
        if (activity == null) false
        else BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prompt() {
        val a = activity ?: return
        val executor = ContextCompat.getMainExecutor(a)
        val cb = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authed = true
                authError = null
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                authError = errString.toString()
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Plaid credentials")
            .setSubtitle("Authenticate to manage your Plaid keys")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        BiometricPrompt(a, executor, cb).authenticate(info)
    }

    LaunchedEffect(Unit) {
        if (canAuth && !authed) prompt() else if (!canAuth) authed = true
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Plaid credentials") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!authed) {
                LockedView(authError = authError, onUnlock = ::prompt)
            } else if (state.hasCredentials) {
                ConfiguredView(
                    env = state.env,
                    onReset = { viewModel.clear() },
                    onDone = onBack
                )
            } else {
                EditorView(
                    saving = state.saving,
                    initialEnv = state.env,
                    onSave = { id, sec, env -> viewModel.save(id, sec, env, onDone = onBack) },
                    onCancel = onBack
                )
            }
        }
    }
}

@Composable
private fun LockedView(authError: String?, onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Authenticate to continue",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = authError ?: "Plaid credentials are protected by device biometrics.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onUnlock) { Text("Unlock") }
    }
}

@Composable
private fun ConfiguredView(
    env: String,
    onReset: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            spacing = 12.dp
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Credentials are encrypted",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "Your Plaid client ID and secret are stored in the Android Keystore " +
                "(AES-256-GCM, hardware-backed when supported). For security, they cannot " +
                "be displayed again \u2014 you can only replace them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AssistChip(
            onClick = {},
            label = { Text("Environment: $env") }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text("Replace credentials")
        }
    }
}

@Composable
private fun EditorView(
    saving: Boolean,
    initialEnv: String,
    onSave: (clientId: String, secret: String, env: String) -> Unit,
    onCancel: () -> Unit
) {
    var clientId by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var env by rememberSaveable { mutableStateOf(initialEnv.ifBlank { "sandbox" }) }
    var secretVisible by rememberSaveable { mutableStateOf(true) }

    // Clear secret from memory when leaving the composable. The TextField keeps
    // the value in saveable state during config changes, but on permanent exit
    // we forget it so it isn't lingering longer than needed.
    DisposableEffect(Unit) {
        onDispose {
            clientId = ""
            secret = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Enter your Plaid client ID and secret. They will be encrypted with a " +
                "Keystore-backed key and never shown again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it },
            label = { Text("Client ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
        )
        OutlinedTextField(
            value = secret,
            onValueChange = { secret = it },
            label = { Text("Secret") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { secretVisible = !secretVisible }) {
                    Icon(
                        imageVector = if (secretVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (secretVisible) "Hide secret" else "Show secret"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Text(
            text = "Environment",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            spacing = 8.dp,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("sandbox", "development", "production").forEach { option ->
                FilterChip(
                    selected = env == option,
                    onClick = { env = option },
                    label = { Text(option) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onSave(clientId, secret, env) },
            enabled = !saving && clientId.isNotBlank() && secret.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (saving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(18.dp)
                )
                Spacer(Modifier.height(0.dp))
                Text("  Saving…")
            } else {
                Text("Save securely")
            }
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

// Tiny helper so callers can write Row(spacing = N) without repeating Arrangement.spacedBy.
@Composable
private fun Row(
    spacing: androidx.compose.ui.unit.Dp,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = verticalAlignment,
        content = content
    )
}
