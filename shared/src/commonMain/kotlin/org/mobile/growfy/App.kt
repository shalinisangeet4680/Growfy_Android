package org.mobile.growfy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.core.ui.theme.GrowfyColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject
import org.mobile.growfy.core.network.SessionEvents
import org.mobile.growfy.core.ui.Messenger
import org.mobile.growfy.core.ui.components.LocalSnackbarHostState
import org.mobile.growfy.core.ui.theme.GrowfyTheme
import kotlinx.coroutines.flow.first
import org.mobile.growfy.core.platform.DeepLinks
import org.mobile.growfy.domain.repository.AuthRepository
import org.mobile.growfy.navigation.AppLinkParser
import org.mobile.growfy.navigation.AppNavHost
import org.mobile.growfy.navigation.Tab
import org.mobile.growfy.navigation.TabRequests
import org.mobile.growfy.navigation.Route
import org.mobile.growfy.navigation.SessionRouter
import org.mobile.growfy.navigation.resetTo

@Composable
fun App() {
    GrowfyTheme {
        val router = koinInject<SessionRouter>()
        val messenger = koinInject<Messenger>()
        val sessionEvents = koinInject<SessionEvents>()
        val deepLinks = koinInject<DeepLinks>()
        val auth = koinInject<AuthRepository>()
        val tabRequests = koinInject<TabRequests>()
        val snackbar = remember { SnackbarHostState() }
        val navController = rememberNavController()
        var start by remember { mutableStateOf<Route?>(null) }

        LaunchedEffect(Unit) { start = router.destination() }
        LaunchedEffect(messenger) {
            messenger.messages.collect { message ->
                val result = snackbar.showSnackbar(message.text, message.actionLabel, duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
            }
        }
        LaunchedEffect(start) {
            if (start == null) return@LaunchedEffect
            deepLinks.links.collect { link ->
                val target = AppLinkParser.parse(link) ?: return@collect
                deepLinks.consumed()
                if (!auth.isSignedIn.first()) return@collect
                when (target) {
                    is Tab -> {
                        tabRequests.request(target)
                        navController.navigate(Route.Main) { popUpTo<Route.Main> { inclusive = false }; launchSingleTop = true }
                    }
                    is Route -> navController.navigate(target)
                }
            }
        }
        LaunchedEffect(start) {
            if (start == null) return@LaunchedEffect
            sessionEvents.expired.collect {
                messenger.show("Your session ended. Sign in again.")
                navController.resetTo(Route.Login)
            }
        }

        CompositionLocalProvider(LocalSnackbarHostState provides snackbar) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                start?.let { AppNavHost(navController, it) } ?: Splash()
            }
        }
    }
}

/** Shown while the start route is resolved (stored session + Google connection check). */
@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(76.dp).clip(RoundedCornerShape(22.dp)).background(GrowfyColors.Ink), contentAlignment = Alignment.Center) {
            Icon(AppIcons.Logo, null, Modifier.size(30.dp), tint = Color.White)
        }
    }
}
