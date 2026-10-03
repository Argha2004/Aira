package com.aira.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.BuildConfig
import com.aira.app.R
import com.aira.app.ui.components.LocalOnSky
import com.aira.app.ui.components.glassSurface
import com.aira.app.ui.components.PlainTheme
import com.aira.app.domain.engine.LoggingStatus
import com.aira.app.domain.engine.PlaceCandidate
import com.aira.app.domain.engine.PlaceSuggestions
import com.aira.app.domain.engine.LocationRules
import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.CustomLocation
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.SavedPlace
import com.aira.app.ui.components.AlertIcon
import com.aira.app.ui.components.labelRes
import com.aira.app.ui.theme.AiraTheme
import java.text.DateFormat
import java.util.Date

private val IntervalChoices = listOf(5, 15, 30, 60)
private val BatteryChoices = listOf(10, 15, 20, 30)
private val CooldownChoices = listOf(60, 180, 360)

/** The buttons of the Places section. */
data class PlaceActions(
    val onSetHomeHere: () -> Unit,
    val onSetCollegeHere: () -> Unit,
    val onUseHomeSuggestion: (GeoPoint) -> Unit,
    val onUseCollegeSuggestion: (GeoPoint) -> Unit,
    val onDeleteSaved: (id: Long) -> Unit = {},
    val onAddSaved: () -> Unit = {},
)

/** Everything the user can do on the Settings screen. */
data class SettingsActions(
    val onLoggingChange: (Boolean) -> Unit,
    val onIntervalChange: (Int) -> Unit,
    val onNightPauseChange: (Boolean) -> Unit,
    val onBatteryChange: (Int) -> Unit,
    val onFahrenheitChange: (Boolean) -> Unit,
    val onAlertsChange: (Boolean) -> Unit,
    val onAlertTypeChange: (AlertType, Boolean) -> Unit,
    val onCooldownChange: (Int) -> Unit,
    val places: PlaceActions,
    val onOpenSensorStatus: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onExportCsv: () -> Unit,
    val onDeleteAllData: () -> Unit,
    val onGenerateDemoData: () -> Unit,
    val onFireTestAlert: (AlertType) -> Unit,
    val onNameChange: (String) -> Unit = {},
)

/** Connects the screen to its ViewModel and to the system "save file" dialog. */
@Composable
fun SettingsRoute(
    onOpenSensorStatus: () -> Unit,
    modifier: Modifier = Modifier,
    onAddLocation: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The user may change a permission in the system settings and come back: look again.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.exportCsv(uri)
    }
    SettingsScreen(
        state = state,
        actions = SettingsActions(
            onLoggingChange = viewModel::setLoggingEnabled,
            onIntervalChange = viewModel::setIntervalMinutes,
            onNightPauseChange = viewModel::setNightPauseEnabled,
            onBatteryChange = viewModel::setBatterySaverPercent,
            onFahrenheitChange = viewModel::setUseFahrenheit,
            onAlertsChange = viewModel::setAlertsEnabled,
            onAlertTypeChange = viewModel::setAlertTypeEnabled,
            onCooldownChange = viewModel::setCooldownMinutes,
            places = PlaceActions(
                onSetHomeHere = viewModel::setHomeHere,
                onSetCollegeHere = viewModel::setCollegeHere,
                onUseHomeSuggestion = viewModel::useHomeSuggestion,
                onUseCollegeSuggestion = viewModel::useCollegeSuggestion,
                onDeleteSaved = viewModel::deleteSavedLocation,
                onAddSaved = onAddLocation,
            ),
            onOpenSensorStatus = onOpenSensorStatus,
            onOpenAppSettings = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                }
            },
            onExportCsv = { exportLauncher.launch("aira_snapshots.csv") },
            onDeleteAllData = viewModel::deleteAllData,
            onGenerateDemoData = viewModel::generateDemoData,
            onFireTestAlert = viewModel::fireTestAlert,
            onNameChange = viewModel::setUserName,
        ),
        modifier = modifier,
    )
}

/** Stateless screen: Logging, Units, Places, Alerts, Sensors, Data and About. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    showDebugTools: Boolean = BuildConfig.DEBUG,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsGroup { ProfileSection(state, actions) }
        SettingsGroup { LoggingSection(state, actions) }
        SettingsGroup { UnitsSection(state, actions) }
        SettingsGroup { PlacesSection(state, actions.places) }
        SettingsGroup { AlertsSection(state, actions, showDebugTools) }
        SettingsGroup { SensorsSection(actions) }
        SettingsGroup { DataSection(state, actions, onDeleteClick = { confirmDelete = true }, showDebugTools = showDebugTools) }
        SettingsGroup { AboutSection() }
    }

    if (confirmDelete) {
        PlainTheme {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDeleteAllData()
                }) { Text(stringResource(R.string.delete_confirm_button)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Sections
// ---------------------------------------------------------------------------------------------

/** A white rounded card around one section. The rows inside bring their own padding. */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    if (LocalOnSky.current) {
        // On the sky: frosted glass, like the cards on the tabs.
        // The rows inside are list items, which paint the surface colour: clear here so only the card is frosted.
        val clearRows = MaterialTheme.colorScheme.copy(surface = Color.Transparent)
        MaterialTheme(colorScheme = clearRows, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
            Column(modifier = Modifier.fillMaxWidth().glassSurface(MaterialTheme.shapes.large).padding(bottom = 8.dp), content = content)
        }
        return
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp), content = content)
    }
}

/** The name for the Home greeting and the avatar. Optional; it never leaves the phone. */
@Composable
private fun ProfileSection(state: SettingsUiState, actions: SettingsActions) {
    // The text the user is typing; until they type, the saved name is shown.
    var typed by remember { mutableStateOf<String?>(null) }
    SectionHeader(R.string.section_profile)
    OutlinedTextField(
        value = typed ?: state.userName,
        onValueChange = {
            typed = it
            actions.onNameChange(it)
        },
        label = { Text(stringResource(R.string.profile_name)) },
        supportingText = { Text(stringResource(R.string.profile_name_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

@Composable
private fun LoggingSection(state: SettingsUiState, actions: SettingsActions) {
    val lastSnapshot = state.lastSnapshotMillis?.let {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
    } ?: stringResource(R.string.never)

    SectionHeader(R.string.section_logging)
    SwitchItem(
        title = stringResource(R.string.logging),
        summary = stringResource(if (state.loggingEnabled) R.string.logging_on else R.string.logging_off),
        checked = state.loggingEnabled,
        onChange = actions.onLoggingChange,
    )
    // "On" must not hide that nothing can be recorded.
    when (state.loggingStatus) {
        LoggingStatus.NEEDS_LOCATION -> LoggingWarning(R.string.logging_needs_location, actions.onOpenAppSettings)
        LoggingStatus.NEEDS_BACKGROUND_LOCATION -> LoggingWarning(R.string.logging_needs_background, actions.onOpenAppSettings)
        LoggingStatus.OK, LoggingStatus.OFF -> Unit
    }
    ListItem(headlineContent = { Text(stringResource(R.string.last_snapshot, lastSnapshot)) })
    ChoiceItem(
        title = stringResource(R.string.interval_title),
        choices = IntervalChoices,
        selected = state.intervalMinutes,
        label = { stringResource(R.string.interval_minutes, it) },
        onSelect = actions.onIntervalChange,
    )
    SwitchItem(
        title = stringResource(R.string.night_pause),
        summary = stringResource(R.string.night_pause_summary),
        checked = state.nightPauseEnabled,
        onChange = actions.onNightPauseChange,
    )
    ChoiceItem(
        title = stringResource(R.string.battery_saver),
        summary = stringResource(R.string.battery_saver_summary),
        choices = BatteryChoices,
        selected = state.batterySaverPercent,
        label = { stringResource(R.string.percent_value, it) },
        onSelect = actions.onBatteryChange,
    )
}

/** A warning under the Logging switch, with a button to the app's system settings. */
@Composable
private fun LoggingWarning(messageRes: Int, onOpenAppSettings: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(messageRes), color = MaterialTheme.colorScheme.error) },
        supportingContent = {
            TextButton(onClick = onOpenAppSettings) { Text(stringResource(R.string.open_app_settings)) }
        },
    )
}

@Composable
private fun UnitsSection(state: SettingsUiState, actions: SettingsActions) {
    SectionHeader(R.string.section_units)
    SwitchItem(
        title = stringResource(R.string.use_fahrenheit),
        summary = stringResource(R.string.use_fahrenheit_summary),
        checked = state.useFahrenheit,
        onChange = actions.onFahrenheitChange,
    )
}

@Composable
private fun PlacesSection(state: SettingsUiState, actions: PlaceActions) {
    SectionHeader(R.string.places)
    PlaceItem(
        title = R.string.place_home,
        place = state.home,
        suggestion = state.suggestions.home,
        locationFailed = state.locationFailed,
        onSetHere = actions.onSetHomeHere,
        onUseSuggestion = actions.onUseHomeSuggestion,
    )
    PlaceItem(
        title = R.string.place_college,
        place = state.college,
        suggestion = state.suggestions.college,
        locationFailed = false,
        onSetHere = actions.onSetCollegeHere,
        onUseSuggestion = actions.onUseCollegeSuggestion,
    )
    SavedLocations(state.savedLocations, actions.onDeleteSaved, actions.onAddSaved)
}

/** The places added on the map (for the weather on Home): name and coordinates, a delete button, and Add location. */
@Composable
private fun SavedLocations(saved: List<CustomLocation>, onDelete: (Long) -> Unit, onAdd: () -> Unit) {
    Text(
        text = stringResource(R.string.loc_saved_title, saved.size, LocationRules.MAX_CUSTOM_LOCATIONS),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
    )
    if (saved.isEmpty()) {
        Text(
            stringResource(R.string.loc_saved_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    saved.forEach { place ->
        ListItem(
            headlineContent = { Text(place.name) },
            supportingContent = { Text(stringResource(R.string.loc_coordinates, place.latitude, place.longitude)) },
            leadingContent = { Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = {
                IconButton(onClick = { onDelete(place.id) }) {
                    Icon(Icons.Filled.DeleteOutline, stringResource(R.string.loc_delete, place.name))
                }
            },
        )
    }
    TextButton(
        onClick = onAdd,
        enabled = LocationRules.canAdd(saved.size),
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.loc_add))
    }
}

@Composable
private fun AlertsSection(state: SettingsUiState, actions: SettingsActions, showDebugTools: Boolean) {
    SectionHeader(R.string.section_alerts)
    SwitchItem(
        title = stringResource(R.string.alerts_enabled),
        summary = stringResource(R.string.alerts_enabled_summary),
        checked = state.alertsEnabled,
        onChange = actions.onAlertsChange,
    )
    ChoiceItem(
        title = stringResource(R.string.alert_cooldown),
        choices = CooldownChoices,
        selected = state.cooldownMinutes,
        label = { stringResource(R.string.cooldown_hours, it / 60) },
        onSelect = actions.onCooldownChange,
    )
    Text(
        text = stringResource(R.string.alert_types),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
    )
    AlertType.entries.forEach { type ->
        ListItem(
            headlineContent = { Text(stringResource(type.labelRes())) },
            leadingContent = { AlertIcon(type) },
            trailingContent = {
                Switch(
                    checked = state.alertTypesEnabled[type] ?: true,
                    onCheckedChange = { actions.onAlertTypeChange(type, it) },
                    enabled = state.alertsEnabled,
                )
            },
        )
    }
    // Debug builds only: one button per alert type.
    if (showDebugTools) {
        AlertType.entries.forEach { type ->
            ListItem(
                headlineContent = { Text(stringResource(R.string.fire_test_alert, stringResource(type.labelRes()))) },
                supportingContent = { Text(stringResource(R.string.fire_test_alert_summary)) },
                modifier = Modifier.clickable { actions.onFireTestAlert(type) },
            )
        }
    }
}

@Composable
private fun SensorsSection(actions: SettingsActions) {
    SectionHeader(R.string.section_sensors)
    ListItem(
        headlineContent = { Text(stringResource(R.string.sensor_status)) },
        supportingContent = { Text(stringResource(R.string.sensor_status_summary)) },
        modifier = Modifier.clickable(onClick = actions.onOpenSensorStatus),
    )
}

@Composable
private fun DataSection(state: SettingsUiState, actions: SettingsActions, onDeleteClick: () -> Unit, showDebugTools: Boolean) {
    SectionHeader(R.string.section_data)
    ListItem(
        headlineContent = { Text(stringResource(R.string.export_csv)) },
        supportingContent = { Text(stringResource(R.string.export_csv_summary)) },
        modifier = Modifier.clickable(onClick = actions.onExportCsv),
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.delete_all_data)) },
        supportingContent = { Text(stringResource(R.string.delete_all_data_summary)) },
        modifier = Modifier.clickable(onClick = onDeleteClick),
    )
    state.dataMessage?.let { message ->
        Text(
            text = when (message) {
                is DataMessage.Exported -> pluralStringResource(R.plurals.export_done, message.count, message.count)
                DataMessage.ExportFailed -> stringResource(R.string.export_failed)
                DataMessage.Deleted -> stringResource(R.string.data_deleted)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    // Debug builds only: hidden in release builds.
    if (showDebugTools) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.generate_demo_data)) },
            supportingContent = {
                Text(
                    stringResource(
                        if (state.demoDataAdded) R.string.demo_data_added else R.string.generate_demo_data_summary,
                    ),
                )
            },
            modifier = Modifier.clickable(onClick = actions.onGenerateDemoData),
        )
    }
}

@Composable
private fun AboutSection() {
    SectionHeader(R.string.section_about)
    ListItem(
        headlineContent = { Text(stringResource(R.string.app_name)) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
                Text(stringResource(R.string.about_project))
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------------------------

@Composable
private fun SectionHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchItem(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

/** A title with a row of exclusive buttons, e.g. 15 / 30 / 60 minutes. */
@Composable
private fun ChoiceItem(
    title: String,
    choices: List<Int>,
    selected: Int,
    label: @Composable (Int) -> String,
    onSelect: (Int) -> Unit,
    summary: String? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                if (summary != null) Text(summary)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    choices.forEachIndexed { index, choice ->
                        SegmentedButton(
                            selected = choice == selected,
                            onClick = { onSelect(choice) },
                            shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                            // No check mark: the fill already shows the choice, and the label keeps room on narrow phones.
                            icon = {},
                        ) { Text(label(choice), maxLines = 1, softWrap = false) }
                    }
                }
            }
        },
    )
}

@Composable
private fun PlaceItem(
    title: Int,
    place: SavedPlace?,
    suggestion: PlaceCandidate?,
    locationFailed: Boolean,
    onSetHere: () -> Unit,
    onUseSuggestion: (GeoPoint) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = {
            Column {
                Text(
                    if (place != null) {
                        stringResource(R.string.location_value, place.latitude, place.longitude)
                    } else {
                        stringResource(R.string.place_not_set)
                    },
                )
                if (locationFailed) Text(stringResource(R.string.place_location_failed), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onSetHere) { Text(stringResource(R.string.place_set_current)) }
                if (suggestion != null) {
                    Text(
                        stringResource(R.string.place_suggestion_hint, suggestion.point.latitude, suggestion.point.longitude),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { onUseSuggestion(suggestion.point) }) {
                        Text(pluralStringResource(R.plurals.place_use_suggestion, suggestion.snapshotCount, suggestion.snapshotCount))
                    }
                }
            }
        },
    )
}

private val previewActions = SettingsActions(
    onLoggingChange = {}, onIntervalChange = {}, onNightPauseChange = {}, onBatteryChange = {},
    onFahrenheitChange = {}, onAlertsChange = {}, onAlertTypeChange = { _, _ -> }, onCooldownChange = {},
    places = PlaceActions({}, {}, {}, {}),
    onOpenSensorStatus = {}, onOpenAppSettings = {}, onExportCsv = {}, onDeleteAllData = {}, onGenerateDemoData = {},
    onFireTestAlert = {},
)

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    AiraTheme {
        SettingsScreen(
            state = SettingsUiState(
                lastSnapshotMillis = 1_790_000_000_000,
                home = SavedPlace("Home", 22.5726, 88.3639),
                suggestions = PlaceSuggestions(null, PlaceCandidate(GeoPoint(22.5958, 88.2636), 84)),
                dataMessage = DataMessage.Exported(120),
                loggingStatus = LoggingStatus.NEEDS_BACKGROUND_LOCATION,
            ),
            actions = previewActions,
            showDebugTools = true,
        )
    }
}
