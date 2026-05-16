package com.example.creditcardapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.creditcardapp.ui.add.AddCardScreen
import com.example.creditcardapp.ui.home.HomeScreen
import com.example.creditcardapp.ui.plaidsetup.PlaidSetupScreen
import com.example.creditcardapp.ui.transactions.TransactionsScreen

private const val MOTION = 320

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(MOTION)) +
                fadeIn(tween(MOTION))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(MOTION)) +
                fadeOut(tween(MOTION))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(MOTION)) +
                fadeIn(tween(MOTION))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(MOTION)) +
                fadeOut(tween(MOTION))
        }
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                onAddCard = { navController.navigate(Destination.AddCard.route) },
                onViewTransactions = { id -> navController.navigate(Destination.Transactions.build(id)) },
                onOpenPlaidSetup = { navController.navigate(Destination.PlaidSetup.route) },
            )
        }
        composable(Destination.AddCard.route) {
            AddCardScreen(onDone = { navController.popBackStack() })
        }
        composable(Destination.PlaidSetup.route) {
            PlaidSetupScreen(onBack = { navController.popBackStack() })
        }
        composable(Destination.RewardsHub.route) {
            com.example.creditcardapp.ui.rewards.hub.RewardsHubScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Destination.Offers.route) {
            com.example.creditcardapp.ui.offers.OffersScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Destination.Transactions.route,
            arguments = listOf(navArgument(Destination.Transactions.ARG_CARD_ID) { type = NavType.LongType })
        ) {
            TransactionsScreen(onBack = { navController.popBackStack() })
        }
    }
}
