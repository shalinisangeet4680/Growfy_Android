package org.mobile.growfy.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.core.ui.theme.GrowfyColors
import org.mobile.growfy.domain.model.Location

/** "All locations ▾" pill from Home (a2). */
@Composable
fun LocationPill(locations: List<Location>, selectedId: String?, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = locations.firstOrNull { it.id == selectedId }
    Box {
        Row(
            Modifier
                .height(40.dp)
                .widthIn(max = 240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { open = true }
                .padding(start = 14.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LocationSwatch(selected?.let { GrowfyColors.location(it.id) } ?: MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(8.dp))
            Text(
                selected?.title ?: "All locations",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(AppIcons.DropDown, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LocationMenu(open, locations, selectedId, onDismiss = { open = false }, onSelect = { open = false; onSelect(it) })
    }
}

/** "All locations ▾" filter chip from Reviews (a3). */
@Composable
fun LocationFilterChip(locations: List<Location>, selectedId: String?, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = locations.firstOrNull { it.id == selectedId }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { open = true },
            label = { Text(selected?.title ?: "All locations", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { Icon(AppIcons.DropDown, null, Modifier.size(18.dp)) },
            modifier = Modifier.widthIn(max = 200.dp),
        )
        LocationMenu(open, locations, selectedId, onDismiss = { open = false }, onSelect = { open = false; onSelect(it) })
    }
}

@Composable
private fun LocationMenu(
    expanded: Boolean,
    locations: List<Location>,
    selectedId: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("All locations") },
            leadingIcon = { LocationSwatch(MaterialTheme.colorScheme.onSurface) },
            trailingIcon = { if (selectedId == null) Icon(AppIcons.Check, null, Modifier.size(18.dp)) },
            onClick = { onSelect(null) },
        )
        locations.forEach { location ->
            DropdownMenuItem(
                text = { Text(location.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { LocationSwatch(GrowfyColors.location(location.id)) },
                trailingIcon = { if (selectedId == location.id) Icon(AppIcons.Check, null, Modifier.size(18.dp)) },
                onClick = { onSelect(location.id) },
            )
        }
    }
}
