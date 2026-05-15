package com.example.creditcardapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

private const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 12

@Composable
fun PinSetupScreen(onPinSet: (CharArray) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            text = "Create backup PIN",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Used if biometrics aren't available. $MIN_PIN_LENGTH–$MAX_PIN_LENGTH digits.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { new -> pin = new.filter { it.isDigit() }.take(MAX_PIN_LENGTH) },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { new -> confirm = new.filter { it.isDigit() }.take(MAX_PIN_LENGTH) },
            label = { Text("Confirm PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = pin.length >= MIN_PIN_LENGTH && confirm.length >= MIN_PIN_LENGTH,
            onClick = {
                when {
                    pin.length < MIN_PIN_LENGTH ->
                        error = "PIN must be at least $MIN_PIN_LENGTH digits."
                    pin != confirm ->
                        error = "PINs don't match. Try again."
                    else -> {
                        val chars = pin.toCharArray()
                        pin = ""
                        confirm = ""
                        error = null
                        onPinSet(chars)
                    }
                }
            }
        ) { Text("Save PIN") }
    }
}

@Composable
fun PinEntryScreen(
    onVerify: (CharArray) -> Boolean,
    onCancel: (() -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            text = "Enter backup PIN",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { new -> pin = new.filter { it.isDigit() }.take(MAX_PIN_LENGTH) },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = pin.length >= MIN_PIN_LENGTH,
            onClick = {
                val chars = pin.toCharArray()
                pin = ""
                if (chars.isEmpty()) {
                    error = "Enter your PIN."
                } else if (!onVerify(chars)) {
                    error = "Incorrect PIN."
                }
            }
        ) { Text("Unlock") }
        if (onCancel != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onCancel) { Text("Use biometrics") }
        }
    }
}
