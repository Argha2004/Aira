package com.aira.app.ui.onboarding

import com.aira.app.ui.onboarding.OnboardingStep.ACTIVITY
import com.aira.app.ui.onboarding.OnboardingStep.BACKGROUND_LOCATION
import com.aira.app.ui.onboarding.OnboardingStep.BATTERY
import com.aira.app.ui.onboarding.OnboardingStep.FINISH
import com.aira.app.ui.onboarding.OnboardingStep.LOCATION
import com.aira.app.ui.onboarding.OnboardingStep.NOTIFICATIONS
import com.aira.app.ui.onboarding.OnboardingStep.WELCOME
import com.aira.app.ui.onboarding.OnboardingStep.WHY_LOCATION
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStepsTest {

    @Test
    fun `android 8 has no background, activity or notification permission screens`() =
        assertEquals(listOf(WELCOME, WHY_LOCATION, LOCATION, BATTERY, FINISH), onboardingSteps(26))

    @Test
    fun `android 10 adds background location and activity recognition`() =
        assertEquals(
            listOf(WELCOME, WHY_LOCATION, LOCATION, BACKGROUND_LOCATION, ACTIVITY, BATTERY, FINISH),
            onboardingSteps(29),
        )

    @Test
    fun `android 13 also adds notifications in the guide order`() =
        assertEquals(
            listOf(
                WELCOME, WHY_LOCATION, LOCATION, BACKGROUND_LOCATION, ACTIVITY, NOTIFICATIONS, BATTERY, FINISH,
            ),
            onboardingSteps(33),
        )

    @Test
    fun `android 12 has no notification screen`() =
        assertEquals(false, onboardingSteps(32).contains(NOTIFICATIONS))

    @Test
    fun `progress goes from first to last step`() {
        val steps = onboardingSteps(33)
        assertEquals(1f / steps.size, OnboardingUiState(steps, 0).progress, 0.0001f)
        assertEquals(1f, OnboardingUiState(steps, steps.lastIndex).progress, 0.0001f)
    }
}
