@file:OptIn(ExperimentalMaterial3Api::class)

package com.aira.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aira.app.R
import com.aira.app.ui.components.PlainTheme
import com.aira.app.domain.engine.LocationRules
import com.aira.app.domain.model.CustomLocation
import com.aira.app.ui.components.IconTile
import com.aira.app.ui.components.WideButton
import com.aira.app.ui.components.formatDegrees

/**
 * The "Locations" sheet, opened from the location row on Home: the live location first (always there), then
 * up to five saved places with their temperature. Tap one to show it on Home; the bin deletes a saved place.
 */
@Composable
fun LocationsSheet(
    places: List<CustomLocation>,
    selectedId: Long?,
    temps: Map<Long, Double>,
    liveSubtitle: String,
    onSelect: (Long?) -> Unit,
    onDelete: (Long) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    PlainTheme {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.loc_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.loc_count, places.size, LocationRules.MAX_CUSTOM_LOCATIONS),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PlaceRow(
                icon = Icons.Filled.MyLocation,
                name = stringResource(R.string.loc_live),
                subtitle = liveSubtitle,
                temp = null,
                selected = selectedId == null,
                onClick = { onSelect(null); onDismiss() },
                onDelete = null,
            )
            places.forEach { place ->
                PlaceRow(
                    icon = Icons.Filled.Place,
                    name = place.name,
                    subtitle = stringResource(R.string.loc_coordinates, place.latitude, place.longitude),
                    temp = temps[place.id],
                    selected = selectedId == place.id,
                    onClick = { onSelect(place.id); onDismiss() },
                    onDelete = { onDelete(place.id) },
                )
            }
            val canAdd = LocationRules.canAdd(places.size)
            WideButton(onClick = { onDismiss(); onAdd() }, enabled = canAdd) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.loc_add), style = MaterialTheme.typography.titleMedium)
            }
            if (!canAdd) {
                Text(
                    stringResource(R.string.loc_full),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    }
}

@Composable
private fun PlaceRow(
    icon: ImageVector,
    name: String,
    subtitle: String,
    temp: Double?,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick).padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, MaterialTheme.colorScheme.primary, size = 40)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (temp != null) Text(formatDegrees(temp), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
        if (selected) {
            Icon(Icons.Filled.CheckCircle, stringResource(R.string.loc_selected), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp).size(20.dp))
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, stringResource(R.string.loc_delete, name), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
