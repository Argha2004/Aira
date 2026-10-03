package com.aira.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.aira.app.MainActivity
import com.aira.app.R
import com.aira.app.data.location.LocationProvider
import com.aira.app.data.repository.SnapshotRepository
import com.aira.app.data.repository.WeatherBundle
import com.aira.app.data.repository.WeatherRepository
import com.aira.app.data.settings.SettingsStore
import com.aira.app.domain.engine.ForecastLookup
import com.aira.app.domain.engine.Temperature
import com.aira.app.domain.engine.TemperatureUnit
import com.aira.app.domain.model.WeatherCode
import com.aira.app.ui.home.skyPalette
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.DateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the widget needs from the app. A widget is not an Activity, so Hilt gives these through an entry point. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun weather(): WeatherRepository
    fun snapshots(): SnapshotRepository
    fun settings(): SettingsStore
    fun location(): LocationProvider
}

/**
 * The home-screen weather widget, in five sizes (2×1, 2×2, 4×1, 4×2 and 4×4). The user can resize it, and it picks
 * the layout that fits: the smallest shows the temperature, the biggest adds details and the next six hours.
 * The background is the same weather sky colour as Home. Tap it to open Aira.
 *
 * It refreshes when it is added or resized, every 30 minutes (Android's minimum for widgets), and after every
 * background snapshot. The weather itself is reused for up to an hour, like on Home.
 */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = refreshInBackground(context)

    /** The user resized the widget: choose the layout that fits the new size. */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) =
        refreshInBackground(context)

    /** A broadcast receiver may only work for a few seconds; goAsync keeps it alive until the weather is here. */
    private fun refreshInBackground(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                refreshAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val FALLBACK_LATITUDE = 22.57
        private const val FALLBACK_LONGITUDE = 88.36
        private const val HOURS = 6
        private const val SKY_OPACITY = 235

        private val HOUR_IDS = listOf(
            Triple(R.id.hour_time_1, R.id.hour_icon_1, R.id.hour_temp_1),
            Triple(R.id.hour_time_2, R.id.hour_icon_2, R.id.hour_temp_2),
            Triple(R.id.hour_time_3, R.id.hour_icon_3, R.id.hour_temp_3),
            Triple(R.id.hour_time_4, R.id.hour_icon_4, R.id.hour_temp_4),
            Triple(R.id.hour_time_5, R.id.hour_icon_5, R.id.hour_temp_5),
            Triple(R.id.hour_time_6, R.id.hour_icon_6, R.id.hour_temp_6),
        )

        /** Redraws every widget of this app. Does nothing when none is on a home screen. */
        suspend fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val app = context.applicationContext
            val entry = EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java)
            val bundle = loadWeather(entry)
            val unit = if (entry.settings().useFahrenheit.first()) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
            for (id in ids) {
                val size = sizeOf(app, manager.getAppWidgetOptions(id))
                val views = if (bundle != null) weatherViews(app, size, bundle, unit) else emptyViews(app, size)
                views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(app))
                manager.updateAppWidget(id, views)
            }
        }

        /** The space this widget has now: in portrait its minimum width and maximum height, in landscape the reverse. */
        private fun sizeOf(context: Context, options: Bundle): WidgetSize {
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val width = options.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            val height = options.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            return if (width <= 0 || height <= 0) WidgetSize.WIDE_4X2 else WidgetSize.of(width, height)
        }

        private fun layoutOf(size: WidgetSize): Int = when (size) {
            WidgetSize.SMALL_2X1 -> R.layout.widget_weather_2x1
            WidgetSize.SQUARE_2X2 -> R.layout.widget_weather_2x2
            WidgetSize.WIDE_4X1 -> R.layout.widget_weather_4x1
            WidgetSize.WIDE_4X2 -> R.layout.widget_weather_4x2
            WidgetSize.LARGE_4X4 -> R.layout.widget_weather_4x4
        }

        /** The weather at the phone's last known position: the newest snapshot, else the phone's location, else Kolkata. */
        private suspend fun loadWeather(entry: WidgetEntryPoint): WeatherBundle? {
            val snapshot = entry.snapshots().getLatest()
            val here = if (snapshot != null) {
                snapshot.latitude to snapshot.longitude
            } else {
                entry.location().getCurrentLocation()?.let { it.latitude to it.longitude }
                    ?: (FALLBACK_LATITUDE to FALLBACK_LONGITUDE)
            }
            return entry.weather().getBundle(here.first, here.second).getOrNull()
        }

        private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /**
         * Fills one layout. Every layout uses the same view ids, and a view that a layout does not have is simply
         * skipped, so one function serves all five sizes.
         */
        private fun weatherViews(context: Context, size: WidgetSize, bundle: WeatherBundle, unit: TemperatureUnit): RemoteViews {
            val weather = bundle.weather
            val views = RemoteViews(context.packageName, layoutOf(size))
            val sky = skyPalette(weather.weatherCode, weather.isDay, LocalTime.now().hour)
            views.setInt(R.id.widget_sky, "setColorFilter", sky.top.toArgb())
            views.setInt(R.id.widget_sky, "setImageAlpha", SKY_OPACITY)

            views.setTextViewText(R.id.widget_icon, WeatherCode.emoji(weather.weatherCode, weather.isDay))
            views.setTextViewText(R.id.widget_temp, Temperature.format(weather.tempC, unit))
            views.setTextViewText(R.id.widget_condition, WeatherCode.describe(weather.weatherCode))
            views.setTextViewText(R.id.widget_location, context.getString(R.string.home_location_device))

            val high = weather.highC
            val low = weather.lowC
            views.setTextViewText(
                R.id.widget_hilo,
                if (high != null && low != null) {
                    context.getString(R.string.widget_hilo, Temperature.value(high, unit), Temperature.value(low, unit))
                } else {
                    ""
                },
            )
            val humidity = context.getString(R.string.widget_humidity, weather.humidity)
            val wind = context.getString(R.string.widget_wind, weather.windKmh.toInt())
            views.setTextViewText(R.id.widget_details, "$humidity   $wind")
            views.setTextViewText(R.id.widget_feels, context.getString(R.string.widget_feels_like, Temperature.format(weather.feelsLikeC, unit)))
            views.setTextViewText(R.id.widget_humidity, humidity)
            views.setTextViewText(R.id.widget_wind, wind)
            views.setTextViewText(R.id.widget_uv, context.getString(R.string.widget_uv, weather.uvIndex.toInt()))
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
            views.setTextViewText(R.id.widget_updated, context.getString(R.string.widget_updated, time))

            val hours = ForecastLookup.nextHours(bundle.forecast, System.currentTimeMillis(), HOURS)
            val format = DateTimeFormatter.ofPattern("h a")
            HOUR_IDS.forEachIndexed { index, (timeId, iconId, tempId) ->
                val hour = hours.getOrNull(index)
                val temp = hour?.tempC
                views.setViewVisibility(timeId, if (hour != null) View.VISIBLE else View.INVISIBLE)
                if (hour != null) {
                    views.setTextViewText(
                        timeId,
                        if (index == 0) context.getString(R.string.widget_now) else format.format(Instant.ofEpochMilli(hour.time).atZone(ZoneId.systemDefault())),
                    )
                    views.setTextViewText(iconId, WeatherCode.emoji(hour.weatherCode ?: weather.weatherCode, hour.isDayHour()))
                    views.setTextViewText(tempId, temp?.let { Temperature.format(it, unit).trimEnd('C', 'F') } ?: "")
                }
            }
            return views
        }

        /** Shown when no weather could be loaded (offline): tapping it opens the app. */
        private fun emptyViews(context: Context, size: WidgetSize): RemoteViews {
            val views = RemoteViews(context.packageName, layoutOf(size))
            views.setInt(R.id.widget_sky, "setColorFilter", 0xFF557394.toInt())
            views.setInt(R.id.widget_sky, "setImageAlpha", SKY_OPACITY)
            views.setTextViewText(R.id.widget_temp, "--")
            views.setTextViewText(R.id.widget_icon, "")
            views.setTextViewText(R.id.widget_condition, context.getString(R.string.widget_unavailable))
            views.setTextViewText(R.id.widget_location, context.getString(R.string.widget_tap_to_open))
            return views
        }

        /** Day or night for a forecast hour, by the clock (6:00 to 18:59 is day), to pick the sun or moon. */
        private fun com.aira.app.domain.model.HourlyForecast.isDayHour(): Boolean =
            Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).hour in 6..18
    }
}
