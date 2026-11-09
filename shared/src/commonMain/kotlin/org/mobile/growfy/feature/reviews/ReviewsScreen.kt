package org.mobile.growfy.feature.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.mobile.growfy.core.ui.components.EmptyState
import org.mobile.growfy.core.ui.components.FullPage
import org.mobile.growfy.core.ui.components.RefreshablePage
import org.mobile.growfy.core.ui.components.LocationFilterChip
import org.mobile.growfy.core.ui.components.ReviewerAvatar
import org.mobile.growfy.core.ui.components.StarRating
import org.mobile.growfy.core.ui.components.TipCard
import androidx.compose.ui.platform.testTag
import org.mobile.growfy.core.ui.TestTags
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.core.util.relativeTo
import org.mobile.growfy.domain.model.InboxFilter
import org.mobile.growfy.domain.model.Review
import org.mobile.growfy.navigation.Route
import kotlin.time.Instant

@Composable
fun ReviewsScreen(onNavigate: (Route) -> Unit, viewModel: ReviewsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showHint by viewModel.showSwipeHint.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        SearchField(state.query, viewModel::setQuery)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterTab("Waiting ${state.pending}", state.filter == InboxFilter.Pending, TestTags.REVIEWS_WAITING) { viewModel.setFilter(InboxFilter.Pending) }
            FilterTab("Replied ${state.replied}", state.filter == InboxFilter.Replied, TestTags.REVIEWS_REPLIED) { viewModel.setFilter(InboxFilter.Replied) }
            LocationFilterChip(state.locations, state.selectedLocationId, viewModel::selectLocation)
        }
        RefreshablePage(
            loading = state.loading && state.reviews.isEmpty(),
            error = state.error.takeIf { state.reviews.isEmpty() },
            refreshing = state.refreshing,
            onRefresh = { viewModel.refresh(pull = true) },
            modifier = Modifier.weight(1f),
        ) {
          Box(Modifier.fillMaxSize()) {
            val listState = rememberLazyListState()
            val nearEnd by remember {
                derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= listState.layoutInfo.totalItemsCount - 3 } == true }
            }
            LaunchedEffect(nearEnd) { if (nearEnd) viewModel.loadMore() }
            if (state.reviews.isEmpty()) {
                FullPage {
                    EmptyState(if (state.query.isNotBlank()) "No matches" else if (state.filter == InboxFilter.Pending) "All caught up" else "No replies yet")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    itemsIndexed(state.reviews, key = { _, it -> it.id }) { index, review ->
                        ReviewRow(review, state.now, Modifier.testTag(TestTags.reviewRow(index)), onOpen = { onNavigate(Route.Reply(review.id)) }, onSend = { viewModel.sendDraft(review) })
                    }
                    if (state.loadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
            if (showHint && state.filter == InboxFilter.Pending && state.reviews.any { it.draft != null }) {
                TipCard(
                    "Swipe left to send a draft",
                    "Got it",
                    onAction = viewModel::dismissSwipeHint,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )
            }
          }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(48.dp)
            .clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(AppIcons.Search, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search reviews", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().testTag(TestTags.REVIEWS_SEARCH),
            )
        }
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    FilterChip(
        modifier = Modifier.testTag(tag),
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) ({ Icon(AppIcons.Check, null, Modifier.size(18.dp)) }) else null,
    )
}

@Composable
private fun ReviewRow(review: Review, now: Instant, modifier: Modifier, onOpen: () -> Unit, onSend: suspend () -> Boolean) {
    val swipe = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val canSwipe = !review.isAnswered && review.draft != null
    SwipeToDismissBox(
        state = swipe,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = canSwipe,
        onDismiss = { scope.launch { if (!onSend()) swipe.reset() } },
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.CenterEnd) {
                Column(Modifier.width(104.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(AppIcons.Send, null, tint = Color.White)
                    Text("Send", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 88.dp).background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReviewerAvatar(review.authorName, review.authorPhoto)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row {
                    Text(
                        review.authorName,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (review.isAnswered) FontWeight.Normal else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(review.reviewedAt.relativeTo(now), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StarRating(review.stars, fontSize = 12.sp)
                Text(
                    if (review.isAnswered) "↩ ${review.replyText}" else review.text.ifBlank { "No written review" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
