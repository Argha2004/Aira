package com.aira.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.aira.app.R

const val ONBOARDING_ROUTE = "onboarding"
const val SETTINGS_ROUTE = "settings"
const val SENSOR_STATUS_ROUTE = "sensor_status"
const val LOCATION_PICKER_ROUTE = "location_picker"

/** The five tabs of the bottom bar. */
enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    HOME("home", R.string.tab_home, Icons.Outlined.Home),
    TIMELINE("timeline", R.string.tab_timeline, Icons.AutoMirrored.Outlined.TrendingUp),
    CALENDAR("calendar", R.string.tab_calendar, Icons.Outlined.CalendarMonth),
    INSIGHTS("insights", R.string.tab_insights, Icons.Outlined.BarChart),
    TASKS("tasks", R.string.tab_tasks, Icons.Outlined.Tune),
}
