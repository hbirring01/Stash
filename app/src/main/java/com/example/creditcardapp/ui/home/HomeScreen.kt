package com.example.creditcardapp.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.creditcardapp.ui.list.CardListScreen
import com.example.creditcardapp.ui.rewards.RewardsMapScreen
import com.example.creditcardapp.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

private data class HomeTab(
    val label: String,
    val icon: ImageVector,
)

private val HomeTabs = listOf(
    HomeTab("Wallet", Icons.Outlined.CreditCard),
    HomeTab("Map", Icons.Outlined.Map),
    HomeTab("Settings", Icons.Outlined.Settings),
)

/**
 * The root, swipe-able home of the app: Wallet ↔ Map ↔ Settings.
 *
 * Sub-screens (Add Card, Transactions, Plaid Setup) are still routed through
 * the NavController push/pop — those callbacks bubble up to the parent nav
 * graph. Horizontal swipe between the three top-level pages is provided by
 * [HorizontalPager]; the bottom [NavigationBar] is an explicit secondary
 * affordance.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAddCard: () -> Unit,
    onViewTransactions: (Long) -> Unit,
    onOpenPlaidSetup: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { HomeTabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                HomeTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // beyondBoundsPageCount = 1, // keep adjacent page composed for smoother swipe
        ) { page ->
            when (page) {
                0 -> CardListScreen(
                    onAddCard = onAddCard,
                    onViewTransactions = onViewTransactions,
                    onOpenPlaidSetup = onOpenPlaidSetup,
                    onOpenRewardsMap = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                )
                1 -> RewardsMapScreen(
                    isActive = pagerState.currentPage == 1,
                    onBack = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                )
                2 -> SettingsScreen(
                    onOpenPlaidSetup = onOpenPlaidSetup,
                )
            }
        }
    }
}
