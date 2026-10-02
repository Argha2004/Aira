package com.aira.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo forecast API (free, no API key). */
interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("hourly") hourly: String = FORECAST_HOURLY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 2,
    ): ForecastResponse

    /** Hourly weather for yesterday and today, used to fill in snapshots taken while offline. */
    @GET("v1/forecast")
    suspend fun hourlyHistory(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("past_days") pastDays: Int = 1,
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("timezone") timezone: String = "auto",
    ): HourlyResponse

    companion object {
        const val HOURLY_FIELDS = "temperature_2m,apparent_temperature,relative_humidity_2m," +
            "rain,uv_index,wind_speed_10m"
        const val BASE_URL = "https://api.open-meteo.com/"
        const val CURRENT_FIELDS = "temperature_2m,apparent_temperature,relative_humidity_2m," +
            "precipitation,rain,weather_code,wind_speed_10m,wind_direction_10m,uv_index,is_day,pressure_msl"
        const val DAILY_FIELDS = "sunrise,sunset,temperature_2m_max,temperature_2m_min"

        /** Hourly forecast for today and tomorrow (forecast_days = 2). */
        const val FORECAST_HOURLY_FIELDS = "precipitation_probability,precipitation,apparent_temperature,temperature_2m,weather_code"
    }
}
