package com.aira.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aira.app.R
import com.aira.app.domain.engine.AirBand
import com.aira.app.domain.engine.Altitude
import com.aira.app.domain.engine.TemperatureUnit
import com.aira.app.domain.engine.Comfort
import com.aira.app.domain.engine.LightLevel
import com.aira.app.domain.engine.DailyStats
import com.aira.app.domain.engine.HomeBands
import com.aira.app.domain.engine.UvBand
import com.aira.app.domain.engine.UvLevel
import com.aira.app.domain.engine.WindLevel
import com.aira.app.domain.engine.formatMinutesShort
import com.aira.app.domain.model.AirQuality
import com.aira.app.domain.model.AlertType
import com.aira.app.domain.model.DiaryEvent
import com.aira.app.domain.model.DiaryEventType
import com.aira.app.domain.model.GeoPoint
import com.aira.app.domain.model.HourlyForecast
import com.aira.app.domain.model.Movement
import com.aira.app.domain.model.Place
import com.aira.app.domain.model.Snapshot
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.model.WeatherCode
import com.aira.app.domain.model.WeatherNow
import com.aira.app.domain.model.WeatherTask
import com.aira.app.domain.engine.Thresholds
import com.aira.app.ui.components.rememberBarometer
import com.aira.app.ui.components.AiraCard
import com.aira.app.ui.components.AnimatedWeatherIcon
import com.aira.app.ui.components.CompassDial
import com.aira.app.ui.components.ExposureTile
import com.aira.app.ui.components.rememberHeading
import com.aira.app.ui.components.LocalTemperatureUnit
import com.aira.app.ui.components.IconTile
import com.aira.app.ui.components.MetricTile
import com.aira.app.ui.components.SectionLabel
import com.aira.app.ui.components.StatCard
import com.aira.app.ui.components.eventTitle
import com.aira.app.ui.components.formatDegrees
import com.aira.app.ui.components.formatTemperature
import com.aira.app.ui.components.localizeTemperatures
import com.aira.app.ui.lognote.LogSkyNoteButton
import com.aira.app.ui.theme.AiraTheme
import com.aira.app.ui.theme.HeatCoral
import com.aira.app.ui.theme.LiveGreen
import com.aira.app.ui.theme.RainBlue
import com.aira.app.ui.theme.SunAmber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SUN_GOAL_MINUTES = 60
private const val HEAT_CAP_MINUTES = 30
private const val RAIN_GOAL_ENCOUNTERS = 3
private const val DIARY_ROWS = 3

/** Connects the screen to its ViewModel. */
@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    onOpenTimeline: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onAddLocation: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Reload when returning to the app, e.g. after granting location permission.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    // While Home is on screen the "Right now" card refreshes itself every 5 minutes.
    LifecycleResumeEffect(viewModel) {
        viewModel.startLive()
        onPauseOrDispose { viewModel.stopLive() }
    }
    HomeScreen(
        state = state,
        onRetry = viewModel::load,
        actions = HomeActions(
            onAcknowledge = viewModel::dismissAlert,
            onRemindLater = viewModel::remindLater,
            onOpenTimeline = onOpenTimeline,
            onOpenInsights = onOpenInsights,
            onOpenPlaces = viewModel::loadPlaceTemps,
            onSelectPlace = viewModel::selectPlace,
            onDeletePlace = viewModel::deletePlace,
            onAddPlace = onAddLocation,
        ),
        modifier = modifier,
    )
}

/** Everything the user can do on Home besides retrying the weather. */
data class HomeActions(
    val onAcknowledge: (alertId: Long) -> Unit = {},
    /** "Remind in 30m": the alert, its first task (if any) and its first suggestion (if any). */
    val onRemindLater: (alertId: Long, taskId: Long?, suggestion: String?) -> Unit = { _, _, _ -> },
    val onOpenTimeline: () -> Unit = {},
    val onOpenInsights: () -> Unit = {},
    /** The locations sheet was opened (load the saved places' temperatures). */
    val onOpenPlaces: () -> Unit = {},
    val onSelectPlace: (id: Long?) -> Unit = {},
    val onDeletePlace: (id: Long) -> Unit = {},
    val onAddPlace: () -> Unit = {},
)

/**
 * Stateless Home screen, top to bottom: the main weather card, four small cards (humidity, UV, wind, air),
 * today's exposure, the active alert, the hourly forecast, today's sky diary, and the "Log Sky Note" button.
 * The title and greeting are in the shared header above.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    actions: HomeActions = HomeActions(),
    logButton: @Composable () -> Unit = { LogSkyNoteButton() },
) {
    var showPlaces by rememberSaveable { mutableStateOf(false) }
    // The page sits on the weather sky drawn by the tab page; the main card is frosted glass over it.
    Column(
        modifier = modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (val w = state.weather) {
            WeatherState.Loading -> AiraCard {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is WeatherState.Error -> AiraCard { ErrorContent(w.message, onRetry) }
            is WeatherState.Success -> {
                MainWeatherCard(w, onPlaceClick = { actions.onOpenPlaces(); showPlaces = true })
                WeatherCards(w, state.yesterdayHumidity)
                val sunrise = w.weather.sunrise
                val sunset = w.weather.sunset
                if (sunrise != null && sunset != null) SunCard(sunrise, sunset)
                MoonCard(sunrise, sunset)
            }
        }
        (state.weather as? WeatherState.Success)?.let { AltitudeCard(it) }
        CompassCard((state.weather as? WeatherState.Success)?.weather?.windDirection)
        AmbientCard(state.latest, state.today)
        ExposureCard(state.today, state.yesterdayOutdoorMinutes, actions.onOpenInsights)
        state.activeAlert?.let { alert ->
            AlertCard(alert, state.alertTasks, state.suggestions, actions)
        }
        (state.weather as? WeatherState.Success)?.hours?.takeIf { it.isNotEmpty() }?.let { HourlyCard(it) }
        DiaryCard(state.events, actions.onOpenTimeline)
        logButton()
    }

    if (showPlaces) {
        val live = state.weather as? WeatherState.Success
        LocationsSheet(
            places = state.places,
            selectedId = state.selectedPlaceId,
            temps = state.placeTemps,
            liveSubtitle = stringResource(
                if (live != null && live.placeName == null && live.isDefaultLocation) R.string.home_location_default else R.string.loc_live_gps,
            ),
            onSelect = actions.onSelectPlace,
            onDelete = actions.onDeletePlace,
            onAdd = actions.onAddPlace,
            onDismiss = { showPlaces = false },
        )
    }
}

// ---- Main weather card ----

@Composable
private fun MainWeatherCard(state: WeatherState.Success, onPlaceClick: () -> Unit) {
    val weather = state.weather
    GlassCard {
        // Tapping the place opens the locations sheet.
        Row(
            modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onPlaceClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (state.placeName == null) Icons.Filled.MyLocation else Icons.Filled.LocationOn,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                state.placeName ?: stringResource(if (state.isDefaultLocation) R.string.home_location_default else R.string.home_location_device),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BigTemperature(formatTemperature(weather.tempC))
                Text(WeatherCode.describe(weather.weatherCode), style = MaterialTheme.typography.titleLarge)
                val highLow = if (weather.highC != null && weather.lowC != null) {
                    " · " + stringResource(R.string.home_high_low, formatTemperature(weather.highC), formatTemperature(weather.lowC))
                } else {
                    ""
                }
                Text(
                    stringResource(R.string.home_feels_like, formatTemperature(weather.feelsLikeC)) + highLow,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // A smaller picture on narrow phones so the temperature and text keep their room.
            val narrow = LocalConfiguration.current.screenWidthDp < 360
            AnimatedWeatherIcon(weather.weatherCode, weather.isDay, size = if (narrow) 84.dp else 112.dp)
        }
    }
}

/** "28°C" drawn as a big bold "28" with a smaller "°C" next to it. */
@Composable
private fun BigTemperature(text: String) {
    val number = text.takeWhile { it.isDigit() || it == '-' || it == '−' || it == '.' }
    Row(verticalAlignment = Alignment.Top) {
        Text(number, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.SemiBold)
        Text(
            text.drop(number.length),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

// ---- Four small weather cards ----

@Composable
private fun WeatherCards(state: WeatherState.Success, yesterdayHumidity: Double?) {
    val weather = state.weather
    val none = stringResource(R.string.ins_none)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val change = HomeBands.humidityChange(weather.humidity, yesterdayHumidity)
            StatCard(
                label = stringResource(R.string.humidity),
                icon = Icons.Filled.WaterDrop,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                value = stringResource(R.string.percent, weather.humidity),
                caption = change?.let { stringResource(R.string.humidity_vs_yesterday, it) } ?: stringResource(R.string.mini_no_yesterday),
                captionColor = when {
                    change == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    change <= 0 -> LiveGreen
                    else -> HeatCoral
                },
                modifier = Modifier.weight(1f),
            )
            val uv = HomeBands.uv(weather.uvIndex)
            StatCard(
                label = stringResource(R.string.uv_index),
                icon = Icons.Filled.WbSunny,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                value = stringResource(R.string.whole_number, weather.uvIndex),
                unit = stringResource(uvLevelText(uv)),
                caption = stringResource(uvAdvice(uv)),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(R.string.wind),
                icon = Icons.Filled.Air,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                value = stringResource(R.string.whole_number, weather.windKmh),
                unit = stringResource(R.string.unit_kmh),
                caption = weather.windDirection?.let {
                    stringResource(R.string.wind_caption, HomeBands.compass(it), stringResource(windText(HomeBands.wind(weather.windKmh))))
                } ?: stringResource(windText(HomeBands.wind(weather.windKmh))),
                modifier = Modifier.weight(1f),
            )
            val air = state.air
            val band = air?.let { HomeBands.air(it.aqi) }
            StatCard(
                label = stringResource(R.string.air_title),
                icon = Icons.Filled.Eco,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                value = air?.aqi?.toString() ?: none,
                unit = band?.let { stringResource(airText(it)) },
                caption = stringResource(band?.let { airAdvice(it) } ?: R.string.air_unavailable),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun uvLevelText(level: UvLevel): Int = when (level) {
    UvLevel.LOW -> R.string.uv_level_low
    UvLevel.MODERATE -> R.string.uv_level_moderate
    UvLevel.HIGH -> R.string.uv_level_high
    UvLevel.VERY_HIGH -> R.string.uv_level_very_high
}

private fun uvAdvice(level: UvLevel): Int = when (level) {
    UvLevel.LOW -> R.string.uv_advice_low
    UvLevel.MODERATE -> R.string.uv_advice_moderate
    UvLevel.HIGH -> R.string.uv_advice_high
    UvLevel.VERY_HIGH -> R.string.uv_advice_very_high
}

private fun windText(level: WindLevel): Int = when (level) {
    WindLevel.CALM -> R.string.wind_calm
    WindLevel.GENTLE -> R.string.wind_gentle
    WindLevel.BREEZY -> R.string.wind_breezy
    WindLevel.WINDY -> R.string.wind_windy
}

private fun airText(band: AirBand): Int = when (band) {
    AirBand.GOOD -> R.string.air_good
    AirBand.MODERATE -> R.string.air_moderate
    AirBand.SENSITIVE -> R.string.air_sensitive
    AirBand.UNHEALTHY -> R.string.air_unhealthy
}

private fun airAdvice(band: AirBand): Int = when (band) {
    AirBand.GOOD -> R.string.air_advice_good
    AirBand.MODERATE -> R.string.air_advice_moderate
    AirBand.SENSITIVE -> R.string.air_advice_sensitive
    AirBand.UNHEALTHY -> R.string.air_advice_unhealthy
}

@Composable
private fun ErrorContent(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.weather_error), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(message ?: stringResource(R.string.error_unknown), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

// ---- Altitude ----

/**
 * The user's live altitude from the phone's barometer, read live while Home is open, with today's sea-level
 * pressure from the weather service (the standard 1013.25 hPa when Home shows a saved place, since that pressure
 * is for somewhere else). Phones without a barometer see "Pressure sensor unavailable". Feet when the user chose °F.
 */
@Composable
private fun AltitudeCard(state: WeatherState.Success) {
    val barometer = rememberBarometer()
    val seaLevel = state.weather.pressureHpa.takeIf { state.placeName == null } ?: Altitude.STANDARD_SEA_LEVEL_HPA
    val metres = Altitude.fromPressure(barometer.pressureHpa?.toDouble(), seaLevel)
    val feet = LocalTemperatureUnit.current == TemperatureUnit.FAHRENHEIT
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    AiraCard {
        SectionLabel(stringResource(R.string.alt_title))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Filled.Terrain, MaterialTheme.colorScheme.primary, size = 52)
            Column(modifier = Modifier.weight(1f)) {
                if (!barometer.available) {
                    Text(stringResource(R.string.alt_no_sensor), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    return@Column
                }
                Text(stringResource(R.string.alt_you), style = MaterialTheme.typography.labelMedium, color = muted)
                Text(
                    metres?.let { heightText(it, feet) } ?: stringResource(R.string.ins_none),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                barometer.pressureHpa?.let {
                    Text(stringResource(R.string.alt_from_barometer, it, seaLevel), style = MaterialTheme.typography.labelMedium, color = muted)
                }
            }
        }
    }
}

/** "12 m" or "39 ft". */
@Composable
private fun heightText(metres: Int, feet: Boolean): String =
    if (feet) stringResource(R.string.alt_feet, Altitude.toFeet(metres)) else stringResource(R.string.alt_metres, metres)

// ---- Compass ----

/**
 * A live compass from the phone's rotation-vector sensor: a dial that turns with the phone, the heading as
 * degrees and compass point, and where the wind comes from. Not shown on phones without a magnetometer.
 */
@Composable
private fun CompassCard(windFrom: Int?) {
    val heading = rememberHeading() ?: return
    AiraCard {
        SectionLabel(stringResource(R.string.compass_title))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            CompassDial(heading, windFrom = windFrom)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.compass_heading), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.compass_value, heading.roundToInt() % 360, HomeBands.compass(heading.roundToInt())),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (windFrom != null) {
                    Text(
                        stringResource(R.string.compass_wind, HomeBands.compass(windFrom)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.compass_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---- Right now: personal ambient context ----

/**
 * "Right now · Personal ambient context": the newest sensor snapshot, always from where the user is. Where
 * they are and how they move, a plain sentence about it, and three tiles: light, microclimate and steps.
 */
@Composable
private fun AmbientCard(latest: Snapshot?, today: DailyStats) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    AiraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.now_title), modifier = Modifier.weight(1f))
            if (latest != null) {
                Text(stringResource(R.string.now_live_sync, agoText(latest.timestamp)), style = MaterialTheme.typography.labelMedium, color = muted)
            }
        }
        if (latest == null) {
            Text(stringResource(R.string.now_empty), style = MaterialTheme.typography.bodyMedium, color = muted)
            return@AiraCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(placeText(latest.place)) + " · " + stringResource(movementText(latest.movement)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(nowSentence(latest), style = MaterialTheme.typography.bodyMedium, color = muted)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(modifier = Modifier.fillMaxWidth()) {
            val lux = latest.lux
            AmbientValue(
                label = stringResource(R.string.tile_daylight),
                value = lux?.let { stringResource(R.string.lux_grouped, it) } ?: stringResource(R.string.not_available),
                caption = lux?.let { stringResource(lightText(HomeBands.light(it))) },
                modifier = Modifier.weight(1f),
            )
            AmbientValue(
                label = stringResource(R.string.tile_microclimate),
                value = if (latest.weatherPending) stringResource(R.string.not_available) else formatTemperature(latest.temperature),
                caption = if (latest.weatherPending) null else stringResource(comfortText(HomeBands.comfort(latest.feelsLike))),
                modifier = Modifier.weight(1f),
            )
            AmbientValue(
                label = stringResource(R.string.tile_activity),
                value = stringResource(R.string.steps_grouped, today.steps),
                caption = stringResource(R.string.steps_logged),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** One plain value of the ambient card: small grey label, the value, a grey caption. */
@Composable
private fun AmbientValue(label: String, value: String, caption: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        // Always the caption's room, so the three values keep the same height when one has no caption.
        Text(caption.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2, maxLines = 2)
    }
}

/** "just now", "2m ago", "1h ago". */
@Composable
private fun agoText(millis: Long): String {
    val minutes = ((System.currentTimeMillis() - millis) / 60_000).toInt().coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.time_just_now)
        minutes < 60 -> stringResource(R.string.time_min_ago, minutes)
        else -> stringResource(R.string.time_h_ago, minutes / 60)
    }
}

private fun placeText(place: Place): Int = when (place) {
    Place.OUTDOOR -> R.string.now_outdoors
    Place.INDOOR -> R.string.now_indoors
    Place.UNKNOWN -> R.string.now_unknown_place
}

private fun movementText(movement: Movement): Int = when (movement) {
    Movement.WALKING -> R.string.now_walking
    Movement.STILL -> R.string.now_still
    Movement.VEHICLE -> R.string.now_vehicle
}

private fun lightText(level: LightLevel): Int = when (level) {
    LightLevel.STRONG_SUN -> R.string.light_strong_sun
    LightLevel.BRIGHT -> R.string.light_bright
    LightLevel.INDOOR -> R.string.light_indoor
    LightLevel.DIM -> R.string.light_dim
}

private fun comfortText(comfort: Comfort): Int = when (comfort) {
    Comfort.COOL -> R.string.comfort_cool
    Comfort.COMFORTABLE -> R.string.comfort_comfortable
    Comfort.WARM -> R.string.comfort_warm
    Comfort.HOT -> R.string.comfort_hot
}

/** One plain sentence about where the user is and how it feels there. */
@Composable
private fun nowSentence(snapshot: Snapshot): String {
    val who = stringResource(
        when {
            snapshot.movement == Movement.VEHICLE -> R.string.now_s_vehicle
            snapshot.place == Place.OUTDOOR && snapshot.movement == Movement.WALKING -> R.string.now_s_walk_out
            snapshot.place == Place.OUTDOOR -> R.string.now_s_out
            snapshot.place == Place.INDOOR && snapshot.movement == Movement.WALKING -> R.string.now_s_walk_in
            snapshot.place == Place.INDOOR -> R.string.now_s_in
            else -> R.string.now_s_unknown
        },
    )
    return if (snapshot.weatherPending) {
        stringResource(R.string.now_sentence, who)
    } else {
        stringResource(R.string.now_sentence_weather, who, formatTemperature(snapshot.feelsLike), snapshot.humidity)
    }
}

// ---- Exposure ----

@Composable
private fun ExposureCard(stats: DailyStats, yesterdayOutdoorMinutes: Int, onDetails: () -> Unit) {
    AiraCard {
        SectionLabel(stringResource(R.string.exposure_title), action = stringResource(R.string.details), onAction = onDetails)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ExposureTile(
                label = stringResource(R.string.exp_daylight),
                value = stringResource(R.string.exp_minutes_short, stats.sunMinutes),
                caption = stringResource(R.string.exp_goal, SUN_GOAL_MINUTES),
                progressValue = stats.sunMinutes,
                goal = SUN_GOAL_MINUTES,
                color = SunAmber,
                modifier = Modifier.weight(1f),
            )
            ExposureTile(
                label = stringResource(R.string.exp_heat),
                value = stringResource(R.string.exp_minutes_short, stats.heatMinutes),
                caption = stringResource(R.string.exp_cap, HEAT_CAP_MINUTES),
                progressValue = stats.heatMinutes,
                goal = HEAT_CAP_MINUTES,
                color = HeatCoral,
                modifier = Modifier.weight(1f),
            )
            ExposureTile(
                label = stringResource(R.string.exp_rain),
                value = stringResource(R.string.exp_times, stats.rainEncounters),
                caption = stringResource(R.string.exp_rain_caption),
                progressValue = stats.rainEncounters,
                goal = RAIN_GOAL_ENCOUNTERS,
                color = RainBlue,
                modifier = Modifier.weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                boldValue(stringResource(R.string.outdoor_today), formatMinutesShort(stats.outdoorMinutes)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            val difference = stats.outdoorMinutes - yesterdayOutdoorMinutes
            Text(
                when {
                    difference > 0 -> stringResource(R.string.vs_yesterday_more, formatMinutesShort(difference))
                    difference < 0 -> stringResource(R.string.vs_yesterday_less, formatMinutesShort(abs(difference)))
                    else -> stringResource(R.string.vs_yesterday_same)
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (difference >= 0) LiveGreen else HeatCoral,
            )
        }
    }
}

/** "Outdoor time today: " followed by the value in bold. */
private fun boldValue(label: String, value: String): AnnotatedString = buildAnnotatedString {
    append(label)
    append(" ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(value) }
}

// ---- Alert ----

/**
 * The newest active alert: headline (with the rain chance for "rain soon"), what to do, and two buttons:
 * Acknowledge (dismisses the alert) and, if a task belongs to it, snooze that task; otherwise add the
 * first suggestion as a task.
 */
@Composable
private fun AlertCard(alert: WeatherAlert, tasks: List<WeatherTask>, suggestions: List<String>, actions: HomeActions) {
    AiraCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(localizeTemperatures(alert.message), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            val chance = alert.value?.takeIf { alert.type == AlertType.RAIN_SOON }
            if (chance != null) {
                Text(
                    stringResource(R.string.alert_chance, chance.toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        val body = when {
            tasks.isNotEmpty() -> stringResource(R.string.alert_your_tasks, tasks.joinToString(", ") { it.title })
            suggestions.isNotEmpty() -> stringResource(R.string.alert_suggested, suggestions.take(2).joinToString(", "))
            else -> null
        }
        if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { actions.onAcknowledge(alert.id) },
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) { Text(stringResource(R.string.acknowledge)) }
            Spacer(Modifier.width(8.dp))
            val firstTask = tasks.firstOrNull()
            val firstSuggestion = suggestions.firstOrNull()
            if (firstTask != null || firstSuggestion != null) {
                TextButton(onClick = { actions.onRemindLater(alert.id, firstTask?.id, firstSuggestion) }) {
                    Text(stringResource(R.string.remind_later))
                }
            }
        }
    }
}

// ---- Hourly forecast ----

@Composable
private fun HourlyCard(hours: List<HourlyForecast>) {
    // The first rainy hour is highlighted, like the "4 PM" in the design.
    val rainy = hours.firstOrNull { it.precipProbability >= Thresholds.RAIN_SOON_PROBABILITY }?.time
    val format = remember { DateTimeFormatter.ofPattern("h a") }
    AiraCard {
        SectionLabel(stringResource(R.string.hourly_title), action = pluralStringResource(R.plurals.hourly_hours, hours.size, hours.size))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            hours.forEachIndexed { index, hour ->
                val time = Instant.ofEpochMilli(hour.time).atZone(ZoneId.systemDefault())
                val highlighted = hour.time == rainy
                Column(
                    modifier = Modifier.width(60.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (highlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .then(
                            if (highlighted) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            else Modifier,
                        )
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        if (index == 0) stringResource(R.string.now) else time.format(format),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HourIcon(hour, time.hour)
                    Text(formatDegrees(hour.tempC ?: hour.feelsLikeC), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.percent, hour.precipProbability),
                        style = MaterialTheme.typography.labelSmall,
                        color = RainBlue,
                    )
                }
            }
        }
    }
}

/**
 * The hour's weather icon from its weather code. Without a code: rain likely is a drop, maybe rain a cloud,
 * otherwise the sun by day or the moon at night.
 */
@Composable
private fun HourIcon(hour: HourlyForecast, clockHour: Int) {
    val day = clockHour in Thresholds.DAY_START_HOUR until Thresholds.DAY_END_HOUR
    val code = hour.weatherCode
    val (icon, tint) = when {
        code != null && code <= 1 -> if (day) Icons.Filled.WbSunny to SunAmber else Icons.Filled.NightsStay to MaterialTheme.colorScheme.primary
        code == 2 -> Icons.Filled.WbCloudy to (if (day) SunAmber else MaterialTheme.colorScheme.onSurfaceVariant)
        code != null && code in 3..48 -> Icons.Filled.Cloud to MaterialTheme.colorScheme.onSurfaceVariant
        code != null && code in 71..86 -> Icons.Filled.AcUnit to RainBlue
        code != null && code >= 95 -> Icons.Filled.Thunderstorm to RainBlue
        code != null && code >= 51 -> Icons.Filled.Grain to RainBlue
        hour.precipProbability >= Thresholds.RAIN_SOON_PROBABILITY -> Icons.Filled.WaterDrop to RainBlue
        hour.precipProbability >= MAYBE_RAIN_PERCENT -> Icons.Filled.Cloud to MaterialTheme.colorScheme.onSurfaceVariant
        day -> Icons.Filled.WbSunny to SunAmber
        else -> Icons.Filled.NightsStay to MaterialTheme.colorScheme.primary
    }
    Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
}

private const val MAYBE_RAIN_PERCENT = 30

// ---- Sky diary ----

@Composable
private fun DiaryCard(events: List<DiaryEvent>, onOpenTimeline: () -> Unit) {
    AiraCard {
        SectionLabel(stringResource(R.string.diary_title), action = stringResource(R.string.view_all), onAction = onOpenTimeline)
        if (events.isEmpty()) {
            Text(stringResource(R.string.diary_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // The latest few, in time order.
            events.sortedBy { it.startTime }.takeLast(DIARY_ROWS).forEach { DiaryRow(it) }
        }
    }
}

/** One diary event as a row: time, what it was and its summary, and the temperature. */
@Composable
fun DiaryRow(event: DiaryEvent, modifier: Modifier = Modifier) {
    val format = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val time = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault()).toLocalTime().format(format)
    Row(
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(time, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(eventTitle(event.type, event.startTime), style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                localizeTemperatures(event.summary),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (event.avgTemperature != 0.0) Text(formatTemperature(event.avgTemperature), style = MaterialTheme.typography.titleSmall)
    }
}

// ---- Previews ----

private val previewWeather = WeatherNow(
    tempC = 28.0, feelsLikeC = 31.0, humidity = 72, rainMm = 0.0, uvIndex = 4.2,
    windKmh = 12.0, weatherCode = 2, isDay = true, sunrise = null, sunset = null,
    highC = 29.0, lowC = 21.0, pressureHpa = 1014.0, elevationM = 9.0,
)
private val previewStats = DailyStats(
    sunMinutes = 42, heatMinutes = 18, rainEncounters = 2, uvDoseBand = UvBand.MODERATE, outdoorMinutes = 80, steps = 3840,
)
private val previewHours = (0 until 6).map {
    HourlyForecast(time = 1_790_650_000_000 + it * 3_600_000L, precipProbability = listOf(10, 15, 30, 75, 20, 10)[it], precipitationMm = 0.0, feelsLikeC = 28.0 - it)
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun HomeFullPreview() {
    AiraTheme {
        HomeScreen(
            HomeUiState(
                weather = WeatherState.Success(previewWeather, GeoPoint(22.57, 88.36), false, air = AirQuality(34, 8.0), hours = previewHours),
                today = previewStats,
                yesterdayOutdoorMinutes = 45,
                yesterdayHumidity = 77.0,
                latest = Snapshot(
                    timestamp = System.currentTimeMillis() - 120_000, latitude = 22.57, longitude = 88.36, temperature = 29.4,
                    feelsLike = 31.0, humidity = 72, rainMm = 0.0, uvIndex = 4.0, windSpeed = 12.0, lux = 1280f, stepsDelta = 60,
                    place = Place.OUTDOOR, movement = Movement.WALKING, pressure = 1012.5f,
                ),
                activeAlert = WeatherAlert(1, AlertType.RAIN_SOON, 0, "Rain expected at 4:15 PM", 75.0),
                suggestions = listOf("Bring clothes inside", "Carry an umbrella"),
                events = listOf(
                    DiaryEvent(1, 1_000_000, 4_000_000, DiaryEventType.OUTDOOR, "Outdoors, 50 min", 24.0),
                    DiaryEvent(2, 5_000_000, 9_000_000, DiaryEventType.INDOOR, "Indoors, 1 h 6 min", 22.0),
                ),
            ),
            onRetry = {},
            logButton = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeErrorPreview() {
    AiraTheme { HomeScreen(HomeUiState(weather = WeatherState.Error("No internet")), onRetry = {}, logButton = {}) }
}
