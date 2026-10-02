package com.aira.app.data.remote

import com.aira.app.domain.model.HourlyForecast
import com.aira.app.domain.model.HourlyWeather
import com.aira.app.domain.model.WeatherNow
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** JSON shape returned by Open-Meteo `/v1/forecast` for the fields we request. */
@Serializable
data class ForecastResponse(
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    /** Height of the ground at the location in metres (from a terrain map), sent with every forecast. */
    val elevation: Double? = null,
    val current: CurrentDto,
    val daily: DailyDto? = null,
    val hourly: ForecastHourlyDto? = null,
)

/** The hourly forecast block of the forecast request. Lists line up by index with [time]. */
@Serializable
data class ForecastHourlyDto(
    val time: List<String> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
)

/**
 * One [HourlyForecast] per forecast hour. An hour without a feels-like temperature is skipped;
 * a missing rain probability or amount counts as zero.
 */
fun ForecastResponse.toHourlyForecast(): List<HourlyForecast> {
    val block = hourly ?: return emptyList()
    return block.time.mapIndexedNotNull { i, text ->
        val time = localToEpochMillis(text, utcOffsetSeconds) ?: return@mapIndexedNotNull null
        val feelsLike = block.apparentTemperature.getOrNull(i) ?: return@mapIndexedNotNull null
        HourlyForecast(
            time = time,
            precipProbability = block.precipitationProbability.getOrNull(i) ?: 0,
            precipitationMm = block.precipitation.getOrNull(i) ?: 0.0,
            feelsLikeC = feelsLike,
            tempC = block.temperature.getOrNull(i),
            weatherCode = block.weatherCode.getOrNull(i),
        )
    }
}

@Serializable
data class CurrentDto(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int,
    val precipitation: Double = 0.0,
    val rain: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("uv_index") val uvIndex: Double = 0.0,
    @SerialName("is_day") val isDay: Int = 1,
    @SerialName("pressure_msl") val pressure: Double? = null,
    @SerialName("wind_direction_10m") val windDirection: Double? = null,
)

@Serializable
data class DailyDto(
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
)

/** Hourly forecast/history block. Lists line up by index with [time]; values can be null. */
@Serializable
data class HourlyResponse(
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val hourly: HourlyDto,
)

@Serializable
data class HourlyDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val humidity: List<Int?> = emptyList(),
    val rain: List<Double?> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
)

/**
 * One [HourlyWeather] per hour. An hour without temperature, feels-like or humidity is skipped;
 * missing rain, UV or wind count as zero.
 */
fun HourlyResponse.toHourlyWeather(): List<HourlyWeather> = hourly.time.mapIndexedNotNull { i, text ->
    val time = localToEpochMillis(text, utcOffsetSeconds) ?: return@mapIndexedNotNull null
    val temp = hourly.temperature.getOrNull(i) ?: return@mapIndexedNotNull null
    val feels = hourly.apparentTemperature.getOrNull(i) ?: return@mapIndexedNotNull null
    val humidity = hourly.humidity.getOrNull(i) ?: return@mapIndexedNotNull null
    HourlyWeather(
        time = time,
        tempC = temp,
        feelsLikeC = feels,
        humidity = humidity,
        rainMm = hourly.rain.getOrNull(i) ?: 0.0,
        uvIndex = hourly.uvIndex.getOrNull(i) ?: 0.0,
        windKmh = hourly.windSpeed.getOrNull(i) ?: 0.0,
    )
}

/** Converts the API response to the domain model. Sunrise/sunset become epoch millis. */
fun ForecastResponse.toWeatherNow(): WeatherNow = WeatherNow(
    tempC = current.temperature,
    feelsLikeC = current.apparentTemperature,
    humidity = current.humidity,
    rainMm = current.rain,
    uvIndex = current.uvIndex,
    windKmh = current.windSpeed,
    weatherCode = current.weatherCode,
    isDay = current.isDay == 1,
    sunrise = daily?.sunrise?.firstOrNull()?.let { localToEpochMillis(it, utcOffsetSeconds) },
    sunset = daily?.sunset?.firstOrNull()?.let { localToEpochMillis(it, utcOffsetSeconds) },
    highC = daily?.temperatureMax?.firstOrNull(),
    lowC = daily?.temperatureMin?.firstOrNull(),
    pressureHpa = current.pressure,
    windDirection = current.windDirection?.toInt(),
    elevationM = elevation,
)

/** Open-Meteo sends local times like "2026-09-29T05:27"; the offset makes them absolute. */
private fun localToEpochMillis(local: String, offsetSeconds: Int): Long? = try {
    LocalDateTime.parse(local).toEpochSecond(ZoneOffset.ofTotalSeconds(offsetSeconds)) * 1000
} catch (e: java.time.format.DateTimeParseException) {
    null
}
