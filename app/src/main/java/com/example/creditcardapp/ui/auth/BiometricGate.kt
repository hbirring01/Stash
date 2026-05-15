package com.example.creditcardapp.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.creditcardapp.data.auth.PinStore

@Composable
fun BiometricGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context as? FragmentActivity }
    val pinStore = remember(context) { PinStore(context.applicationContext) }

    // First-launch PIN setup: blocks everything until a backup PIN exists.
    var hasPin by remember { mutableStateOf(pinStore.hasPin()) }
    if (!hasPin) {
        PinSetupScreen(onPinSet = { chars ->
            pinStore.setPin(chars)
            hasPin = true
        })
        return
    }

    // Intentionally NOT rememberSaveable: backgrounding the app must re-lock.
    var unlocked by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var showPinEntry by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                unlocked = false
                lastError = null
                showPinEntry = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlocked = true
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                lastError = errString.toString()
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock wallet")
            .setSubtitle("Authenticate to view your cards")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        BiometricPrompt(a, executor, callback).authenticate(info)
    }

    LaunchedEffect(Unit) {
        if (!unlocked && !showPinEntry) {
            if (canAuth) prompt() else showPinEntry = true
        }
    }

    when {
        unlocked -> content()
        showPinEntry -> PinEntryScreen(
            onVerify = { chars ->
                val ok = pinStore.verify(chars)
                if (ok) unlocked = true
                ok
            },
            onCancel = if (canAuth) {
                {
                    showPinEntry = false
                    prompt()
                }
            } else null
        )
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
            Text(
                text = "Wallet locked",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = lastError ?: "Authenticate to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = ::prompt) { Text("Unlock") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showPinEntry = true }) { Text("Use backup PIN") }
        }
    }
}
