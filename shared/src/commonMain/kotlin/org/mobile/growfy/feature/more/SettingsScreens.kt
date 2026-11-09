package org.mobile.growfy.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mobile.growfy.core.ui.UiError
import org.mobile.growfy.core.ui.components.LocalSnackbarHostState
import org.mobile.growfy.core.ui.components.RefreshablePage
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.domain.model.GoogleConnection

// Not in the mobile design file; layout follows the TreeVook Android prototype's sub-pages.

@Composable
private fun SubScreen(
    title: String,
    onBack: () -> Unit,
    loading: Boolean,
    error: UiError?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons.Back, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        RefreshablePage(loading, error, refreshing, onRefresh, Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { content() }
        }
    }
}

@Composable
fun TemplatesScreen(onBack: () -> Unit, viewModel: TemplatesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SubScreen(
        "Reply templates", onBack,
        loading = state.loading && state.templates.isEmpty(),
        error = state.error.takeIf { state.templates.isEmpty() },
        refreshing = state.refreshing,
        onRefresh = { viewModel.load(pull = true) },
    ) {
        if (state.templates.isNotEmpty()) {
            Group {
                state.templates.forEachIndexed { i, template ->
                    if (i > 0) Divider()
                    Row(
                        Modifier.padding(16.dp).alpha(if (template.hidden) 0.55f else 1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(template.name, style = MaterialTheme.typography.bodyLarge)
                            Text(ratingsLabel(template.ratings), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            if (template.preview.isNotBlank()) {
                                Text(template.preview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                            }
                        }
                        Switch(checked = !template.hidden, onCheckedChange = { viewModel.setEnabled(template, it) })
                    }
                }
            }
        }
    }
}

private fun ratingsLabel(ratings: List<Int>): String = when {
    ratings.isEmpty() -> "Any rating"
    ratings.size == 1 -> "For ${ratings.first()} star reviews"
    else -> "For ${ratings.min()}–${ratings.max()} star reviews"
}

@Composable
fun GoogleAccountsScreen(onBack: () -> Unit, viewModel: GoogleAccountsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    var confirm by remember { mutableStateOf<GoogleConnection?>(null) }
    SubScreen(
        "Google accounts", onBack,
        loading = state.loading && state.connections.isEmpty(),
        error = state.error.takeIf { state.connections.isEmpty() },
        refreshing = state.refreshing,
        onRefresh = { viewModel.refresh(pull = true) },
    ) {
        if (state.connections.isNotEmpty()) {
            Group {
                state.connections.forEachIndexed { i, connection ->
                    if (i > 0) Divider()
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(AppIcons.Google, null, Modifier.size(24.dp), tint = androidx.compose.ui.graphics.Color.Unspecified)
                        Column(Modifier.weight(1f)) {
                            Text(connection.email, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                connection.error ?: "${connection.locationCount} locations",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (connection.isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            )
                        }
                        TextButton(onClick = { confirm = connection }) { Text("Disconnect", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        OutlinedButton(onClick = viewModel::addAccount, enabled = !state.connecting, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.connecting) "Waiting for Google…" else "+ Add another Google account")
        }
    }
    confirm?.let { connection ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Disconnect ${connection.email}?") },
            text = { Text("TreeVook stops syncing its ${connection.locationCount} locations. Nothing is removed from Google.") },
            confirmButton = {
                TextButton(onClick = { viewModel.disconnect(connection); confirm = null }) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit, viewModel: NotificationsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SubScreen(
        "Notifications", onBack,
        loading = state.loading && state.prefs == null,
        error = state.error.takeIf { state.prefs == null },
        refreshing = state.refreshing,
        onRefresh = { viewModel.load(pull = true) },
    ) {
        state.prefs?.let { prefs ->
            Group {
                ToggleRow("New reviews", "As soon as one arrives", prefs.newReviews) { on -> viewModel.update { it.copy(newReviews = on) } }
                Divider()
                ToggleRow("Low ratings", "1–3 stars, right away", prefs.lowRatings) { on -> viewModel.update { it.copy(lowRatings = on) } }
                Divider()
                ToggleRow("Weekly summary", "Every Monday", prefs.weeklySummary) { on -> viewModel.update { it.copy(weeklySummary = on) } }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
