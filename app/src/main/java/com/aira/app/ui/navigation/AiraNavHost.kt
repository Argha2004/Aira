package com.aira.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import com.aira.app.ui.components.LocalBottomInset
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.shadow
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
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
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
    // Shared with Cloudy: the pages are the source and the bottom bar blurs them.
    val blurSource = rememberSky()
    // The pages slide and fade when the tab changes; keep the blur following them while they move.
    LaunchedEffect(currentRoute) { blurSource.invalidate(durationMillis = 500) }
    val sky = skyCondition?.let { skyPalette(it.weatherCode, it.isDay, LocalTime.now().hour) }
    val temperatureUnit = if (useFahrenheit) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS

    CompositionLocalProvider(LocalTemperatureUnit provides temperatureUnit) {
    // Each page draws its own header (inside the NavHost), so the header slides together with its page. Only the
    // bottom bar lives here; it slides down out of view on pages that are not tabs (Settings and its sub-pages).
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Pages are full screen: they draw behind the status bar and the navigation bar themselves.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = onTab && !onOnboarding,
                enter = slideInVertically { height -> height },
                exit = slideOutVertically { height -> height },
            ) { AiraBottomBar(navController, currentRoute, onSky = sky != null, blurSource = blurSource) }
        },
    ) { padding ->
        // Pages reach the bottom of the screen so the sky shows through the frosted bar; each page keeps its
        // content above the bar with this padding.
        // The sky is also drawn behind the pages: while one page slides or fades out, the other comes in over the
        // same sky, so no white (or empty) background shows between them.
        Box(
            Modifier.fillMaxSize()
                .then(if (sky != null && !onOnboarding) Modifier.skyBackground(sky) else Modifier)
                // Cloudy copies what this box shows (the sky and the cards) so the bar can blur it.
                .sky(blurSource),
        ) {
        // On a tab: the pill's height. Elsewhere there is no pill, only the phone's navigation bar.
        val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomInset = if (onTab) padding.calculateBottomPadding() else navigationBar
        CompositionLocalProvider(LocalBottomInset provides bottomInset) {
        NavHost(
            navController = navController,
            startDestination = start,
            enterTransition = Transitions.enter,
            exitTransition = Transitions.exit,
            popEnterTransition = Transitions.popEnter,
            popExitTransition = Transitions.popExit,
            predictivePopEnterTransition = Transitions.predictivePopEnter,
            predictivePopExitTransition = Transitions.predictivePopExit,
        ) {
            val openSettings = { navController.navigate(SETTINGS_ROUTE) }
            val back: () -> Unit = { navController.popBackStack() }
            composable(ONBOARDING_ROUTE) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
                        .navigationBarsPadding(),
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
            Box(modifier = Modifier.weight(1f)) { content() }
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
            Box(modifier = Modifier.weight(1f).padding(bottom = LocalBottomInset.current)) { content() }
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
private fun AiraBottomBar(navController: NavHostController, currentRoute: String?, onSky: Boolean, blurSource: Sky) {
    // A floating pill. On the sky it is liquid glass: Cloudy blurs the page behind it, and a light wash and a
    // bright edge on top make it look like a thick piece of glass. Otherwise it is a plain card with a thin edge.
    val shape = CircleShape
    val barModifier = if (onSky) {
        Modifier
            .clip(shape)
            .cloudy(sky = blurSource, radius = 28, tint = Color.Black.copy(alpha = 0.10f), shape = shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.08f))))
            .border(BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.14f)))), shape)
    } else {
        Modifier.shadow(6.dp, shape).clip(shape).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    }
    Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = barModifier.fillMaxWidth().padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab -> PillTab(tab, selected = currentRoute == tab.route, onSky = onSky) { navController.navigateToTab(tab) } }
        }
    }
}

/** One tab of the pill: just its icon; the chosen tab is a highlighted pill that also shows its name. */
@Composable
private fun PillTab(tab: Tab, selected: Boolean, onSky: Boolean, onClick: () -> Unit) {
    val highlight = if (onSky) Color.White.copy(alpha = 0.28f) else MaterialTheme.colorScheme.primaryContainer
    val chosen = if (onSky) Color.White else MaterialTheme.colorScheme.primary
    val idle = if (onSky) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
    val label = stringResource(tab.label)
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) highlight else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .animateContentSize()
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, contentDescription = if (selected) null else label, tint = if (selected) chosen else idle, modifier = Modifier.size(24.dp))
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = chosen, maxLines = 1)
        }
    }
}
