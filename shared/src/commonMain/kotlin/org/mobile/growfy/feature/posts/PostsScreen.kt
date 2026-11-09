package org.mobile.growfy.feature.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.mobile.growfy.core.ui.components.EmptyState
import org.mobile.growfy.core.ui.components.RefreshablePage
import org.mobile.growfy.core.ui.components.PhotoPlaceholder
import org.mobile.growfy.core.ui.components.StatusPill
import androidx.compose.ui.platform.testTag
import org.mobile.growfy.core.ui.TestTags
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.core.ui.theme.GrowfyColors
import org.mobile.growfy.core.util.longDay
import org.mobile.growfy.core.util.longName
import org.mobile.growfy.core.util.monthDay
import org.mobile.growfy.core.util.shortName
import org.mobile.growfy.core.util.time24
import org.mobile.growfy.domain.model.Post
import org.mobile.growfy.domain.model.PostStatus
import org.mobile.growfy.navigation.Route
import kotlin.time.Instant

@Composable
fun PostsScreen(onNavigate: (Route) -> Unit, viewModel: PostsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    var picking by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(state.selectedDay.month.longName, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { picking = true }, modifier = Modifier.testTag(TestTags.POSTS_CALENDAR)) {
                    Icon(AppIcons.Calendar, "Pick a date", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            WeekStrip(state, viewModel::pickDay)
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PostFilter.entries.forEach { f ->
                    val selected = state.filter == f
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setFilter(f) },
                        modifier = Modifier.testTag(TestTags.postFilter(f.name.lowercase())),
                        label = { Text(f.label) },
                        leadingIcon = if (selected) ({ Icon(AppIcons.Check, null, Modifier.size(18.dp)) }) else null,
                    )
                }
            }
            RefreshablePage(
                loading = state.loading && state.posts.isEmpty(),
                error = state.error.takeIf { state.posts.isEmpty() },
                refreshing = state.refreshing,
                onRefresh = { viewModel.refresh(pull = true) },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.dayFiltered) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(state.selectedDay.longDay(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = viewModel::clearDay) { Text("Show all") }
                                }
                            }
                        }
                        itemsIndexed(state.visible, key = { _, it -> it.id }) { index, post ->
                            PostCard(post, state.locationName(post.locationId), Modifier.testTag(TestTags.postCard(index))) { onNavigate(Route.ComposePost(postId = post.id)) }
                        }
                        if (state.visible.isEmpty()) {
                            item {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    EmptyState(if (state.dayFiltered) "Nothing planned this day" else "No posts here yet")
                                    Button(onClick = { onNavigate(newPostRoute(state)) }) { Text("Plan a post") }
                                }
                            }
                        }
                    }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onNavigate(newPostRoute(state)) },
            icon = { Icon(AppIcons.Edit, null) },
            text = { Text("New post") },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(TestTags.POSTS_NEW),
        )
    }

    if (picking) {
        val tz = TimeZone.UTC
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.selectedDay.atStartOfDayIn(tz).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.jumpTo(Instant.fromEpochMilliseconds(it).toLocalDateTime(tz).date) }
                    picking = false
                }) { Text("Go") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { DatePicker(pickerState) }
    }
}

/** With a day picked in the strip, a new post starts scheduled for 10:00 that day. */
private fun newPostRoute(state: PostsUiState): Route.ComposePost =
    if (state.dayFiltered && state.selectedDay > state.today) {
        Route.ComposePost(scheduleAt = state.selectedDay.atTime(LocalTime(10, 0)).toInstant(TimeZone.currentSystemDefault()).toString())
    } else {
        Route.ComposePost()
    }

@Composable
private fun WeekStrip(state: PostsUiState, onPick: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        state.days.forEach { day ->
            val selected = day == state.selectedDay
            val dayPosts = state.postsOn(day)
            Box(
                Modifier.weight(1f).height(68.dp).clip(RoundedCornerShape(20.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onPick(day) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        day.dayOfWeek.shortName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${day.day}",
                        fontSize = 18.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (dayPosts.isNotEmpty() && !selected) {
                    val live = dayPosts.all { it.status == PostStatus.Published }
                    Box(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).size(6.dp).clip(CircleShape)
                            .background(if (live) MaterialTheme.colorScheme.primary else GrowfyColors.Pending),
                    )
                }
            }
        }
    }
}

@Composable
private fun PostCard(post: Post, locationName: String?, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val local = post.date.toLocalDateTime(TimeZone.currentSystemDefault())
    val pill: @Composable () -> Unit = {
        when (post.status) {
            PostStatus.Scheduled -> StatusPill(
                "Scheduled · ${local.date.dayOfWeek.shortName} ${local.time24()}",
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
            )
            PostStatus.Published -> StatusPill("Live", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            PostStatus.Draft -> StatusPill("Draft", GrowfyColors.DraftContainer, MaterialTheme.colorScheme.onSurfaceVariant)
            PostStatus.Failed -> StatusPill("Failed", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        }
    }
    val meta = listOfNotNull(post.content.type.label, locationName.takeIf { post.status != PostStatus.Published }, local.date.monthDay().takeIf { post.status == PostStatus.Published })
        .joinToString(" · ")
    val mediaUrl = post.content.mediaUrl
    Column(
        modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape).clickable(onClick = onClick),
    ) {
        if (mediaUrl != null) {
            Box(Modifier.fillMaxWidth().height(120.dp)) {
                PhotoPlaceholder(Modifier.matchParentSize())
                AsyncImage(mediaUrl, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pill()
                    Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(post.content.summary, style = MaterialTheme.typography.bodyLarge)
                post.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        } else {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(post.content.type.label.take(1), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(post.content.summary, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                    Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    post.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
                pill()
            }
        }
    }
}
