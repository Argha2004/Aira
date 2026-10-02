package com.aira.app.ui.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.aira.app.ui.components.AiraCard
import com.aira.app.ui.components.IconTile
import com.aira.app.ui.components.SlideContent
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.R
import com.aira.app.ui.theme.AiraTheme

/** Text of one onboarding screen. */
private data class StepText(@StringRes val title: Int, @StringRes val body: Int, @StringRes val button: Int)

private fun textFor(step: OnboardingStep) = when (step) {
    OnboardingStep.WELCOME -> StepText(R.string.onb_welcome_title, R.string.onb_welcome_body, R.string.onb_get_started)
    OnboardingStep.WHY_LOCATION -> StepText(R.string.onb_why_title, R.string.onb_why_body, R.string.onb_continue)
    OnboardingStep.LOCATION -> StepText(R.string.onb_location_title, R.string.onb_location_body, R.string.onb_allow)
    OnboardingStep.BACKGROUND_LOCATION ->
        StepText(R.string.onb_background_title, R.string.onb_background_body, R.string.onb_open_permission)
    OnboardingStep.ACTIVITY -> StepText(R.string.onb_activity_title, R.string.onb_activity_body, R.string.onb_allow)
    OnboardingStep.NOTIFICATIONS ->
        StepText(R.string.onb_notifications_title, R.string.onb_notifications_body, R.string.onb_allow)
    OnboardingStep.BATTERY -> StepText(R.string.onb_battery_title, R.string.onb_battery_body, R.string.onb_open_settings)
    OnboardingStep.FINISH -> StepText(R.string.onb_finish_title, R.string.onb_finish_body, R.string.onb_start_logging)
}

private fun iconFor(step: OnboardingStep): ImageVector = when (step) {
    OnboardingStep.WELCOME -> Icons.Filled.WbSunny
    OnboardingStep.WHY_LOCATION -> Icons.Filled.Lock
    OnboardingStep.LOCATION -> Icons.Filled.LocationOn
    OnboardingStep.BACKGROUND_LOCATION -> Icons.Filled.NearMe
    OnboardingStep.ACTIVITY -> Icons.AutoMirrored.Filled.DirectionsWalk
    OnboardingStep.NOTIFICATIONS -> Icons.Filled.Notifications
    OnboardingStep.BATTERY -> Icons.Filled.BatteryChargingFull
    OnboardingStep.FINISH -> Icons.Filled.CheckCircle
}

/**
 * Handles the permission requests; the screen itself only draws the current step.
 * The newer permission names are only used on the Android versions that have them (see onboardingSteps).
 */
@SuppressLint("InlinedApi")
@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Whatever the user answers, we move on: the app still works without a permission.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.next()
    }

    OnboardingScreen(
        state = state,
        onPrimary = {
            when (state.step) {
                OnboardingStep.WELCOME, OnboardingStep.WHY_LOCATION -> viewModel.next()
                OnboardingStep.LOCATION -> permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                OnboardingStep.BACKGROUND_LOCATION -> permission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                OnboardingStep.ACTIVITY -> permission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                OnboardingStep.NOTIFICATIONS -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                OnboardingStep.BATTERY -> {
                    // Some phone brands do not have this settings screen; then we simply carry on.
                    runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                    viewModel.next()
                }
                OnboardingStep.FINISH -> {
                    viewModel.finish()
                    onFinished()
                }
            }
        },
        onSkip = viewModel::next,
        modifier = modifier,
    )
}

/** Stateless screen: draws the current step with a progress bar and buttons. */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onPrimary: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = textFor(state.step)
    val canSkip = state.step != OnboardingStep.WELCOME && state.step != OnboardingStep.FINISH
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth().clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )
        // Each step slides in from the right (or back from the left if a step is ever revisited).
        SlideContent(target = state, key = { it.index }, forward = { from, to -> to.index > from.index }) { shown ->
            val shownText = textFor(shown.step)
            AiraCard {
                IconTile(icon = iconFor(shown.step), tint = MaterialTheme.colorScheme.primary, size = 72)
                Text(stringResource(shownText.title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(shownText.body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(text.button))
            }
            if (canSkip) {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.onb_skip)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    AiraTheme {
        OnboardingScreen(
            state = OnboardingUiState(onboardingSteps(sdk = 34), index = 3),
            onPrimary = {},
            onSkip = {},
        )
    }
}
