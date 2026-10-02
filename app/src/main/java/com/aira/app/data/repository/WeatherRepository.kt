package com.aira.app.data.repository

import com.aira.app.data.remote.OpenMeteoApi
import com.aira.app.data.remote.toHourlyForecast
import com.aira.app.data.remote.toHourlyWeather
import com.aira.app.data.remote.toWeatherNow
import com.aira.app.domain.model.HourlyForecast
import com.aira.app.domain.model.HourlyWeather
import com.aira.app.domain.model.WeatherNow
import com.aira.app.domain.usecase.ForecastSource
import com.aira.app.domain.usecase.WeatherSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.round

/** Current weather and the hourly forecast that came in the same request. */
data class WeatherBundle(val weather: WeatherNow, val forecast: List<HourlyForecast>)

/**
 * Loads current weather and the hourly forecast. Each result is reused for 60 minutes for its location
 * (rounded to 2 decimals, which is also all we ever send to the API), so asking for the weather and then for
 * the forecast is one network request, and switching between saved places on Home does not reload them.
 * Up to [MAX_CACHED] places are kept.
 */
@Singleton
class WeatherRepository(
    private val api: OpenMeteoApi,
    private val nowMillis: () -> Long,
) : WeatherSource, ForecastSource {
    @Inject
    constructor(api: OpenMeteoApi) : this(api, System::currentTimeMillis)

    private data class CacheEntry(val key: Pair<Double, Double>, val savedAt: Long, val bundle: WeatherBundle)

    /** The cached results by location, oldest first. Only touched under [lock]. */
    private val cache = LinkedHashMap<Pair<Double, Double>, CacheEntry>()
    private val lock = Any()

    override suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherNow> =
        getBundle(latitude, longitude).map { it.weather }

    /** Today's and tomorrow's forecast, hour by hour. */
    override suspend fun getForecast(latitude: Double, longitude: Double): Result<List<HourlyForecast>> =
        getBundle(latitude, longitude).map { it.forecast }

    suspend fun getBundle(latitude: Double, longitude: Double): Result<WeatherBundle> {
        val key = round2(latitude) to round2(longitude)
        val cached = synchronized(lock) { cache[key] }
        if (cached != null && nowMillis() - cached.savedAt < CACHE_MILLIS) {
            return Result.success(cached.bundle)
        }
        return try {
            val response = api.forecast(key.first, key.second)
            val bundle = WeatherBundle(response.toWeatherNow(), response.toHourlyForecast())
            synchronized(lock) {
                cache.remove(key)
                cache[key] = CacheEntry(key, nowMillis(), bundle)
                if (cache.size > MAX_CACHED) cache.remove(cache.keys.first())
            }
            Result.success(bundle)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Hourly weather for yesterday and today. Not cached; used to fill in offline snapshots. */
    suspend fun getRecentHours(latitude: Double, longitude: Double): Result<List<HourlyWeather>> = try {
        Result.success(api.hourlyHistory(round2(latitude), round2(longitude)).toHourlyWeather())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun round2(value: Double) = round(value * 100) / 100

    companion object {
        const val CACHE_MILLIS = 60 * 60 * 1000L

        /** The live location plus up to five saved places. */
        const val MAX_CACHED = 6
    }
}
