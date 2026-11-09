package org.mobile.growfy.feature.posts

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.mobile.growfy.core.platform.rememberPhotoPicker
import org.mobile.growfy.core.ui.components.LocalSnackbarHostState
import org.mobile.growfy.core.ui.components.LocationBadge
import org.mobile.growfy.core.ui.components.LocationSwatch
import org.mobile.growfy.core.ui.components.PhotoPlaceholder
import androidx.compose.ui.platform.testTag
import org.mobile.growfy.core.ui.TestTags
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.core.ui.theme.GrowfyColors
import org.mobile.growfy.core.util.longDay
import org.mobile.growfy.core.util.scheduleLabel
import org.mobile.growfy.core.util.today
import org.mobile.growfy.domain.model.CtaType
import org.mobile.growfy.domain.model.PostType
import org.mobile.growfy.navigation.Route
import kotlin.time.Instant

@Composable
fun ComposePostScreen(
    route: Route.ComposePost,
    onClose: () -> Unit,
    viewModel: ComposePostViewModel = koinViewModel(key = "${route.postId}-${route.scheduleAt}") { parametersOf(route) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) { if (state.closed) onClose() }
    val pickPhoto = rememberPhotoPicker(viewModel::uploadPhoto)
    var previewing by remember { mutableStateOf(false) }
    var dateDialog by remember { mutableStateOf<DateTarget?>(null) }
    var timeDialog by remember { mutableStateOf<LocalDate?>(null) }
    val editable = !state.readOnly && !state.saving

    Scaffold(
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.postId == null) "New post" else if (state.readOnly) "Post" else "Edit post") },
                navigationIcon = { IconButton(onClick = onClose, modifier = Modifier.testTag(TestTags.COMPOSE_CLOSE)) { Icon(AppIcons.Close, "Close") } },
                actions = {
                    if (!state.readOnly) {
                        Button(
                            onClick = viewModel::publish,
                            enabled = editable && state.problem == null && !state.uploading,
                            modifier = Modifier.padding(end = 8.dp).height(40.dp).testTag(TestTags.COMPOSE_PUBLISH),
                        ) { Text(if (state.scheduled) "Schedule" else "Post") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().imePadding()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { previewing = true }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(AppIcons.Eye, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Preview on Google", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.postId != null) {
                        TextButton(onClick = viewModel::delete, enabled = !state.saving) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (!state.readOnly) TextButton(onClick = viewModel::saveDraft, enabled = editable, modifier = Modifier.testTag(TestTags.COMPOSE_DRAFT)) { Text("Save draft") }
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.readOnly) {
                Text(
                    "Google doesn’t allow editing a published post. Delete it and post again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PostType.entries.forEachIndexed { i, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { viewModel.setType(type) },
                        enabled = editable,
                        shape = SegmentedButtonDefaults.itemShape(i, PostType.entries.size),
                    ) { Text(type.label) }
                }
            }
            LocationField(state, enabled = editable && state.postId == null, onSelect = viewModel::setLocation)
            if (state.type.hasSchedule) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    label = { Text(if (state.type == PostType.Offer) "Offer title" else "Event title") },
                    singleLine = true,
                    enabled = editable,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateBox("Starts", state.startDate, Modifier.weight(1f), editable) { dateDialog = DateTarget.Start }
                    DateBox("Ends", state.endDate, Modifier.weight(1f), editable) { dateDialog = DateTarget.End }
                }
            }
            OutlinedTextField(
                value = state.summary,
                onValueChange = viewModel::setSummary,
                label = { Text("What’s new?") },
                minLines = 4,
                enabled = editable,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.COMPOSE_SUMMARY),
                supportingText = {
                    Text("${state.summary.length} / ${ComposeUiState.SUMMARY_MAX}", Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                },
            )
            PhotoSection(state, editable, pickPhoto, viewModel::removePhoto)
            if (state.type == PostType.Offer) {
                OutlinedTextField(
                    value = state.couponCode,
                    onValueChange = viewModel::setCoupon,
                    label = { Text("Coupon code (optional)") },
                    singleLine = true,
                    enabled = editable,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SettingRow(
                icon = AppIcons.Calendar,
                title = "Schedule",
                subtitle = if (state.scheduled) state.scheduledAt.scheduleLabel() else "Posts right away",
                subtitleColor = if (state.scheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = if (state.scheduled && editable) ({ dateDialog = DateTarget.Schedule }) else null,
            ) {
                Switch(
                    checked = state.scheduled,
                    onCheckedChange = viewModel::setScheduled,
                    enabled = editable,
                    thumbContent = if (state.scheduled) ({ Icon(AppIcons.Check, null, Modifier.size(14.dp)) }) else null,
                )
            }
            CtaRow(state, editable, viewModel::setCta)
            if (state.cta?.needsUrl == true) {
                val invalid = state.ctaUrl.isNotBlank() && !state.ctaUrl.startsWith("https://")
                OutlinedTextField(
                    value = state.ctaUrl,
                    onValueChange = viewModel::setCtaUrl,
                    label = { Text("Button link") },
                    placeholder = { Text("https://") },
                    singleLine = true,
                    isError = invalid,
                    supportingText = if (invalid) ({ Text("Use an https link") }) else null,
                    enabled = editable,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val problem = state.problem
            if (!state.readOnly && problem != null && state.summary.isNotBlank()) {
                Text(problem, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    dateDialog?.let { target ->
        val initial = when (target) {
            DateTarget.Start -> state.startDate
            DateTarget.End -> state.endDate
            DateTarget.Schedule -> state.scheduledAt.date
        }
        DateDialog(initial, onDismiss = { dateDialog = null }) { picked ->
            dateDialog = null
            when (target) {
                DateTarget.Start -> viewModel.setStart(picked)
                DateTarget.End -> viewModel.setEnd(picked)
                DateTarget.Schedule -> timeDialog = picked
            }
        }
    }
    timeDialog?.let { day ->
        val timeState = rememberTimePickerState(state.scheduledAt.hour, state.scheduledAt.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { timeDialog = null },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setScheduledAt(day.atTime(LocalTime(timeState.hour, timeState.minute)))
                    timeDialog = null
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { timeDialog = null }) { Text("Cancel") } },
            text = { TimePicker(timeState) },
        )
    }
    if (previewing) {
        ModalBottomSheet(onDismissRequest = { previewing = false }) { GooglePreview(state) }
    }
}

private enum class DateTarget { Start, End, Schedule }

@Composable
private fun LocationField(state: ComposeUiState, enabled: Boolean, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = state.location?.title.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Location") },
            leadingIcon = state.location?.let { { LocationSwatch(GrowfyColors.location(it.id), size = 12.dp) } },
            trailingIcon = { Icon(AppIcons.DropDown, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (enabled) Box(Modifier.matchParentSize().clickable { open = true })
        DropdownMenu(open, onDismissRequest = { open = false }) {
            state.locations.forEach { location ->
                DropdownMenuItem(
                    text = { Text(location.title) },
                    leadingIcon = { LocationSwatch(GrowfyColors.location(location.id), size = 12.dp) },
                    onClick = { open = false; onSelect(location.id) },
                )
            }
        }
    }
}

@Composable
private fun DateBox(label: String, date: LocalDate, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Box(modifier) {
        OutlinedTextField(
            value = date.longDay(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (enabled) Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

@Composable
private fun PhotoSection(state: ComposeUiState, editable: Boolean, pickPhoto: (() -> Unit)?, onRemove: () -> Unit) {
    val mediaUrl = state.mediaUrl
    when {
        mediaUrl != null -> Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(12.dp))) {
            PhotoPlaceholder(Modifier.matchParentSize())
            AsyncImage(mediaUrl, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            if (editable) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Close, "Remove photo", Modifier.size(16.dp), tint = Color.White) }
            }
        }
        state.uploading -> Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp))
        }
        pickPhoto != null && editable -> {
            val outline = MaterialTheme.colorScheme.outline
            Row(
                Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp))
                    .drawBehind {
                        drawRoundRect(
                            outline,
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                        )
                    }
                    .clickable(onClick = pickPhoto),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+  Add photo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CtaRow(state: ComposeUiState, editable: Boolean, onSelect: (CtaType?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        SettingRow(
            icon = AppIcons.ButtonPill,
            title = "Button",
            subtitle = state.cta?.label ?: "No button",
            onClick = if (editable) ({ open = true }) else null,
        ) { Icon(AppIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            CtaType.entries.forEach { type ->
                DropdownMenuItem(text = { Text(type.label) }, onClick = { open = false; onSelect(type) })
            }
            DropdownMenuItem(text = { Text("No button") }, onClick = { open = false; onSelect(null) })
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = subtitleColor)
        }
        trailing()
    }
}

@Composable
private fun DateDialog(initial: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val utc = TimeZone.UTC
    val todayMillis = today().atStartOfDayIn(utc).toEpochMilliseconds()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDayIn(utc).toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPicked(Instant.fromEpochMilliseconds(it).toLocalDateTime(utc).date) } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(pickerState) }
}

@Composable
private fun GooglePreview(state: ComposeUiState) {
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Preview on Google", style = MaterialTheme.typography.titleMedium)
        Column(
            Modifier.fillMaxWidth().clip(shape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            state.location?.let { location ->
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LocationBadge(location, size = 32.dp)
                    Text(location.title, style = MaterialTheme.typography.labelLarge)
                }
            }
            state.mediaUrl?.let {
                Box(Modifier.fillMaxWidth().height(160.dp)) {
                    PhotoPlaceholder(Modifier.matchParentSize())
                    AsyncImage(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                }
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.type.hasSchedule && state.title.isNotBlank()) {
                    Text(state.title, style = MaterialTheme.typography.titleMedium)
                    Text("${state.startDate.longDay()} – ${state.endDate.longDay()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    state.summary.ifBlank { "Your post appears here" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.summary.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                )
                state.cta?.let { OutlinedButton(onClick = {}, enabled = false) { Text(it.label) } }
            }
        }
    }
}
