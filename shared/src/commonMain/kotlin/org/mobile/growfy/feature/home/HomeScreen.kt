package org.mobile.growfy.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mobile.growfy.core.ui.components.EmptyState
import org.mobile.growfy.core.ui.components.ErrorState
import org.mobile.growfy.core.ui.components.RefreshablePage
import org.mobile.growfy.core.ui.components.LocationPill
import org.mobile.growfy.core.ui.components.ReviewerAvatar
import org.mobile.growfy.core.ui.components.StarRating
import androidx.compose.ui.platform.testTag
import org.mobile.growfy.core.ui.TestTags
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.core.ui.theme.GrowfyColors
import org.mobile.growfy.core.util.relativeTo
import org.mobile.growfy.domain.model.HomeSummary
import org.mobile.growfy.domain.model.Review
import org.mobile.growfy.domain.model.Stat
import org.mobile.growfy.navigation.Route
import org.mobile.growfy.navigation.Tab

@Composable
fun HomeScreen(onNavigate: (Route) -> Unit, onTab: (Tab) -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LocationPill(state.locations, state.selectedLocationId, viewModel::selectLocation)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onTab(Tab.Reviews) }) {
                Icon(AppIcons.Search, "Search reviews", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onTab(Tab.More) },
                contentAlignment = Alignment.Center,
            ) {
                Text(state.me?.initials.orEmpty(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        RefreshablePage(
            loading = state.loading && !state.hasData,
            error = state.error.takeIf { !state.hasData },
            refreshing = state.refreshing,
            onRefresh = { viewModel.refresh(pull = true) },
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.summary?.let { summary ->
                    summary.nextStep?.let { next ->
                        item {
                            SetupCard(summary, onContinue = {
                                when (next.key) {
                                    "templates" -> onNavigate(Route.Templates)
                                    "first_reply" -> onTab(Tab.Reviews)
                                    else -> onNavigate(Route.Onboarding)
                                }
                            })
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Needs reply", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { onTab(Tab.Reviews) }) {
                            Text(if (state.waiting > 0) "See all ${state.waiting}" else "Open inbox")
                        }
                    }
                }
                item {
                    val focus = state.focus
                    when {
                        focus != null -> FocusReview(
                            review = focus,
                            meta = listOfNotNull(state.locationName(focus.locationId), focus.reviewedAt.relativeTo(state.now)).joinToString(" · "),
                            sending = state.sending,
                            onEdit = { onNavigate(Route.Reply(focus.id)) },
                            onSend = viewModel::sendFocus,
                        )
                        state.error != null -> ErrorState(state.error!!, onRetry = { viewModel.refresh(pull = true) }, compact = true)
                        !state.loading -> EmptyState("All caught up")
                    }
                }
                state.summary?.let { summary ->
                    item { HealthCard(summary, onClick = { onTab(if (state.waiting > 0) Tab.Reviews else Tab.Posts) }) }
                }
                state.stats.chunked(2).forEach { row ->
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { StatTile(it, Modifier.weight(1f)) }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupCard(summary: HomeSummary, onContinue: () -> Unit) {
    val done = summary.setup.count { it.done }
    val total = summary.setup.size
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Getting started · $done of $total",
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Continue",
                Modifier.clickable(onClick = onContinue),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
        summary.nextStep?.let {
            Text("Next: ${it.label.replaceFirstChar(Char::lowercase)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun FocusReview(review: Review, meta: String, sending: Boolean, onEdit: () -> Unit, onSend: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReviewerAvatar(review.authorName, review.authorPhoto)
            Column(Modifier.weight(1f)) {
                Text(review.authorName, style = MaterialTheme.typography.titleMedium)
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            StarRating(review.stars)
        }
        if (review.text.isNotBlank()) Text(review.text, style = MaterialTheme.typography.bodyLarge)
        review.draft?.let {
            Text(
                it.text,
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            OutlinedButton(onClick = onEdit, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.testTag(TestTags.HOME_FOCUS_EDIT)) {
                Text(if (review.draft == null) "Write reply" else "Edit")
            }
            if (review.draft != null) Button(onClick = onSend, enabled = !sending, modifier = Modifier.testTag(TestTags.HOME_FOCUS_SEND)) { Text("Send") }
        }
    }
}

@Composable
private fun HealthCard(summary: HomeSummary, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(56.dp)) {
                val stroke = 5.dp.toPx()
                val inset = stroke / 2
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(GrowfyColors.StarEmpty, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                drawArc(primary, -90f, 360f * summary.healthScore / 100f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            }
            Text("${summary.healthScore}", fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Profile health", style = MaterialTheme.typography.titleMedium)
            Text(summary.healthNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(AppIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatTile(stat: Stat, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stat.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            buildAnnotatedString {
                append("${stat.value} ")
                stat.changePercent?.let { change ->
                    withStyle(
                        SpanStyle(
                            fontSize = 12.sp,
                            color = if (change >= 0) MaterialTheme.colorScheme.primary else GrowfyColors.Negative,
                        ),
                    ) { append(if (change >= 0) "+$change%" else "−${-change}%") }
                }
            },
            fontSize = 24.sp,
        )
    }
}
