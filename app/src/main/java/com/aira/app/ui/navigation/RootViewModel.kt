package com.aira.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.app.data.settings.SettingsStore
import com.aira.app.ui.components.CurrentSky
import com.aira.app.ui.components.SkyCondition
import com.aira.app.worker.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide state: whether onboarding is done (decides the first screen), and keeping the
 * background jobs in line with the settings (logging on/off and interval).
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    settings: SettingsStore,
    scheduler: WorkScheduler,
    currentSky: CurrentSky,
) : ViewModel() {

    /**
     * The two things the first screen needs, read together: whether onboarding is done and the saved sky. The app
     * draws nothing until both are known, so it never opens on a plain page and then switches to the sky.
     */
    private val startup = combine(settings.onboardingDone, currentSky.current) { done, sky -> done to sky }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** null while the setting is still loading. */
    val onboardingDone: StateFlow<Boolean?> = startup.map { it?.first }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The weather behind every tab's sky (the saved one at first, then what Home loads). */
    val sky: StateFlow<SkyCondition?> = startup.map { it?.second }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** °F instead of °C; every screen reads this through a CompositionLocal. */
    val useFahrenheit: StateFlow<Boolean> = settings.useFahrenheit
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The name for the avatar initial in the top bar (empty if not set). */
    val userName: StateFlow<String> = settings.userName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        viewModelScope.launch {
            combine(settings.onboardingDone, settings.loggingEnabled, settings.intervalMinutes) { done, on, minutes ->
                Triple(done, on, minutes)
            }.collect { (done, loggingOn, minutes) ->
                when {
                    !done -> Unit // Nothing is scheduled before onboarding is finished.
                    loggingOn -> {
                        scheduler.scheduleSnapshots(minutes)
                        scheduler.scheduleWeatherFill()
                    }
                    else -> scheduler.cancelAll()
                }
            }
        }
    }
}
