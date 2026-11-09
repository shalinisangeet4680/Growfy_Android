package org.mobile.growfy.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mobile.growfy.core.ui.components.LocationBadge
import org.mobile.growfy.core.ui.components.RefreshablePage
import androidx.compose.ui.platform.testTag
import org.mobile.growfy.core.ui.TestTags
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.domain.model.Location
import org.mobile.growfy.domain.model.ReplyMode
import org.mobile.growfy.navigation.Route

@Composable
fun MoreScreen(onNavigate: (Route) -> Unit, viewModel: MoreViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.signedOut) { if (state.signedOut) onNavigate(Route.Login) }

    RefreshablePage(
        loading = state.loading && !state.hasData,
        error = state.error.takeIf { !state.hasData },
        refreshing = state.refreshing,
        onRefresh = { viewModel.refresh(pull = true) },
    ) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(state.me?.initials.orEmpty(), fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(state.me?.workspaceName.orEmpty(), fontSize = 20.sp)
                val email = state.googleEmail ?: state.me?.email ?: state.me?.phone
                if (email != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.googleEmail != null) Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (state.locations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Locations", Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Group {
                    state.locations.forEachIndexed { i, location ->
                        if (i > 0) Divider()
                        LocationCard(location, i) { viewModel.setReplyMode(location, it) }
                    }
                }
            }
        }

        Group {
            val enabled = state.templates.count { !it.hidden }
            NavRow(AppIcons.Star, "Reply templates", if (state.templates.isEmpty()) "" else "$enabled of ${state.templates.size} on", TestTags.MORE_TEMPLATES) { onNavigate(Route.Templates) }
            Divider()
            NavRow(AppIcons.Google, "Google accounts", "${state.connections} connected", TestTags.MORE_ACCOUNTS, tint = Color.Unspecified) { onNavigate(Route.GoogleAccounts) }
            Divider()
            NavRow(AppIcons.Bell, "Notifications", notificationsLabel(state), TestTags.MORE_NOTIFICATIONS) { onNavigate(Route.Notifications) }
        }
        Group {
            NavRow(AppIcons.Back, "Sign out", "", TestTags.MORE_SIGN_OUT, chevron = false, onClick = viewModel::signOut)
        }
    }
    }
}

private fun notificationsLabel(state: MoreUiState): String {
    val p = state.notifications ?: return "New reviews, low ratings"
    return listOfNotNull("New reviews".takeIf { p.newReviews }, "low ratings".takeIf { p.lowRatings }, "weekly summary".takeIf { p.weeklySummary })
        .joinToString(", ").replaceFirstChar(Char::uppercase).ifEmpty { "Off" }
}

@Composable
private fun LocationCard(location: Location, index: Int, onMode: (ReplyMode) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LocationBadge(location)
            Column(Modifier.weight(1f)) {
                Text(location.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val rating = location.rating?.let { "${(it * 10).toInt() / 10.0} ★" }
                val sub = if (location.needsReply > 0) "${location.needsReply} waiting" else "all replied"
                Text(listOfNotNull(rating, sub).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Mixed/Manual locations show neither segment selected; picking one applies it to every star rating.
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(36.dp)) {
            listOf(ReplyMode.Auto to "Auto", ReplyMode.Approval to "Approve").forEachIndexed { i, (mode, label) ->
                SegmentedButton(
                    selected = location.mode == mode,
                    onClick = { if (location.mode != mode) onMode(mode) },
                    modifier = Modifier.testTag(TestTags.locationMode(index, mode.api)),
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(label) }
            }
        }
    }
}

@Composable
internal fun Group(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)) { content() }
}

@Composable
internal fun Divider() = HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    chevron: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).testTag(tag).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = tint)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (chevron) Icon(AppIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
