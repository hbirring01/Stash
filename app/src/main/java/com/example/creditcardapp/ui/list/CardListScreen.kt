package com.example.creditcardapp.ui.list

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.creditcardapp.domain.model.CreditCard
import com.example.creditcardapp.ui.components.CreditCardTile
import com.example.creditcardapp.ui.components.EmptyCardsState
import com.example.creditcardapp.ui.components.IndeterminateWave
import com.example.creditcardapp.ui.components.WaveProgress
import com.example.creditcardapp.ui.format.asCurrency
import com.example.creditcardapp.ui.format.expiry
import com.example.creditcardapp.ui.permission.RequestLocationPermission
import com.plaid.link.OpenPlaidLink
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardListScreen(
    onAddCard: () -> Unit,
    onViewTransactions: (Long) -> Unit,
    onOpenPlaidSetup: () -> Unit,
    onOpenRewardsMap: () -> Unit,
    viewModel: CardListViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val logo by viewModel.institutionLogo.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(enabled = expandedId != null) { expandedId = null }

    val listState = rememberLazyListState()
    LaunchedEffect(expandedId) {
        if (expandedId != null) {
            val index = cards.indexOfFirst { it.id == expandedId }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    RequestLocationPermission()

    val plaidLauncher = rememberLauncherForActivityResult(OpenPlaidLink()) { result ->
        when (result) {
            is LinkSuccess -> viewModel.onLinkSuccess(result.publicToken)
            is LinkExit -> viewModel.onLinkExit(result.error?.displayMessage)
        }
    }

    LaunchedEffect(event) {
        when (val e = event) {
            is BankLinkEvent.StartLink -> {
                plaidLauncher.launch(
                    LinkTokenConfiguration.Builder().token(e.linkToken).build()
                )
                viewModel.consumeEvent()
            }
            is BankLinkEvent.Message -> {
                snackbar.showSnackbar(e.text)
                viewModel.consumeEvent()
            }
            BankLinkEvent.OpenPlaidSetup -> {
                viewModel.consumeEvent()
                onOpenPlaidSetup()
            }
            null -> Unit
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Wallet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenRewardsMap) {
                        Icon(
                            Icons.Outlined.Map,
                            contentDescription = "Best card nearby",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { viewModel.connectBank() }) {
                        Icon(
                            Icons.Outlined.AccountBalance,
                            contentDescription = "Connect bank",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (cards.isNotEmpty() && expandedId == null) {
                ExtendedFloatingActionButton(
                    onClick = onAddCard,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add card") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedVisibility(
                visible = busy,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(220))
            ) {
                IndeterminateWave(modifier = Modifier.padding(horizontal = 20.dp))
            }

            if (cards.isEmpty()) {
                EmptyCardsState(
                    onConnectBank = { viewModel.connectBank() },
                    onAddCard = onAddCard
                )
                return@Column
            }

            PullToRefreshBox(
                isRefreshing = busy,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                val visibleCards = remember(cards, expandedId) {
                    if (expandedId == null) cards else cards.filter { it.id == expandedId }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(visibleCards, key = { it.id }) { card ->
                        val isExpanded = expandedId == card.id
                        CardListRow(
                            card = card,
                            logoBase64 = logo,
                            isExpanded = isExpanded,
                            onClick = {
                                expandedId = if (isExpanded) null else card.id
                            },
                            onViewTransactions = { onViewTransactions(card.id) },
                            onDelete = {
                                expandedId = null
                                viewModel.deleteCard(card.id)
                            },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(220),
                                placementSpec = spring(),
                                fadeOutSpec = tween(220)
                            )
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CardListRow(
    card: CreditCard,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onViewTransactions: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    logoBase64: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (isExpanded) 3.dp else 1.dp,
        shadowElevation = if (isExpanded) 2.dp else 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.clickable { onClick() }) {
                CreditCardTile(card = card, logoBase64 = logoBase64)
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Column {
                    card.nickname?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    val progress = remember(card.balance, card.creditLimit) {
                        if (card.creditLimit > 0.0)
                            (card.balance / card.creditLimit).coerceIn(0.0, 1.0).toFloat()
                        else 0f
                    }
                    Spacer(Modifier.height(8.dp))
                    WaveProgress(
                        progress = progress,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        height = 8.dp,
                        amplitude = 2.dp
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = card.balance.asCurrency(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "of ${card.creditLimit.asCurrency()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(360),
                expandFrom = Alignment.Top
            ) + fadeIn(tween(280)),
            exit = shrinkVertically(
                animationSpec = tween(240),
                shrinkTowards = Alignment.Top
            ) + fadeOut(tween(180))
        ) {
            ExpandedCardDetails(
                card = card,
                onViewTransactions = onViewTransactions,
                onDelete = onDelete
            )
        }
        }
    }
}

@Composable
private fun ExpandedCardDetails(
    card: CreditCard,
    onViewTransactions: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        DetailRow(label = "Cardholder", value = card.cardholderName.ifBlank { "—" })
        DetailRow(
            label = "Number",
            value = "•••• \u2002•••• \u2002•••• \u2002${card.last4}"
        )
        DetailRow(label = "Expires", value = expiry(card.expiryMonth, card.expiryYear))
        DetailRow(label = "Brand", value = card.brand)

        Spacer(Modifier.height(8.dp))
        val available = (card.creditLimit - card.balance).coerceAtLeast(0.0)
        Text(
            text = "AVAILABLE CREDIT",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = available.asCurrency(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onViewTransactions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (card.sourceAccountId != null) "View transactions" else "Transactions")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Delete card")
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}
