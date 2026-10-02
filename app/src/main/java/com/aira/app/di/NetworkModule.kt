package com.aira.app.di

import com.aira.app.BuildConfig
import com.aira.app.data.remote.AirQualityApi
import com.aira.app.data.remote.GeocodingApi
import com.aira.app.data.remote.OpenMeteoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideOpenMeteoApi(client: OkHttpClient, json: Json): OpenMeteoApi =
        retrofit(OpenMeteoApi.BASE_URL, client, json).create(OpenMeteoApi::class.java)

    @Provides
    @Singleton
    fun provideAirQualityApi(client: OkHttpClient, json: Json): AirQualityApi =
        retrofit(AirQualityApi.BASE_URL, client, json).create(AirQualityApi::class.java)

    @Provides
    @Singleton
    fun provideGeocodingApi(client: OkHttpClient, json: Json): GeocodingApi =
        retrofit(GeocodingApi.BASE_URL, client, json).create(GeocodingApi::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
