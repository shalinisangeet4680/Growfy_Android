package org.mobile.growfy.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.core.ui.theme.GrowfyColors
import org.mobile.growfy.domain.model.ReplyMode

@Composable
fun OnboardingScreen(onDone: () -> Unit, viewModel: OnboardingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 28.dp),
    ) {
        StepRail(state.step.ordinal)
        AnimatedContent(state.step, modifier = Modifier.weight(1f)) { step ->
            Column(Modifier.fillMaxSize()) {
                when (step) {
                    OnboardingStep.Connect -> ConnectStep(state, viewModel::connect)
                    OnboardingStep.PickLocations -> PickLocationsStep(state, viewModel::toggle, viewModel::back, viewModel::next)
                    OnboardingStep.ReplyMode -> ReplyModeStep(state, viewModel::pickMode, viewModel::back, viewModel::finish)
                }
            }
        }
    }
}

@Composable
private fun StepRail(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            Box(
                Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun ColumnScope.ConnectStep(state: OnboardingUiState, onConnect: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Spacer(Modifier.height(48.dp))
    ConnectIllustration()
    Spacer(Modifier.height(36.dp))
    Title("Connect Google", "We’ll find every location you manage.")
    state.error?.let { ErrorText(it) }
    Spacer(Modifier.weight(1f))
    Button(
        onClick = onConnect,
        enabled = !state.connecting,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(AppIcons.Google, null, Modifier.size(15.dp), tint = Color.Unspecified)
        }
        Spacer(Modifier.width(10.dp))
        Text(if (state.connecting) "Connecting…" else "Continue with Google", style = MaterialTheme.typography.titleMedium)
    }
    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = { uriHandler.openUri("https://business.google.com/create") },
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        Text("Not on Google yet? Create a profile")
    }
}

@Composable
private fun ConnectIllustration() {
    val dot = Color(0xFFD3DDD6)
    Box(
        Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val step = 14.dp.toPx()
            var y = step / 2
            while (y < size.height) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(dot, radius = 1.dp.toPx(), center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile(GrowfyColors.Ink) { Icon(AppIcons.Logo, null, Modifier.size(30.dp), tint = Color.White) }
            val green = MaterialTheme.colorScheme.primary
            Canvas(Modifier.width(80.dp).height(10.dp)) {
                drawLine(
                    Color(0xFFA9B5AD),
                    Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                )
                drawCircle(green, radius = 5.dp.toPx(), center = Offset(size.width / 2, size.height / 2))
            }
            Tile(Color.White) { Icon(AppIcons.Google, null, Modifier.size(32.dp), tint = Color.Unspecified) }
        }
    }
}

@Composable
private fun Tile(color: Color, content: @Composable () -> Unit) {
    Box(
        Modifier.size(76.dp).shadow(10.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(color),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ColumnScope.PickLocationsStep(
    state: OnboardingUiState,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Spacer(Modifier.height(40.dp))
    Title("Pick locations", "Found ${state.locations.size} on your account.")
    Spacer(Modifier.height(24.dp))
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.locations.forEach { location ->
            val on = location.id in state.selected
            ChoiceCard(on, onClick = { onToggle(location.id) }) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(GrowfyColors.location(location.id)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(location.title.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text(location.title, style = MaterialTheme.typography.bodyLarge)
                    state.categories[location.id]?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Checkbox(checked = on, onCheckedChange = { onToggle(location.id) })
            }
        }
    }
    state.error?.let { ErrorText(it) }
    Footer(onBack) {
        Button(onClick = onNext, enabled = state.selected.isNotEmpty(), modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(28.dp)) {
            Text(if (state.selected.isEmpty()) "Pick at least one" else "Continue with ${state.selected.size}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ColumnScope.ReplyModeStep(
    state: OnboardingUiState,
    onPick: (ReplyMode) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    Spacer(Modifier.height(40.dp))
    Title("How should replies go?", "Change it anytime.")
    Spacer(Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            Triple(ReplyMode.Approval, "Approve first", "You OK each reply"),
            Triple(ReplyMode.Auto, "Auto", "Replies post on their own"),
        ).forEach { (mode, title, sub) ->
            ChoiceCard(state.mode == mode, onClick = { onPick(mode) }, verticalPadding = 20.dp) {
                RadioButton(selected = state.mode == mode, onClick = { onPick(mode) })
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    state.error?.let { ErrorText(it) }
    Spacer(Modifier.weight(1f))
    Footer(onBack) {
        Button(onClick = onFinish, enabled = !state.finishing, modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(28.dp)) {
            if (state.finishing) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Finish", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ChoiceCard(
    selected: Boolean,
    onClick: () -> Unit,
    verticalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
            .background(if (selected) GrowfyColors.SelectedCard else MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun Footer(onBack: () -> Unit, primary: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxHeight(),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) { Text("Back", style = MaterialTheme.typography.titleMedium) }
        Box(Modifier.weight(1f)) { primary() }
    }
}

@Composable
private fun Title(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorText(text: String) {
    Text(text, Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}


