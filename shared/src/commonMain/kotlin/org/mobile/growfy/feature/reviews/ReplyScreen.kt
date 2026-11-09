package org.mobile.growfy.feature.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mobile.growfy.core.ui.components.LocalSnackbarHostState
import org.mobile.growfy.core.ui.components.RefreshablePage
import org.mobile.growfy.core.ui.components.StarRating
import androidx.compose.ui.platform.testTag
import org.mobile.growfy.core.ui.TestTags
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.core.util.relativeTo
import org.mobile.growfy.domain.model.ReplyTone

@Composable
fun ReplyScreen(
    reviewId: String,
    onBack: () -> Unit,
    onOpenReview: (String) -> Unit,
    viewModel: ReplyViewModel = koinViewModel(key = reviewId) { parametersOf(reviewId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.navigation) {
        when (val nav = state.navigation) {
            is ReplyNavigation.Next -> onOpenReview(nav.reviewId)
            ReplyNavigation.Close -> onBack()
            null -> return@LaunchedEffect
        }
        viewModel.navigationHandled()
    }
    val review = state.review

    Scaffold(
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
        topBar = {
            TopAppBar(
                title = { Text(review?.authorName.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons.Back, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (review != null && !review.isAnswered) {
                Row(
                    Modifier.navigationBarsPadding().imePadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = viewModel::skip,
                        modifier = Modifier.height(56.dp).testTag(TestTags.REPLY_SKIP),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) { Text("Skip", style = MaterialTheme.typography.titleMedium) }
                    Button(
                        onClick = viewModel::send,
                        enabled = state.text.isNotBlank() && !state.sending,
                        modifier = Modifier.weight(1f).height(56.dp).testTag(TestTags.REPLY_SEND),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    ) {
                        Icon(AppIcons.Send, null, Modifier.size(20.dp), tint = Color.White)
                        Text("  Send reply", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
    ) { padding ->
        RefreshablePage(
            loading = state.loading,
            error = state.error.takeIf { review == null },
            refreshing = state.refreshing,
            onRefresh = { viewModel.load(pull = true) },
            modifier = Modifier.padding(padding),
        ) {
            if (review != null) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StarRating(review.stars, fontSize = 18.sp, letterSpacing = 2.sp)
                        Text(
                            listOfNotNull(state.locationName, "${review.reviewedAt.relativeTo(state.now)} ago".replace("now ago", "just now")).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(review.text.ifBlank { "No written review" }, fontSize = 22.sp, lineHeight = 30.sp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (review.isAnswered) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                if (review.repliedAutomatically) "Replied automatically" else "You replied",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(review.replyText.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReplyTone.entries.forEach { tone ->
                                val selected = state.tone == tone
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.pickTone(tone) },
                                    modifier = Modifier.testTag(TestTags.replyTone(tone.api)),
                                    enabled = !state.rewriting,
                                    label = { Text(tone.label) },
                                    leadingIcon = if (selected) ({ Icon(AppIcons.Check, null, Modifier.size(18.dp)) }) else null,
                                )
                            }
                            if (state.rewriting) CircularProgressIndicator(Modifier.size(20.dp).align(Alignment.CenterVertically), strokeWidth = 2.dp)
                        }
                        Column {
                            OutlinedTextField(
                                value = state.text,
                                onValueChange = viewModel::setText,
                                label = { Text("Your reply") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth().testTag(TestTags.REPLY_TEXT),
                            )
                            state.templateName?.let {
                                Text(
                                    "From template “$it”",
                                    Modifier.padding(start = 16.dp, top = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

