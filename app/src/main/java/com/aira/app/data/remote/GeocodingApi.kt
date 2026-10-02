package com.aira.app.data.remote

import com.aira.app.domain.model.PlaceResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo place search (free, no API key): turns a typed name into places with coordinates. */
interface GeocodingApi {

    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 8,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponse

    companion object {
        const val BASE_URL = "https://geocoding-api.open-meteo.com/"
    }
}

/** The service leaves out "results" when nothing matches. */
@Serializable
data class GeocodingResponse(val results: List<GeocodingResult> = emptyList())

@Serializable
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("admin1") val region: String? = null,
)

/** The search results, each with "region, country" as its second line. */
fun GeocodingResponse.toPlaces(): List<PlaceResult> = results.map { result ->
    PlaceResult(
        name = result.name,
        region = listOfNotNull(result.region, result.country).filter { it.isNotBlank() }.distinct().joinToString(", "),
        latitude = result.latitude,
        longitude = result.longitude,
    )
}
