package com.aira.app.data.repository

import com.aira.app.data.remote.AirQualityApi
import com.aira.app.data.remote.toAirQuality
import com.aira.app.domain.model.AirQuality
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.round

/** Loads the air quality for the Home screen. A failure only hides that tile; it never blocks the weather. */
@Singleton
class AirQualityRepository @Inject constructor(private val api: AirQualityApi) {

    /** The air quality here, or null if it is unavailable (offline, or no data for this place). */
    suspend fun get(latitude: Double, longitude: Double): AirQuality? = try {
        api.airQuality(round2(latitude), round2(longitude)).toAirQuality()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun round2(value: Double) = round(value * 100) / 100
}
