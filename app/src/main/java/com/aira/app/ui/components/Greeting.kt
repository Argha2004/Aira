package com.aira.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aira.app.R
import java.time.LocalTime

/** "Good afternoon" or "Good afternoon, Alex", depending on the time of day. */
@Composable
fun greetingText(userName: String): String {
    val hour = LocalTime.now().hour
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.greeting_morning
            hour < 18 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        },
    )
    return if (userName.isBlank()) greeting else stringResource(R.string.greeting_with_name, greeting, userName.trim())
}

