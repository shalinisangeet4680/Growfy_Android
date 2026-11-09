package org.mobile.growfy.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import org.mobile.growfy.core.ui.TestTags
import org.mobile.growfy.core.ui.UiError
import org.mobile.growfy.core.ui.icons.Illustrations

/** Centered spinner for a screen's first load. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().testTag(TestTags.LOADING), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Illustrated error with a retry button. Use [compact] inside a section of a page
 * (e.g. Home's "Needs reply" card) and the full size when the whole page failed.
 */
@Composable
fun ErrorState(error: UiError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(
        modifier.fillMaxWidth().testTag(TestTags.ERROR).padding(horizontal = 32.dp, vertical = if (compact) 16.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(Illustrations.forKind(error.kind), contentDescription = null, modifier = Modifier.size(if (compact) 96.dp else 160.dp))
        Text(error.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Text(
            error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (onRetry != null) {
            FilledTonalButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp).testTag(TestTags.ERROR_RETRY)) { Text("Try again") }
        }
    }
}

/**
 * Standard data screen: pull-to-refresh around [content], with a centered spinner on first load
 * and an [ErrorState] when nothing could be loaded. [content] must scroll (LazyColumn or verticalScroll)
 * for the pull gesture to reach the refresh indicator.
 */
@Composable
fun RefreshablePage(
    loading: Boolean,
    error: UiError?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = modifier.fillMaxSize()) {
        when {
            loading -> LoadingState()
            error != null -> FullPage { ErrorState(error, onRetry = onRefresh) }
            else -> content()
        }
    }
}

/** Scrollable (so pull-to-refresh works) but still fills and centers in the visible area. */
@Composable
fun FullPage(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val height = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) { content() }
        }
    }
}
