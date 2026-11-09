package org.mobile.growfy.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import org.mobile.growfy.core.ui.TestTags
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.core.ui.theme.GrowfyColors
import org.mobile.growfy.domain.model.Location

/** App-wide snackbar host so messages survive navigating between screens. */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

@Composable
fun LetterAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val (bg, fg) = GrowfyColors.AvatarPalettes[(name.firstOrNull()?.code ?: 0) % GrowfyColors.AvatarPalettes.size]
    Box(modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = fg, style = MaterialTheme.typography.titleMedium)
    }
}

/** Rounded square in the location's colour, with its initial. */
@Composable
fun LocationBadge(location: Location, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(12.dp)).background(GrowfyColors.location(location.id)),
        contentAlignment = Alignment.Center,
    ) {
        Text(location.title.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

/** The reviewer's Google photo when there is one, otherwise a coloured initial. */
@Composable
fun ReviewerAvatar(name: String, photoUrl: String?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(modifier.size(size)) {
        LetterAvatar(name, size = size)
        if (photoUrl != null) {
            coil3.compose.AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
    }
}

@Composable
fun LocationSwatch(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(3.dp)).background(color))
}

@Composable
fun StarRating(rating: Int, fontSize: TextUnit = 14.sp, letterSpacing: TextUnit = 1.sp) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = GrowfyColors.Star)) { append("★".repeat(rating)) }
            withStyle(SpanStyle(color = GrowfyColors.StarEmpty)) { append("★".repeat(5 - rating)) }
        },
        fontSize = fontSize,
        letterSpacing = letterSpacing,
    )
}

@Composable
fun StatusPill(text: String, container: Color, content: Color) {
    Text(
        text,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(container).padding(horizontal = 8.dp, vertical = 4.dp),
        color = content,
        style = MaterialTheme.typography.labelMedium,
    )
}

/** Stand-in for a post photo until real media URLs come from the API. */
@Composable
fun PhotoPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.linearGradient(GrowfyColors.PhotoGradient)))
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().testTag(TestTags.EMPTY).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFE0F2E8)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Check, contentDescription = null, tint = Color.White)
            }
        }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    }
}

/** Dark hint card used for one-time tips. */
@Composable
fun TipCard(text: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f).padding(vertical = 14.dp),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
        androidx.compose.material3.TextButton(onClick = onAction, modifier = Modifier.testTag(TestTags.REVIEWS_TIP_DISMISS)) {
            Text(action, color = MaterialTheme.colorScheme.inversePrimary)
        }
    }
}
