package com.aira.app.ui.onboarding

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    WELCOME, WHY_LOCATION, LOCATION, BACKGROUND_LOCATION, ACTIVITY, NOTIFICATIONS, BATTERY, FINISH
}

/**
 * The screens shown for a given Android version ([sdk] = API level).
 * Background location and activity recognition are runtime permissions from Android 10 (API 29),
 * notifications from Android 13 (API 33).
 */
fun onboardingSteps(sdk: Int): List<OnboardingStep> = buildList {
    add(OnboardingStep.WELCOME)
    add(OnboardingStep.WHY_LOCATION)
    add(OnboardingStep.LOCATION)
    if (sdk >= 29) {
        add(OnboardingStep.BACKGROUND_LOCATION)
        add(OnboardingStep.ACTIVITY)
    }
    if (sdk >= 33) add(OnboardingStep.NOTIFICATIONS)
    add(OnboardingStep.BATTERY)
    add(OnboardingStep.FINISH)
}

data class OnboardingUiState(val steps: List<OnboardingStep>, val index: Int = 0) {
    val step: OnboardingStep get() = steps[index]
    val progress: Float get() = (index + 1f) / steps.size
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(onboardingSteps(Build.VERSION.SDK_INT)))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Goes to the next screen. Called after a permission answer (granted or not) and on Skip. */
    fun next() {
        _uiState.update { if (it.index < it.steps.lastIndex) it.copy(index = it.index + 1) else it }
    }

    /** Marks onboarding as done. The app then starts the background logging. */
    fun finish() {
        viewModelScope.launch {
            settings.setLoggingEnabled(true)
            settings.setOnboardingDone(true)
        }
    }
}
