package org.mobile.growfy.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.mobile.growfy.feature.auth.LoginScreen
import org.mobile.growfy.feature.main.MainScreen
import org.mobile.growfy.feature.more.GoogleAccountsScreen
import org.mobile.growfy.feature.more.NotificationsScreen
import org.mobile.growfy.feature.more.TemplatesScreen
import org.mobile.growfy.feature.onboarding.OnboardingScreen
import org.mobile.growfy.feature.posts.ComposePostScreen
import org.mobile.growfy.feature.reviews.ReplyScreen

@Composable
fun AppNavHost(navController: NavHostController, startDestination: Route) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Login> {
            LoginScreen(onSignedIn = { navController.resetTo(it) })
        }
        composable<Route.Onboarding> {
            OnboardingScreen(onDone = { navController.resetTo(Route.Main) })
        }
        composable<Route.Main> {
            MainScreen(onNavigate = { route ->
                if (route == Route.Login) navController.resetTo(Route.Login) else navController.navigate(route)
            })
        }
        composable<Route.Reply> { entry ->
            val route = entry.toRoute<Route.Reply>()
            ReplyScreen(
                reviewId = route.reviewId,
                onBack = { navController.popBackStack() },
                onOpenReview = { next ->
                    navController.navigate(Route.Reply(next)) { popUpTo<Route.Reply> { inclusive = true } }
                },
            )
        }
        composable<Route.ComposePost> { entry ->
            ComposePostScreen(entry.toRoute<Route.ComposePost>(), onClose = { navController.popBackStack() })
        }
        composable<Route.Templates> { TemplatesScreen(onBack = { navController.popBackStack() }) }
        composable<Route.GoogleAccounts> { GoogleAccountsScreen(onBack = { navController.popBackStack() }) }
        composable<Route.Notifications> { NotificationsScreen(onBack = { navController.popBackStack() }) }
    }
}

/** Replaces the whole back stack, e.g. after sign-in or sign-out. */
fun NavHostController.resetTo(route: Route) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
