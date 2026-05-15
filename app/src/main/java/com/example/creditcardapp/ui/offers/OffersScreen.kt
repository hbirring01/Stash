package com.example.creditcardapp.ui.offers

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.creditcardapp.domain.model.Offer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    onBack: () -> Unit,
    viewModel: OffersViewModel = hiltViewModel(),
) {
    val offers by viewModel.offers.collectAsStateWithLifecycle()
    val backgroundEnabled by viewModel.backgroundOffersEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }

    // Two-step permission flow: foreground first, then background on Q+.
    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.setBackgroundOffersEnabled(granted)
    }
    val fgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!fineGranted) {
            viewModel.setBackgroundOffersEnabled(false)
            return@rememberLauncherForActivityResult
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            viewModel.setBackgroundOffersEnabled(true)
        }
    }

    val onToggleBackground: (Boolean) -> Unit = { wantOn ->
        if (!wantOn) {
            viewModel.setBackgroundOffersEnabled(false)
        } else if (viewModel.hasBackgroundLocationPermission()) {
            viewModel.setBackgroundOffersEnabled(true)
        } else {
            fgLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Card-linked offers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (offers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No offers yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            item {
                BackgroundNotificationsCard(
                    enabled = backgroundEnabled,
                    onToggle = onToggleBackground,
                    onOpenSettings = {
                        runCatching {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData("package:${context.packageName}".toUri())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    },
                )
            }
            item {
                Text(
                    text = "Tap Open to activate in your issuer's app. " +
                        "We never auto-enroll — that would violate issuer ToS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(offers, key = { it.id }) { offer ->
                OfferRow(
                    offer = offer,
                    isExpired = offer.expiresAt < now,
                    onToggleActivated = { activated ->
                        viewModel.setActivated(offer.id, activated)
                    },
                    onOpenIssuer = {
                        offer.deepLinkUri?.let { uri ->
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun OfferRow(
    offer: Offer,
    isExpired: Boolean,
    onToggleActivated: (Boolean) -> Unit,
    onOpenIssuer: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (offer.isActivated) Icons.Outlined.CheckCircle else Icons.Outlined.LocalOffer,
                    contentDescription = null,
                    tint = if (offer.isActivated) scheme.tertiary else scheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.merchantDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isExpired) TextDecoration.LineThrough else null,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = "${offer.issuer} · ${offer.shortLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = offer.isActivated,
                    onCheckedChange = onToggleActivated,
                    enabled = !isExpired,
                )
            }
            offer.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isExpired) "Expired ${formatDate(offer.expiresAt)}"
                else "Expires ${formatDate(offer.expiresAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isExpired) scheme.error else scheme.onSurfaceVariant,
            )
            if (!isExpired && offer.deepLinkUri != null) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onOpenIssuer) {
                        Text("Open ${offer.issuer}")
                    }
                    if (!offer.isActivated) {
                        OutlinedButton(onClick = { onToggleActivated(true) }) {
                            Text("Mark activated")
                        }
                    }
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
private fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

@Composable
private fun BackgroundNotificationsCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notify me near offer merchants",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (enabled) "On — geofences refresh on each map open"
                        else "Off — only fires while the map screen is open",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Background location is required so Android can wake us up when you arrive at a merchant — we only check location near places you've already passed near.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            if (enabled) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenSettings) {
                    Text("Manage permissions")
                }
            }
        }
    }
}
