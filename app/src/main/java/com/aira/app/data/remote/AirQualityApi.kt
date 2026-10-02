package com.aira.app.data.remote

import com.aira.app.domain.model.AirQuality
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo air quality API (free, no API key). Gets the same rounded location as the weather request. */
interface AirQualityApi {

    @GET("v1/air-quality")
    suspend fun airQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,pm2_5",
    ): AirQualityResponse

    companion object {
        const val BASE_URL = "https://air-quality-api.open-meteo.com/"
    }
}

@Serializable
data class AirQualityResponse(val current: AirQualityDto)

@Serializable
data class AirQualityDto(
    @SerialName("us_aqi") val usAqi: Double? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
)

/** Null when the service has no air quality index for this place. */
fun AirQualityResponse.toAirQuality(): AirQuality? {
    val aqi = current.usAqi ?: return null
    return AirQuality(aqi = aqi.toInt(), pm25 = current.pm25 ?: 0.0)
}
