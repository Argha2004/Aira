package com.aira.app.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.R
import com.aira.app.data.sensor.SensorStatus
import com.aira.app.domain.model.GeoPoint
import com.aira.app.ui.components.AiraCard
import com.aira.app.ui.components.formatTemperature
import com.aira.app.ui.theme.AiraTheme
import java.text.DateFormat
import java.util.Date

/** Which runtime permissions the user has granted. */
data class PermissionState(val location: Boolean, val activity: Boolean) {
    val allGranted get() = location && activity
}

private fun readPermissions(context: Context): PermissionState {
    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    return PermissionState(
        location = granted(Manifest.permission.ACCESS_COARSE_LOCATION),
        // Activity recognition is only a runtime permission from Android 10.
        activity = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            granted(Manifest.permission.ACTIVITY_RECOGNITION),
    )
}

/**
 * Connects the screen to its ViewModel and handles the permission request.
 * ACTIVITY_RECOGNITION is only requested from Android 10 (see readPermissions).
 */
@SuppressLint("InlinedApi")
@Composable
fun SensorStatusRoute(modifier: Modifier = Modifier, viewModel: SensorStatusViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var permissions by remember { mutableStateOf(readPermissions(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissions = readPermissions(context)
    }
    SensorStatusScreen(
        state = state,
        permissions = permissions,
        onRequestPermissions = {
            val missing = buildList {
                if (!permissions.location) add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (!permissions.activity) add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            launcher.launch(missing.toTypedArray())
        },
        onReadNow = viewModel::readNow,
        onTakeSnapshot = viewModel::takeSnapshotNow,
        modifier = modifier,
    )
}

/** Stateless screen: only draws the state it is given. */
@Composable
fun SensorStatusScreen(
    state: SensorStatusUiState,
    permissions: PermissionState,
    onRequestPermissions: () -> Unit,
    onReadNow: () -> Unit,
    onTakeSnapshot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PermissionsCard(permissions, onRequestPermissions)
        AvailabilityCard(state.availability)
        Button(onClick = onReadNow, enabled = !state.isReading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.isReading) R.string.reading else R.string.read_now))
        }
        state.reading?.let { ReadingCard(it) }
        Button(onClick = onTakeSnapshot, enabled = !state.isTakingSnapshot, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.isTakingSnapshot) R.string.taking_snapshot else R.string.take_snapshot))
        }
        state.snapshotOutcome?.let { SnapshotCard(it) }
    }
}

@Composable
private fun SnapshotCard(outcome: SnapshotOutcome) {
    val na = stringResource(R.string.not_available)
    SectionCard(R.string.snapshot) {
        when (outcome) {
            is SnapshotOutcome.Failed -> {
                val reason = when {
                    outcome.noLocation -> stringResource(R.string.error_no_location)
                    else -> outcome.message ?: stringResource(R.string.error_unknown)
                }
                Text(stringResource(R.string.snapshot_failed, reason))
            }
            is SnapshotOutcome.Saved -> with(outcome.snapshot) {
                val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(timestamp))
                ValueRow(R.string.snapshot_saved_at, time)
                ValueRow(R.string.snapshot_place, place.name)
                ValueRow(R.string.snapshot_movement, movement.name)
                ValueRow(R.string.snapshot_temperature, formatTemperature(temperature))
                ValueRow(R.string.sensor_light, lux?.let { stringResource(R.string.lux_value, it) } ?: na)
                ValueRow(R.string.snapshot_steps_delta, stepsDelta?.toString() ?: na)
            }
        }
    }
}

@Composable
private fun PermissionsCard(permissions: PermissionState, onRequest: () -> Unit) {
    SectionCard(R.string.permissions) {
        if (permissions.allGranted) {
            Text(stringResource(R.string.permissions_granted))
        } else {
            Text(stringResource(R.string.permissions_explanation))
            Button(onClick = onRequest) { Text(stringResource(R.string.grant_permissions)) }
        }
    }
}

@Composable
private fun AvailabilityCard(status: SensorStatus) {
    SectionCard(R.string.sensors) {
        AvailabilityRow(R.string.sensor_light, status.light)
        AvailabilityRow(R.string.sensor_proximity, status.proximity)
        AvailabilityRow(R.string.sensor_steps, status.stepCounter)
        AvailabilityRow(R.string.sensor_accelerometer, status.accelerometer)
        AvailabilityRow(R.string.sensor_pressure, status.pressure)
    }
}

@Composable
private fun ReadingCard(reading: SensorReading) {
    val na = stringResource(R.string.not_available)
    SectionCard(R.string.readings) {
        ValueRow(R.string.sensor_light, reading.lux?.let { stringResource(R.string.lux_value, it) } ?: na)
        ValueRow(
            R.string.in_pocket,
            reading.inPocket?.let { stringResource(if (it) R.string.yes else R.string.no) } ?: na,
        )
        ValueRow(R.string.sensor_steps, reading.totalSteps?.toString() ?: na)
        ValueRow(R.string.sensor_pressure, reading.pressure?.let { stringResource(R.string.hpa_value, it) } ?: na)
        ValueRow(R.string.movement, reading.movement?.let { stringResource(R.string.movement_value, it) } ?: na)
        ValueRow(
            R.string.location,
            reading.location?.let { stringResource(R.string.location_value, it.latitude, it.longitude) } ?: na,
        )
    }
}

@Composable
private fun AvailabilityRow(labelRes: Int, available: Boolean) {
    ValueRow(labelRes, stringResource(if (available) R.string.available else R.string.missing))
}

@Composable
private fun ValueRow(labelRes: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(labelRes))
        Text(value)
    }
}

@Composable
private fun SectionCard(titleRes: Int, content: @Composable () -> Unit) {
    AiraCard {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        content()
    }
}

private val previewStatus = SensorStatus(
    light = true, proximity = true, stepCounter = true, accelerometer = true, pressure = false,
)

@Preview(showBackground = true)
@Composable
private fun SensorStatusPreview() {
    AiraTheme {
        SensorStatusScreen(
            state = SensorStatusUiState(
                availability = previewStatus,
                reading = SensorReading(
                    lux = 12000f, inPocket = false, totalSteps = 48213, pressure = null,
                    movement = 0.4f, location = GeoPoint(22.57, 88.36),
                ),
            ),
            permissions = PermissionState(location = false, activity = true),
            onRequestPermissions = {},
            onReadNow = {},
            onTakeSnapshot = {},
        )
    }
}
