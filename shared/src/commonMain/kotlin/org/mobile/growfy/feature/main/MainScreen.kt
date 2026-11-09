package org.mobile.growfy.feature.main

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.testTag
import org.mobile.growfy.core.ui.TestTags
import org.mobile.growfy.navigation.TabRequests
import org.mobile.growfy.core.network.DataChanges
import org.mobile.growfy.core.network.DataKind
import org.mobile.growfy.core.network.getOrNull
import org.mobile.growfy.core.ui.components.LocalSnackbarHostState
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.domain.model.InboxFilter
import org.mobile.growfy.domain.repository.ReviewRepository
import org.mobile.growfy.feature.home.HomeScreen
import org.mobile.growfy.feature.more.MoreScreen
import org.mobile.growfy.feature.posts.PostsScreen
import org.mobile.growfy.feature.reviews.ReviewsScreen
import org.mobile.growfy.navigation.Route
import org.mobile.growfy.navigation.Tab

class MainViewModel(private val reviews: ReviewRepository, changes: DataChanges) : ViewModel() {
    private val _waiting = MutableStateFlow(0)
    val waiting: StateFlow<Int> = _waiting.asStateFlow()

    init {
        viewModelScope.launch { changes.of(DataKind.Reviews).collect { refresh() } }
    }

    fun refresh() {
        viewModelScope.launch {
            reviews.inbox(InboxFilter.Pending, locationId = null).getOrNull()?.let { _waiting.value = it.pending }
        }
    }
}

private data class TabItem(val tab: Tab, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    TabItem(Tab.Home, "Home", AppIcons.Home, AppIcons.HomeFilled),
    TabItem(Tab.Reviews, "Reviews", AppIcons.Reviews, AppIcons.ReviewsFilled),
    TabItem(Tab.Posts, "Posts", AppIcons.Posts, AppIcons.PostsFilled),
    TabItem(Tab.More, "More", AppIcons.More, AppIcons.MoreFilled),
)

@Composable
fun MainScreen(onNavigate: (Route) -> Unit, viewModel: MainViewModel = koinViewModel()) {
    val nav = rememberNavController()
    val waiting by viewModel.waiting.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val backStack by nav.currentBackStackEntryAsState()
    val tabRequests = koinInject<TabRequests>()
    LaunchedEffect(tabRequests) {
        tabRequests.tabs.collect { tab ->
            nav.switchTo(tab)
            tabRequests.consumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    val selected = backStack?.destination?.hierarchy?.any { it.hasRoute(item.tab::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { nav.switchTo(item.tab) },
                        modifier = Modifier.testTag(TestTags.tab(item.label.lowercase())),
                        icon = {
                            BadgedBox(badge = { if (item.tab == Tab.Reviews && waiting > 0) Badge { Text("$waiting") } }) {
                                Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null)
                            }
                        },
                        label = { Text(item.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.Home, modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable<Tab.Home> { HomeScreen(onNavigate = onNavigate, onTab = nav::switchTo) }
            composable<Tab.Reviews> { ReviewsScreen(onNavigate = onNavigate) }
            composable<Tab.Posts> { PostsScreen(onNavigate = onNavigate) }
            composable<Tab.More> { MoreScreen(onNavigate = onNavigate) }
        }
    }
}

private fun NavHostController.switchTo(tab: Tab) {
    navigate(tab) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
