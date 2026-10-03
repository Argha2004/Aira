package com.aira.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import com.aira.app.ui.components.greetingText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aira.app.R
import com.aira.app.domain.engine.TemperatureUnit
import com.aira.app.ui.calendar.CalendarRoute
import com.aira.app.ui.components.LocalTemperatureUnit
import com.aira.app.ui.home.HomeRoute
import java.time.LocalTime
import com.aira.app.ui.home.skyPalette
import com.aira.app.ui.home.skyBackground
import com.aira.app.ui.home.SkyPalette
import com.aira.app.ui.home.LightStatusBarIcons
import com.aira.app.ui.components.LocalOnSky
import com.aira.app.ui.components.LocalPlainColors
import com.aira.app.ui.components.glassColors
import com.aira.app.ui.insights.InsightsRoute
import com.aira.app.ui.locations.LocationPickerRoute
import com.aira.app.ui.onboarding.OnboardingRoute
import com.aira.app.ui.settings.SensorStatusRoute
import com.aira.app.ui.settings.SettingsRoute
import com.aira.app.ui.tasks.TasksRoute
import com.aira.app.ui.timeline.TimelineRoute

/**
 * App shell. First launch shows onboarding; after that: top bar (with gear icon),
 * bottom bar with 5 tabs, and the screens.
 */
/** Room for the bottom bar (and the phone's navigation bar) under each page's content. */
private val LocalBottomBarPadding = compositionLocalOf { 0.dp }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiraNavHost(
    navController: NavHostController = rememberNavController(),
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    // The first screen is decided once, when the "onboarding done" setting has loaded.
    val onboardingDone by rootViewModel.onboardingDone.collectAsStateWithLifecycle()
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(onboardingDone) {
        val done = onboardingDone
        if (startDestination == null && done != null) {
            startDestination = if (done) Tab.HOME.route else ONBOARDING_ROUTE
        }
    }
    val start = startDestination ?: return // still loading: draw nothing for a moment

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onOnboarding = currentRoute == ONBOARDING_ROUTE
    // Tab screens show the gear icon and bottom bar; other screens show a back arrow.
    val onTab = currentRoute == null || Tab.entries.any { it.route == currentRoute }
    val useFahrenheit by rootViewModel.useFahrenheit.collectAsStateWithLifecycle()
    val userName by rootViewModel.userName.collectAsStateWithLifecycle()
    val skyCondition by rootViewModel.sky.collectAsStateWithLifecycle()
    val sky = skyCondition?.let { skyPalette(it.weatherCode, it.isDay, LocalTime.now().hour) }
    val temperatureUnit = if (useFahrenheit) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS

    CompositionLocalProvider(LocalTemperatureUnit provides temperatureUnit) {
    // Each page draws its own header (inside the NavHost), so the header slides together with its page. Only the
    // bottom bar lives here; it slides down out of view on pages that are not tabs (Settings and its sub-pages).
    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = onTab && !onOnboarding,
                enter = slideInVertically { height -> height },
                exit = slideOutVertically { height -> height },
            ) { AiraBottomBar(navController, currentRoute, onSky = sky != null) }
        },
    ) { padding ->
        // Pages reach the bottom of the screen so the sky shows through the frosted bar; each page keeps its
        // content above the bar with this padding.
        CompositionLocalProvider(LocalBottomBarPadding provides padding.calculateBottomPadding()) {
        NavHost(
            navController = navController,
            startDestination = start,
            enterTransition = Transitions.enter,
            exitTransition = Transitions.exit,
            popEnterTransition = Transitions.popEnter,
            popExitTransition = Transitions.popExit,
        ) {
            val openSettings = { navController.navigate(SETTINGS_ROUTE) }
            val back: () -> Unit = { navController.popBackStack() }
            composable(ONBOARDING_ROUTE) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
                        .padding(bottom = LocalBottomBarPadding.current),
                ) {
                OnboardingRoute(
                    onFinished = {
                        navController.navigate(Tab.HOME.route) {
                            popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                        }
                    },
                )
                }
            }
            composable(Tab.HOME.route) {
                TabPage(stringResource(R.string.app_name), greetingText(userName), sky, openSettings) {
                    HomeRoute(
                        onOpenTimeline = { navController.navigateToTab(Tab.TIMELINE) },
                        onOpenInsights = { navController.navigateToTab(Tab.INSIGHTS) },
                        onAddLocation = { navController.navigate(LOCATION_PICKER_ROUTE) },
                    )
                }
            }
            composable(Tab.TIMELINE.route) {
                TabPage(stringResource(Tab.TIMELINE.label), null, sky, openSettings) { TimelineRoute() }
            }
            composable(Tab.CALENDAR.route) {
                TabPage(stringResource(Tab.CALENDAR.label), null, sky, openSettings) {
                    CalendarRoute(onOpenTimeline = { navController.navigateToTab(Tab.TIMELINE) })
                }
            }
            composable(Tab.INSIGHTS.route) {
                TabPage(stringResource(Tab.INSIGHTS.label), null, sky, openSettings) { InsightsRoute() }
            }
            composable(Tab.TASKS.route) {
                TabPage(stringResource(Tab.TASKS.label), null, sky, openSettings) { TasksRoute() }
            }
            composable(SETTINGS_ROUTE) {
                SubPage(stringResource(R.string.settings), back, sky) {
                    SettingsRoute(
                        onOpenSensorStatus = { navController.navigate(SENSOR_STATUS_ROUTE) },
                        onAddLocation = { navController.navigate(LOCATION_PICKER_ROUTE) },
                    )
                }
            }
            composable(SENSOR_STATUS_ROUTE) {
                SubPage(stringResource(R.string.sensor_status), back, sky) { SensorStatusRoute() }
            }
            composable(LOCATION_PICKER_ROUTE) {
                SubPage(stringResource(R.string.loc_add), back, sky) { LocationPickerRoute(onDone = back) }
            }
        }
        }
    }
    }
}

/** A tab: its big title and gear button on top, then the tab's screen. */
@Composable
private fun TabPage(title: String, subtitle: String?, sky: SkyPalette?, onOpenSettings: () -> Unit, content: @Composable () -> Unit) {
    // The weather sky fills the whole screen (status bar and header too) and stays still while the cards scroll.
    if (sky != null) LightStatusBarIcons()
    val pageBackground = MaterialTheme.colorScheme.background
    GlassContent(onSky = sky != null) {
        Column(
            modifier = Modifier.fillMaxSize().background(pageBackground)
                .then(if (sky != null) Modifier.skyBackground(sky) else Modifier)
                .statusBarsPadding(),
        ) {
            TabHeader(title, subtitle, onOpenSettings, onSky = sky != null)
            Box(modifier = Modifier.weight(1f).padding(bottom = LocalBottomBarPadding.current)) { content() }
        }
    }
}

/**
 * On the sky every card, chip and bar is glass: [content] gets see-through white surfaces and white text.
 * Without the sky it is drawn as usual.
 */
@Composable
private fun GlassContent(onSky: Boolean, content: @Composable () -> Unit) {
    if (!onSky) return content()
    val plain = MaterialTheme.colorScheme
    MaterialTheme(colorScheme = glassColors(plain), typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        CompositionLocalProvider(LocalOnSky provides true, LocalPlainColors provides plain, LocalContentColor provides Color.White, content = content)
    }
}

/** A page opened from a tab (Settings and its sub-pages): a bar with a back arrow and the title, then the page. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubPage(title: String, onBack: () -> Unit, sky: SkyPalette? = null, content: @Composable () -> Unit) {
    // With a sky (Settings, Sensor status and Add location) the page is glass like the tabs.
    if (sky != null) LightStatusBarIcons()
    val pageBackground = MaterialTheme.colorScheme.background
    GlassContent(onSky = sky != null) {
        Column(
            modifier = Modifier.fillMaxSize().background(pageBackground)
                .then(if (sky != null) Modifier.skyBackground(sky) else Modifier)
                .statusBarsPadding(),
        ) {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                // The page already keeps itself below the status bar.
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Box(modifier = Modifier.weight(1f).padding(bottom = LocalBottomBarPadding.current)) { content() }
        }
    }
}

/** Switches to a tab the way the bottom bar does: one copy of each tab, and its state is kept. */
private fun NavHostController.navigateToTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(Tab.HOME.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The header of every tab: a big title on the left (on Home "Aira" with the greeting under it) and a round
 * gear button on the right that opens Settings.
 */
@Composable
private fun TabHeader(title: String, subtitle: String?, onOpenSettings: () -> Unit, onSky: Boolean = false) {
    // On Home's sky the header is see-through with white text.
    val text = if (onSky) Color.White else MaterialTheme.colorScheme.onSurface
    val muted = if (onSky) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (onSky) Color.Transparent else MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = text)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = muted, maxLines = 1)
            }
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (onSky) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Settings, stringResource(R.string.settings), tint = text)
        }
    }
}

@Composable
private fun AiraBottomBar(navController: NavHostController, currentRoute: String?, onSky: Boolean) {
    // On the sky the bar is frosted glass: a see-through white wash with a thin light line on top.
    val selected = if (onSky) Color.White else MaterialTheme.colorScheme.primary
    val unselected = if (onSky) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
    NavigationBar(
        containerColor = if (onSky) Color.Transparent else MaterialTheme.colorScheme.surface,
        modifier = if (onSky) {
            Modifier
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.14f))))
                .drawBehind { drawLine(Color.White.copy(alpha = 0.35f), Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
        } else {
            Modifier
        },
    ) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { navController.navigateToTab(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label), style = MaterialTheme.typography.labelSmall) },
                // No pill behind the selected tab: its icon and label simply turn blue.
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = selected,
                    selectedTextColor = selected,
                    unselectedIconColor = unselected,
                    unselectedTextColor = unselected,
                ),
            )
        }
    }
}
